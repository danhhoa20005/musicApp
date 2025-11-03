package com.example.musicapp.data.model

import android.net.Uri

data class Song(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val uri: Uri,
    val artworkUri: Uri? = null
)
