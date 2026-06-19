package com.example.loveosapk.data.repository

import android.util.Log
import com.example.loveosapk.BuildConfig
import com.example.loveosapk.data.PreferenceManager
import com.example.loveosapk.data.local.CycleLogDao
import com.example.loveosapk.data.local.CycleLogEntity
import com.example.loveosapk.domain.model.*
import com.example.loveosapk.domain.repository.CycleRepository
import com.example.loveosapk.domain.usecase.PredictCyclePhasesUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    private val preferenceManager: PreferenceManager
) : CycleRepository, Closeable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val firebaseDb = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val sharedRef = firebaseDb.getReference("loveos_shared")
    private val json = Json { ignoreUnknownKeys = true }
    private val predictor = PredictCyclePhasesUseCase()
    private var cycleLogsRef: DatabaseReference? = null
    private var cycleLogsListener: ChildEventListener? = null
    
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
            syncToFirebase(mergedLog)
        }
    }

    override suspend fun deleteLog(date: LocalDate) {
        dao.deleteLog(date.toString())
        if (syncEnabled && partnerCode != null) {
            withTimeout(15_000) {
                getPartnerRef()?.child("cycle_logs")?.child(date.toString())?.removeValue()?.await()
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

    private suspend fun syncToFirebase(log: CycleLog) {
        val partnerRef = getPartnerRef() ?: return
        val node = partnerRef.child("cycle_logs").child(log.date.toString())
        val firebaseModel = log.toFirebaseModel()
        withTimeout(15_000) {
            ensureSignedIn()
            node.setValue(firebaseModel).await()
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
        
        val ref = getPartnerRef()?.child("cycle_logs") ?: return
        ref.keepSynced(true)
        
        scope.launch { pushLocalCycleSnapshot(ref) }

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, prevKey: String?) {
                syncFromFirebase(snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, prevKey: String?) {
                syncFromFirebase(snapshot)
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val date = snapshot.key ?: return
                scope.launch {
                    val existing = dao.getLogByDate(date).firstOrNull()?.toDomain()
                    if (existing?.source != "me") {
                        dao.deleteLog(date)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("CYCLE_REPOSITORY", "Cycle listener cancelled: ${error.message}", error.toException())
            }
            override fun onChildMoved(snapshot: DataSnapshot, prevKey: String?) {}
        }
        ref.addChildEventListener(listener)
        cycleLogsRef = ref
        cycleLogsListener = listener
    }

    private fun syncFromFirebase(snapshot: DataSnapshot) {
        val date = snapshot.key ?: return
        val map = snapshot.value as? Map<*, *> ?: return
        
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

    private fun getPartnerRef(): DatabaseReference? {
        val code = partnerCode ?: return null
        return sharedRef.child("pairs").child(code)
    }

    private suspend fun pushLocalCycleSnapshot(ref: DatabaseReference) {
        dao.getAllLogs().first().forEach { entity ->
            val remoteSnapshot = ref.child(entity.date).get().await()
            val remoteTs = remoteSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L
            if (!remoteSnapshot.exists() || entity.timestamp >= remoteTs) {
                ref.child(entity.date).setValue(entity.toDomain().toFirebaseModel()).await()
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
        val ref = cycleLogsRef
        val listener = cycleLogsListener
        if (ref != null && listener != null) {
            ref.removeEventListener(listener)
        }
        cycleLogsRef = null
        cycleLogsListener = null
        isListenersSetup = false
        activePartnerCode = null
    }
}
