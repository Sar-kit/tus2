package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.network.MediaItem

@Composable
fun MediaCarousel(media: List<MediaItem>) {
    if (media.isEmpty()) {
        Text("No files")
        return
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(media) { item ->
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(160.dp)
            ) {
                when {
                    item.url == null -> PendingBox()
                    item.mimeType?.startsWith("video") == true -> VideoPlayerBox(item.url)
                    item.mimeType?.startsWith("image") == true -> ImageBox(item.url)
                    else -> UnknownFileBox()
                }
            }
        }
    }
}
