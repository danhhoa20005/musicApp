package com.example.musicapp.data.model

import android.net.Uri

data class ArtistItem(
    val name: String,
    val count: Int,
    val cover: Uri? = null
)
