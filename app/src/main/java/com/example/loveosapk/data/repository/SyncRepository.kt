package com.example.loveosapk.data.repository

import android.content.Context
import android.util.Log
import com.example.loveosapk.BuildConfig
import com.example.loveosapk.data.*
import com.example.loveosapk.data.local.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class SyncRepository(
    private val context: Context,
    val dao: LoveOsDao,
    private val cycleDao: CycleLogDao,
    private val preferenceManager: PreferenceManager
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
    private val sharedRef = database.getReference("loveos_shared")
    private val valueListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
    private val childListeners = mutableListOf<Pair<DatabaseReference, ChildEventListener>>()

    val appState = preferenceManager.appStateFlow
    val wishes = dao.getAllWishes().map { it.map { e -> e.toDomain() } }
    val tasks = dao.getAllTasks().map { it.map { e -> e.toDomain() } }
    val savings = dao.getAllSavings().map { it.map { e -> e.toDomain() } }
    val notes = dao.getAllNotes().map { it.map { e -> e.toDomain() } }
    val capsules = dao.getAllTimeCapsules().map { it.map { e -> e.toDomain() } }
    val chatMessages = dao.getAllChatMessages().map { it.map { e -> e.toDomain() } }
    val periods: Flow<List<String>> = cycleDao.getAllLogs().map { list -> list.filter { it.isPeriod }.map { it.date } }
    val days: Flow<Map<String, CycleLogEntity>> = cycleDao.getAllLogs().map { list -> list.associateBy { it.date } }
    
    private val _incomingHearts = MutableSharedFlow<Heart>()
    val incomingHearts = _incomingHearts.asSharedFlow()

    private var syncEnabled = false
    private var myRole = "me"
    private var isListenersSetup = false
    private var activePartnerCode: String? = null

    init {
        scope.launch {
            appState.collect { state ->
                syncEnabled = state.syncEnabled
                myRole = state.myRole
                if (syncEnabled) {
                    setupFirebaseListeners()
                } else if (isListenersSetup) {
                    removeFirebaseListeners()
                }
            }
        }
    }

    private fun setupFirebaseListeners() {
        if (!syncEnabled) return
        scope.launch {
            ensureSignedIn()
            val partnerCode = appState.first().partnerCode ?: return@launch
            if (isListenersSetup && activePartnerCode == partnerCode) return@launch
            if (isListenersSetup && activePartnerCode != partnerCode) {
                removeFirebaseListeners()
            }
            isListenersSetup = true
            activePartnerCode = partnerCode
            val partnerRef = sharedRef.child("pairs").child(partnerCode)
            
            // Critical for offline support
            partnerRef.keepSynced(true)

            pushLocalSnapshot(partnerRef)

            setupChildSync(
                ref = partnerRef.child("wishes"),
                clazz = Wish::class.java,
                onUpsert = { dao.upsertWish(it.toEntity()) },
                onDelete = { dao.deleteWishById(it) }
            )
            setupChildSync(
                ref = partnerRef.child("tasks"),
                clazz = Task::class.java,
                onUpsert = { dao.upsertTask(it.toEntity()) },
                onDelete = { dao.deleteTaskById(it) }
            )
            setupChildSync(
                ref = partnerRef.child("savings"),
                clazz = Saving::class.java,
                onUpsert = { dao.upsertSaving(it.toEntity()) },
                onDelete = { dao.deleteSavingById(it) }
            )
            setupChildSync(
                ref = partnerRef.child("notes"),
                clazz = Note::class.java,
                onUpsert = { dao.upsertNote(it.toEntity()) },
                onDelete = { dao.deleteNoteById(it) }
            )
            setupChildSync(
                ref = partnerRef.child("capsules"),
                clazz = TimeCapsule::class.java,
                onUpsert = { dao.upsertTimeCapsule(it.toEntity()) },
                onDelete = { dao.deleteTimeCapsuleById(it) }
            )
            
            val chatRef = partnerRef.child("chat")
            val chatListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val msg = snapshot.getValue(ChatMessage::class.java)
                    msg?.let { scope.launch { dao.upsertChatMessage(it.toEntity()) } }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val msg = snapshot.getValue(ChatMessage::class.java)
                    msg?.let { scope.launch { dao.upsertChatMessage(it.toEntity()) } }
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val id = snapshot.key ?: return
                    scope.launch { dao.deleteChatMessageById(id) }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SYNC_REPOSITORY", "Chat listener cancelled: ${error.message}", error.toException())
                }
            }
            chatRef.addChildEventListener(chatListener)
            childListeners += chatRef to chatListener

            val partnerRole = if (myRole == "me") "partner" else "me"
            val profileRef = partnerRef.child("profiles").child(partnerRole)
            val profileListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    profile?.let { p ->
                        scope.launch {
                            val current = appState.first()
                            preferenceManager.updateAppState(current.copy(partner = p))
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SYNC_REPOSITORY", "Profile listener cancelled: ${error.message}", error.toException())
                }
            }
            profileRef.addValueEventListener(profileListener)
            valueListeners += profileRef to profileListener

            val heartsRef = partnerRef.child("hearts")
            val heartsListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val heart = snapshot.getValue(Heart::class.java)
                    if (heart != null && heart.sender != myRole) {
                        scope.launch { _incomingHearts.emit(heart) }
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e("SYNC_REPOSITORY", "Heart listener cancelled: ${error.message}", error.toException())
                }
            }
            heartsRef.addChildEventListener(heartsListener)
            childListeners += heartsRef to heartsListener
        }
    }

    private fun <T> setupChildSync(
        ref: DatabaseReference,
        clazz: Class<T>,
        onUpsert: suspend (T) -> Unit,
        onDelete: suspend (String) -> Unit
    ) {
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val item = snapshot.getValue(clazz) ?: return
                scope.launch { onUpsert(item) }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val item = snapshot.getValue(clazz) ?: return
                scope.launch { onUpsert(item) }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val id = snapshot.key ?: return
                scope.launch { onDelete(id) }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit

            override fun onCancelled(error: DatabaseError) {
                Log.e("SYNC_REPOSITORY", "Child listener cancelled at $ref: ${error.message}", error.toException())
            }
        }
        ref.addChildEventListener(listener)
        childListeners += ref to listener
    }

    private suspend fun getPartnerRef(): com.google.firebase.database.DatabaseReference? {
        ensureSignedIn()
        val code = appState.first().partnerCode ?: return null
        return sharedRef.child("pairs").child(code)
    }

    private suspend fun pushLocalSnapshot(partnerRef: DatabaseReference) {
        wishes.first().forEach { wish ->
            pushIfRemoteMissingOrOlder(partnerRef.child("wishes"), wish.id, wish.ts, wish)
        }
        tasks.first().forEach { task ->
            pushIfRemoteMissingOrOlder(partnerRef.child("tasks"), task.id, task.ts, task)
        }
        savings.first().forEach { saving ->
            pushIfRemoteMissingOrOlder(partnerRef.child("savings"), saving.id, saving.ts, saving)
        }
        notes.first().forEach { note ->
            pushIfRemoteMissingOrOlder(partnerRef.child("notes"), note.id, note.ts, note)
        }
        capsules.first().forEach { capsule ->
            pushIfRemoteMissingOrOlder(partnerRef.child("capsules"), capsule.id, capsule.ts, capsule)
        }
        chatMessages.first().forEach { message ->
            pushIfRemoteMissingOrOlder(partnerRef.child("chat"), message.id, message.ts, message)
        }
    }

    private suspend fun pushIfRemoteMissingOrOlder(
        collectionRef: DatabaseReference,
        id: String,
        localTs: Long,
        value: Any
    ) {
        if (id.isBlank()) return
        val remoteSnapshot = collectionRef.child(id).get().await()
        val remoteTs = remoteSnapshot.child("ts").getValue(Long::class.java) ?: 0L
        if (!remoteSnapshot.exists() || localTs >= remoteTs) {
            collectionRef.child(id).setValue(value).await()
        }
    }

    suspend fun updateMe(profile: UserProfile) {
        val currentState = appState.first()
        myRole = currentState.myRole
        val newState = currentState.copy(me = profile)
        updateAppState(newState)
        if (syncEnabled) {
            awaitRemote { getPartnerRef()?.child("profiles")?.child(myRole)?.setValue(profile)?.await() }
        }
    }

    suspend fun updateAppState(state: AppState) {
        preferenceManager.updateAppState(state)
        // Optionally sync theme/settings to Firebase if needed
    }

    suspend fun addWish(wish: Wish) {
        dao.upsertWish(wish.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("wishes")?.child(wish.id)?.setValue(wish)?.await() }
    }

    suspend fun toggleWish(wish: Wish) {
        val updated = wish.copy(done = !wish.done, ts = System.currentTimeMillis())
        addWish(updated)
    }

    suspend fun deleteWish(id: String) {
        val wish = wishes.first().find { it.id == id } ?: return
        dao.deleteWish(wish.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("wishes")?.child(id)?.removeValue()?.await() }
    }

    suspend fun addTask(task: Task) {
        dao.upsertTask(task.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("tasks")?.child(task.id)?.setValue(task)?.await() }
    }

    suspend fun toggleTask(task: Task) {
        val updated = task.copy(done = !task.done, ts = System.currentTimeMillis())
        addTask(updated)
    }

    suspend fun deleteTask(id: String) {
        val task = tasks.first().find { it.id == id } ?: return
        dao.deleteTask(task.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("tasks")?.child(id)?.removeValue()?.await() }
    }

    suspend fun addSaving(saving: Saving) {
        dao.upsertSaving(saving.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("savings")?.child(saving.id)?.setValue(saving)?.await() }
    }

    suspend fun updateSaving(saving: Saving) {
        addSaving(saving)
    }

    suspend fun deleteSaving(id: String) {
        val saving = savings.first().find { it.id == id } ?: return
        dao.deleteSaving(saving.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("savings")?.child(id)?.removeValue()?.await() }
    }

    suspend fun addNote(note: Note) {
        dao.upsertNote(note.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("notes")?.child(note.id)?.setValue(note)?.await() }
    }

    suspend fun deleteNote(id: String) {
        val note = notes.first().find { it.id == id } ?: return
        dao.deleteNote(note.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("notes")?.child(id)?.removeValue()?.await() }
    }

    suspend fun deleteTimeCapsule(id: String) {
        val capsule = capsules.first().find { it.id == id } ?: return
        dao.deleteTimeCapsule(capsule.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("capsules")?.child(id)?.removeValue()?.await() }
    }

    suspend fun addTimeCapsule(capsule: TimeCapsule) {
        dao.upsertTimeCapsule(capsule.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("capsules")?.child(capsule.id)?.setValue(capsule)?.await() }
    }

    suspend fun saveCycleLog(log: CycleLogEntity) {
        // Handled by CycleRepository
    }

    suspend fun sendChatMessage(
        text: String,
        imageUrl: String? = null,
        voiceUrl: String? = null,
        voiceDuration: Long? = null,
        videoUrl: String? = null,
        videoDuration: Long? = null
    ) {
        val senderRole = appState.first().myRole
        myRole = senderRole
        val msg = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            sender = senderRole,
            text = text,
            imageUrl = imageUrl,
            voiceUrl = voiceUrl,
            voiceDuration = voiceDuration,
            videoUrl = videoUrl,
            videoDuration = videoDuration,
            ts = System.currentTimeMillis(),
            status = MessageStatus.SENT
        )
        dao.upsertChatMessage(msg.toEntity())
        if (syncEnabled) awaitRemote { getPartnerRef()?.child("chat")?.child(msg.id)?.setValue(msg)?.await() }
    }

    suspend fun sendHeart() {
        if (syncEnabled) {
            val senderRole = appState.first().myRole
            myRole = senderRole
            awaitRemote { getPartnerRef()?.child("hearts")?.push()?.setValue(Heart(senderRole, System.currentTimeMillis()))?.await() }
        }
    }

    private suspend fun awaitRemote(block: suspend () -> Unit) {
        ensureSignedIn()
        withTimeout(15_000) {
            block()
        }
    }

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    override fun close() {
        removeFirebaseListeners()
        scope.cancel()
    }

    private fun removeFirebaseListeners() {
        valueListeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        childListeners.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        valueListeners.clear()
        childListeners.clear()
        isListenersSetup = false
        activePartnerCode = null
    }
}
