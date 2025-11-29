package com.example.myapplication.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.AppConfig
import com.example.myapplication.compress.CompressionManager
import kotlinx.coroutines.launch
import com.example.myapplication.network.ApiService
import com.example.myapplication.network.CreateFormRequest
import com.example.myapplication.upload.UploadManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    context: Context,
    api: ApiService
) {
    val scope = rememberCoroutineScope()
    val uploadManager = remember {
        UploadManager(context, AppConfig.TUS_ENDPOINT)
    }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var formId by remember { mutableStateOf<String?>(null) }

    // Picked files
    var files by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // UI state for uploads
    val uploadState = remember {
        mutableStateMapOf<Uri, UploadItemState>()
    }

    val compressionManager = remember { CompressionManager(context) }


    // File picker launcher
    val pickFilesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris != null) {
                files = uris
                uris.forEach { uri ->

                    // Create UI state
                    val state = UploadItemState()
                    uploadState[uri] = state

                    // Detect mime type
                    val mimeType = context.contentResolver.getType(uri)

                    // If it's an image → compress immediately
                    if (mimeType?.startsWith("image") == true) {
                        scope.launch {
                            state.status = "Compressing…"
                            state.isCompressing = true

                            val compressedUri = compressionManager.compressImage(
                                uri,
                                onStart = {
                                    state.isCompressing = true
                                    state.status = "Compressing…"
                                },
                                onComplete = { compressedUri, beforeKb, afterKb ->
                                    state.isCompressing = false
                                    state.status = "Compressed: ${beforeKb}KB → ${afterKb}KB"
                                    state.compressedUri = compressedUri     // ✅ THIS NOW WORKS
                                }
                            )
                        }
                    } else {
                        // Non-image
                        state.status = "Ready"
                    }
                }
            }
        }

    Column(Modifier.padding(16.dp)) {

        Text("Create Form", style = MaterialTheme.typography.titleLarge)

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = { pickFilesLauncher.launch(arrayOf("*/*")) }) {
            Text("Add Files")
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                Log.d("FormScreen", "Submit button CLICKED")
                scope.launch {
                    Log.d("FormScreen", "Coroutine started")

                    // Step 1: create form entry
                    val res = api.createForm(CreateFormRequest(title, description))
                    formId = res.id
                    Log.d("FormScreen create resp", "Coroutine started")
                    Log.d("FormScreen", "Form ID: ${res.id}")


                    // Step 2: start uploads
                    files.forEach { uri ->
                        uploadState[uri]?.status = "Uploading"
                        Log.d("FormScreen", "${uri}")

                        uploadManager.uploadFile(
                            uri,
                            formId!!,
                            onProgress = { uploaded, total ->
                                uploadState[uri]?.apply {
                                    this.uploaded = uploaded
                                    this.total = total
                                }
                            },
                            onComplete = { uploadedUrl ->
                                uploadState[uri]?.apply {
                                    status = "Completed"
                                    url = uploadedUrl
                                }
                            },
                            onError = { e ->
                                uploadState[uri]?.apply {
                                    status = "Error: ${e.message}"
                                }
                            }
                        )
                    }
                }
            },
            enabled = title.isNotBlank()
        ) {
            Text("Submit")
        }

        Spacer(Modifier.height(20.dp))

        // Display upload list
        files.forEach { uri ->
            val state = uploadState[uri] ?: UploadItemState()

            Column(Modifier.padding(vertical = 8.dp)) {
                Text("File: ${uri.lastPathSegment}")

                LinearProgressIndicator(
                    progress = if (state.total > 0) state.uploaded.toFloat() / state.total else 0f,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Status: ${state.status}")

                Row {
                    Button(onClick = {
                        uploadManager.pause(uri,formId)
                        state.status = "Paused"
                    }) {
                        Text("Pause")
                    }

                    Spacer(Modifier.width(12.dp))

                    Button(onClick = {
                        uploadManager.resume(
                            uri,
                            formId ?: return@Button,
                            onProgress = { uploaded, total ->
                                state.uploaded = uploaded
                                state.total = total
                            },
                            onComplete = { url ->
                                state.status = "Completed"
                                state.url = url
                            },
                            onError = {
                                state.status = "Error: ${it.message}"
                            }
                        )
                        state.status = "Uploading"
                    }) {
                        Text("Resume")
                    }
                }
            }
        }
    }
}

class UploadItemState(
    uploaded: Long = 0,
    total: Long = 0,
    status: String = "Pending",
    url: String? = null,
    compressedUri: Uri? = null,
    isCompressing: Boolean = false
) {
    var uploaded by mutableStateOf(uploaded)
    var total by mutableStateOf(total)
    var status by mutableStateOf(status)
    var url by mutableStateOf(url)
    var compressedUri by mutableStateOf(compressedUri)

    var isCompressing by mutableStateOf(isCompressing)
}
