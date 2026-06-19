package com.example.loveosapk.data.sync

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageCompressor(private val context: Context) {
    fun compress(uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        
        // Resize if too large
        val maxWidth = 1080
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width = if (bitmap.width > maxWidth) maxWidth else bitmap.width
        val height = (width / ratio).toInt()
        
        val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
        
        val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        val out = FileOutputStream(file)
        resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
        out.flush()
        out.close()
        
        return file
    }
}
