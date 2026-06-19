package com.example.loveosapk.data.sync

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class VoiceRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun start() {
        currentFile = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(currentFile?.absolutePath)
            prepare()
            start()
        }
    }

    fun stop(): File? {
        mediaRecorder?.let { recorder ->
            runCatching { recorder.stop() }
            recorder.release()
        }
        mediaRecorder = null
        return currentFile
    }

    fun release() {
        mediaRecorder?.release()
        mediaRecorder = null
    }
}
