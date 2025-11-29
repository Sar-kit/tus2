package com.example.myapplication.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil.compose.rememberAsyncImagePainter

@Composable
fun ImageBox(url: String) {
    androidx.compose.foundation.Image(
        painter = rememberAsyncImagePainter(url),
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
    )
}
