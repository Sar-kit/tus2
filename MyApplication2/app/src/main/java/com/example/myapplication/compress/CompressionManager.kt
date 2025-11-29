package com.example.myapplication.compress

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CompressionManager(private val context: Context) {

    suspend fun compressImage(
        uri: Uri,
        onStart: () -> Unit,
        onComplete: (compressedUri: Uri, beforeKb: Long, afterKb: Long) -> Unit
    ): Uri = withContext(Dispatchers.IO) {

        onStart()

        // Convert Uri → File
        val inputFile = FileUtil.fromUri(context, uri)
        val beforeSize = inputFile.length() / 1024

        // Load bitmap bounds
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(inputFile.absolutePath, options)

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        // Decide target resolution
        val (targetW, targetH) =
            if (originalWidth > originalHeight)
                1280 to 720   // landscape
            else
                720 to 1280   // portrait

        val outputFile = File(context.cacheDir, "IMG_COMP_${System.currentTimeMillis()}.jpg")

        val compressed = Compressor.compress(context, inputFile) {
            resolution(targetW, targetH)   // 🔥 Set resolution
            format(Bitmap.CompressFormat.JPEG)
            quality(85)                    // Good balance
        }

        // Save final output
        compressed.copyTo(outputFile, overwrite = true)

        val afterSize = outputFile.length() / 1024
        val resultUri = Uri.fromFile(outputFile)

        onComplete(resultUri, beforeSize, afterSize)

        return@withContext resultUri
    }


}
