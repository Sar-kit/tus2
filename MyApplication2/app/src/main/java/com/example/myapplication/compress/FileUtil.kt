package com.example.myapplication.compress

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object FileUtil {
    fun fromUri(context: Context, uri: Uri): File {
        val input = context.contentResolver.openInputStream(uri)!!
        val output = File(context.cacheDir, "SRC_${System.currentTimeMillis()}")
        val out = FileOutputStream(output)
        input.copyTo(out)
        input.close()
        out.close()
        return output
    }
}
