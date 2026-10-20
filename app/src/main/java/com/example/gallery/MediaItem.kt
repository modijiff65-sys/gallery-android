package com.example.gallery

import android.net.Uri
import java.io.Serializable

data class MediaItem(
    val id: Long,
    val uriString: String,
    val name: String,
    val dateMs: Long,
    val bucket: String,
    val relativePath: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val mimeType: String
) : Serializable {
    val uri: Uri get() = Uri.parse(uriString)
}

data class Album(
    val name: String,
    val count: Int,
    val coverUriString: String
) : Serializable

data class QuickCard(
    val key: String,
    val title: String,
    val count: Int,
    val coverUriString: String,
    val iconRes: Int
) : Serializable

data class SmartCategory(
    val key: String,
    val title: String,
    val count: Int,
    val coverUriString: String,
    val iconRes: Int
) : Serializable

sealed class GridItem {
    data class Header(val title: String) : GridItem()
    data class Media(val item: MediaItem) : GridItem()
}
