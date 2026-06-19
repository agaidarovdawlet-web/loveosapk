package com.example.loveosapk.data.remote

import com.example.loveosapk.BuildConfig
import com.example.loveosapk.domain.repository.RemoteDataSource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRemoteDataSource(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DATABASE_URL)
) : RemoteDataSource {

    override fun <T> observeValue(path: String, clazz: Class<T>): Flow<Result<T>> = callbackFlow {
        val ref = reference(path)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val value = snapshot.getValue(clazz)
                if (value != null) {
                    trySend(Result.success(value))
                } else {
                    trySend(Result.failure(NoSuchElementException("No value at $path")))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    override suspend fun <T> getValueOnce(path: String, clazz: Class<T>): Result<T> = runCatching {
        val snapshot = reference(path).get().await()
        snapshot.getValue(clazz) ?: throw NoSuchElementException("No value at $path")
    }

    override suspend fun <T> setValue(path: String, value: T): Result<Unit> = runCatching {
        reference(path).setValue(value).await()
    }

    override suspend fun updateChildren(path: String, values: Map<String, Any?>): Result<Unit> = runCatching {
        reference(path).updateChildren(values).await()
    }

    private fun reference(path: String): DatabaseReference {
        val normalizedPath = path.trim('/')
        return if (normalizedPath.isBlank()) {
            database.reference
        } else {
            database.getReference(normalizedPath)
        }
    }
}
