package com.example.loveosapk.data.repository

import android.util.Log
import com.example.loveosapk.BuildConfig
import com.example.loveosapk.data.PreferenceManager
import com.example.loveosapk.data.local.CycleLogDao
import com.example.loveosapk.data.local.CycleLogEntity
import com.example.loveosapk.data.remote.FirebaseRemoteDataSource
import com.example.loveosapk.domain.model.*
import com.example.loveosapk.domain.repository.CycleRepository
import com.example.loveosapk.domain.repository.RemoteDataSource
import com.example.loveosapk.domain.usecase.PredictCyclePhasesUseCase
import com.google.firebase.auth.FirebaseAuth
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class CycleRepositoryImpl(
    private val dao: CycleLogDao,
    private val preferenceManager: PreferenceManager,
    private val remoteDataSource: RemoteDataSource = FirebaseRemoteDataSource()
) : CycleRepository, Closeable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val json = Json { ignoreUnknownKeys = true }
    private val predictor = PredictCyclePhasesUseCase()
    private var cycleLogsJob: Job? = null
    
    private var syncEnabled = false
    private var myRole = "me"
    private var partnerCode: String? = null
    private var isListenersSetup = false
    private var activePartnerCode: String? = null

    init {
        scope.launch {
            preferenceManager.appStateFlow.collect { state ->
                syncEnabled = state.syncEnabled
                myRole = state.myRole
                partnerCode = state.partnerCode
                if (syncEnabled && partnerCode != null) {
                    ensureSignedIn()
                    setupRealtimeListener()
                } else if (isListenersSetup) {
                    removeRealtimeListener()
                }
            }
        }
    }

    override fun getLogsForRange(start: LocalDate, end: LocalDate): Flow<List<CycleLog>> =
        dao.getLogsForRange(start.toString(), end.toString())
            .map { entities -> entities.map { it.toDomain() } }

    override fun getAllLogs(): Flow<List<CycleLog>> =
        dao.getAllLogs().map { entities -> entities.map { it.toDomain() } }

    override fun getLogForDate(date: LocalDate): Flow<CycleLog?> =
        dao.getLogByDate(date.toString()).map { it?.toDomain() }

    override suspend fun saveLog(log: CycleLog) {
        val existing = dao.getLogByDate(log.date.toString()).firstOrNull()
        val mergedLog = mergeForLocalSave(existing?.toDomain(), log)

        dao.upsertLog(mergedLog.toEntity())
        
        if (syncEnabled && partnerCode != null) {
            syncToRemote(mergedLog)
        }
    }

    override suspend fun deleteLog(date: LocalDate) {
        dao.deleteLog(date.toString())
        if (syncEnabled && partnerCode != null) {
            withTimeout(15_000) {
                remoteDataSource.updateChildren(cycleLogsPath(), mapOf(date.toString() to null)).getOrThrow()
            }
        }
    }

    override fun getCycleStats(): Flow<CycleStats> = getAllLogs().map { predictor.calculateStats(it) }

    private fun resolveConflict(local: CycleLog, remote: CycleLog): CycleLog {
        return CycleLog(
            date = local.date,
            isPeriod = local.isPeriod || remote.isPeriod,
            flowIntensity = maxOf(local.flowIntensity, remote.flowIntensity),
            symptoms = (local.symptoms + remote.symptoms).distinct(),
            mood = if (remote.timestamp > local.timestamp) remote.mood else local.mood,
            painLevel = maxOf(local.painLevel, remote.painLevel),
            energyLevel = (local.energyLevel + remote.energyLevel) / 2,
            notes = listOf(local.notes, remote.notes).filter { it.isNotBlank() }.distinct().joinToString(" --- "),
            isOvulation = local.isOvulation || remote.isOvulation,
            timestamp = maxOf(local.timestamp, remote.timestamp),
            source = if (remote.timestamp > local.timestamp) remote.source else local.source
        )
    }

    private suspend fun syncToRemote(log: CycleLog) {
        val path = "${cycleLogsPath()}/${log.date}"
        withTimeout(15_000) {
            ensureSignedIn()
            remoteDataSource.setValue(path, log.toFirebaseModel()).getOrThrow()
        }
    }

    private fun setupRealtimeListener() {
        val code = partnerCode ?: return
        if (!syncEnabled) return
        if (isListenersSetup && activePartnerCode == code) return
        if (isListenersSetup && activePartnerCode != code) {
            removeRealtimeListener()
        }
        isListenersSetup = true
        activePartnerCode = code
        
        val path = cycleLogsPath()
        scope.launch { pushLocalCycleSnapshot(path) }

        cycleLogsJob = remoteDataSource.observeValue(path, Map::class.java)
            .onEach { result ->
                result
                    .onSuccess { value -> syncFromRemote(value) }
                    .onFailure { error -> Log.e("CYCLE_REPOSITORY", "Cycle listener failed", error) }
            }
            .launchIn(scope)
    }

    private fun syncFromRemote(logsByDate: Map<*, *>) {
        logsByDate.forEach { (dateValue, logValue) ->
            val date = dateValue as? String ?: return@forEach
            val map = logValue as? Map<*, *> ?: return@forEach
            syncRemoteLog(date, map)
        }
    }

    private fun syncRemoteLog(date: String, map: Map<*, *>) {
        scope.launch {
            val remoteLog = CycleLog(
                date = LocalDate.parse(date),
                isPeriod = map["isPeriod"] as? Boolean ?: false,
                flowIntensity = map["flowIntensity"].asInt(0),
                symptoms = (map["symptoms"] as? List<*>)
                    ?.mapNotNull { (it as? String)?.let { name -> runCatching { Symptom.valueOf(name) }.getOrNull() } }
                    ?: emptyList(),
                mood = (map["mood"] as? String)?.let { runCatching { Mood.valueOf(it) }.getOrNull() },
                painLevel = map["painLevel"].asInt(0),
                energyLevel = map["energyLevel"].asInt(5),
                notes = map["notes"] as? String ?: "",
                isOvulation = map["isOvulation"] as? Boolean ?: false,
                timestamp = map["timestamp"].asLong(System.currentTimeMillis()),
                source = if (map["senderRole"] == myRole) "me" else "partner"
            )
            
            val existing = dao.getLogByDate(date).firstOrNull()
            val mergedLog = mergeForRemoteSync(existing?.toDomain(), remoteLog)
            
            dao.upsertLog(mergedLog.toEntity())
        }
    }

    private fun mergeForLocalSave(existing: CycleLog?, incoming: CycleLog): CycleLog {
        if (existing == null) return incoming
        if (existing.source == incoming.source) return incoming
        return resolveConflict(existing, incoming)
    }

    private fun mergeForRemoteSync(existing: CycleLog?, remote: CycleLog): CycleLog {
        if (existing == null) return remote
        if (existing.source == remote.source) {
            return if (remote.timestamp >= existing.timestamp) remote else existing
        }
        return resolveConflict(existing, remote)
    }

    private fun cycleLogsPath(): String {
        val code = partnerCode ?: error("Partner code is required for cycle sync")
        return "loveos_shared/pairs/$code/cycle_logs"
    }

    private suspend fun pushLocalCycleSnapshot(path: String) {
        dao.getAllLogs().first().forEach { entity ->
            val remoteLog = remoteDataSource.getValueOnce("$path/${entity.date}", Map::class.java).getOrNull()
            val remoteTs = remoteLog?.get("timestamp").asLong(0L)
            if (remoteLog == null || entity.timestamp >= remoteTs) {
                remoteDataSource.setValue("$path/${entity.date}", entity.toDomain().toFirebaseModel()).getOrThrow()
            }
        }
    }

    private fun Any?.asInt(default: Int): Int = when (this) {
        is Number -> toInt()
        is String -> toIntOrNull() ?: default
        else -> default
    }

    private fun Any?.asLong(default: Long): Long = when (this) {
        is Number -> toLong()
        is String -> toLongOrNull() ?: default
        else -> default
    }

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    private fun CycleLogEntity.toDomain() = CycleLog(
        date = LocalDate.parse(date),
        isPeriod = isPeriod,
        flowIntensity = flowIntensity,
        symptoms = try { json.decodeFromString<List<String>>(symptomsJson).map { Symptom.valueOf(it) } } catch (e: Exception) { emptyList() },
        mood = mood?.let { try { Mood.valueOf(it) } catch (e: Exception) { null } },
        painLevel = painLevel,
        energyLevel = energyLevel,
        notes = notes,
        isOvulation = isOvulation,
        timestamp = timestamp,
        source = source
    )

    private fun CycleLog.toEntity() = CycleLogEntity(
        date = date.toString(),
        isPeriod = isPeriod,
        flowIntensity = flowIntensity,
        symptomsJson = json.encodeToString(symptoms.map { it.name }),
        mood = mood?.name,
        painLevel = painLevel,
        energyLevel = energyLevel,
        notes = notes,
        isOvulation = isOvulation,
        timestamp = timestamp,
        source = source
    )

    private fun CycleLog.toFirebaseModel() = mapOf(
        "isPeriod" to isPeriod,
        "flowIntensity" to flowIntensity,
        "symptoms" to symptoms.map { it.name },
        "mood" to mood?.name,
        "painLevel" to painLevel,
        "energyLevel" to energyLevel,
        "notes" to notes,
        "isOvulation" to isOvulation,
        "timestamp" to timestamp,
        "senderRole" to myRole
    )

    override fun close() {
        removeRealtimeListener()
        scope.cancel()
    }

    private fun removeRealtimeListener() {
        cycleLogsJob?.cancel()
        cycleLogsJob = null
        isListenersSetup = false
        activePartnerCode = null
    }
}
