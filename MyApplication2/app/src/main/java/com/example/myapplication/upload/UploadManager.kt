package com.example.myapplication.upload

import android.content.Context
import android.net.Uri
import io.tus.android.client.TusAndroidUpload
import io.tus.android.client.TusPreferencesURLStore
import io.tus.java.client.TusClient
import io.tus.java.client.TusExecutor
import kotlinx.coroutines.*
import java.net.URL

class UploadManager(
    private val context: Context,
    tusEndpoint: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private fun key(uri: Uri, formId: String?) = "$uri::$formId"
    // --- Single TusClient instance required for resume ---
    private val client = TusClient().apply {
        uploadCreationURL = URL(tusEndpoint)

        // SharedPreferences store for resumable URLs
        val prefs = context.getSharedPreferences("tus", Context.MODE_PRIVATE)
        enableResuming(TusPreferencesURLStore(prefs))
    }

    fun uploadFile(
        uri: Uri,
        formId: String,
        onProgress: (Long, Long) -> Unit,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Prevent double upload for same uri
        if (activeJobs.containsKey(key(uri, formId))) return

        val job = scope.launch {
            try {
                val fileName = uri.lastPathSegment ?: "upload.bin"
                val mimeType =
                    context.contentResolver.getType(uri) ?: "application/octet-stream"

                val upload = TusAndroidUpload(uri, context).apply {
                    metadata = mapOf(
                        "formId" to formId,
                        "fileName" to fileName,
                        "mimeType" to mimeType
                    )

                    // Stable fingerprint = required for resume to work
                    fingerprint = "upload-${uri}-form-${formId}"

                }

                val executor = object : TusExecutor() {
                    override fun makeAttempt() {
                        // Resume if exists, otherwise create new
                        val uploader = client.resumeOrCreateUpload(upload)

                        // Smaller chunk size reduces broken pipe issues
                        uploader.chunkSize = 512 * 1024 // 512 KB

                        val total = upload.size

                        while (true) {
                            if (!isActive) return

                            onProgress(uploader.offset, total)

                            val bytes = uploader.uploadChunk()
                            if (bytes < 0) break

                            // Prevent socket overrun — cannot use delay() here
                            Thread.sleep(5)
                        }

                        // Pause happened
                        if (!isActive) return

                        // Finalize on successful upload
                        uploader.finish()
                        onComplete(uploader.uploadURL.toString())
                    }
                }

                executor.makeAttempts()

            } catch (e: Exception) {
                onError(e)
            }
        }

        activeJobs[key(uri,formId)] = job
    }

    fun pause(uri: Uri, formId: String?) {
        val jobKey = key(uri, formId)
        activeJobs[jobKey]?.cancel()
        activeJobs.remove(jobKey)

    }

    fun resume(
        uri: Uri,
        formId: String,
        onProgress: (Long, Long) -> Unit,
        onComplete: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        uploadFile(uri, formId, onProgress, onComplete, onError)
    }
}
