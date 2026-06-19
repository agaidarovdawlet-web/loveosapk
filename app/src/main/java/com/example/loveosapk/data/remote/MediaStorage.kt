package com.example.loveosapk.data.remote

import android.net.Uri
import android.util.Log
import com.example.loveosapk.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

class MediaStorage {
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance(BuildConfig.FIREBASE_STORAGE_BUCKET)
    private val storageRef = storage.reference

    suspend fun uploadPhoto(uri: Uri): String {
        return try {
            withTimeout(30_000) {
                ensureSignedIn()
                val fileName = "photos/${UUID.randomUUID()}.jpg"
                val photoRef = storageRef.child(fileName)
                photoRef.putFile(uri).await()
                photoRef.downloadUrl.await().toString()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("MEDIA_STORAGE", "Photo upload error: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadVoice(file: File): String {
        return try {
            withTimeout(30_000) {
                ensureSignedIn()
                val fileName = "voice/${UUID.randomUUID()}.m4a"
                val voiceRef = storageRef.child(fileName)
                voiceRef.putFile(Uri.fromFile(file)).await()
                voiceRef.downloadUrl.await().toString()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("MEDIA_STORAGE", "Voice upload error: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadVideo(uri: Uri, extension: String, contentType: String): String {
        return try {
            withTimeout(120_000) {
                ensureSignedIn()
                val safeExtension = extension.lowercase().takeIf { it.matches(Regex("^[a-z0-9]{2,5}$")) } ?: "mp4"
                val fileName = "videos/${UUID.randomUUID()}.$safeExtension"
                val videoRef = storageRef.child(fileName)
                val metadata = StorageMetadata.Builder()
                    .setContentType(contentType)
                    .build()
                videoRef.putFile(uri, metadata).await()
                videoRef.downloadUrl.await().toString()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("MEDIA_STORAGE", "Video upload error: ${e.message}", e)
            throw e
        }
    }

    private suspend fun ensureSignedIn() {
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }
}
