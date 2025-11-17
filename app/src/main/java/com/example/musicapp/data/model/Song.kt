package com.example.musicapp.data.model

import android.net.Uri

// Song – thông tin 1 bài hát trong thư viện
data class Song(
    val id: String,              // id – MediaStore ID
    val title: String,           // tên bài
    val artist: String?,         // ca sĩ
    val album: String?,          // album
    val uri: Uri,                // uri phát
    val artworkUri: Uri? = null, // uri ảnh bìa (nếu có)
    val durationMs: Long = 0L,   // thời lượng (ms)
    val mimeType: String? = null,
    val dateAddedSec: Long = 0L, // thời điểm thêm vào máy (s)

    // yêu thích
    val isFavorite: Boolean = false,

    // thời điểm phát gần nhất (ms) – 0 = chưa từng phát
    val lastPlayedAt: Long = 0L
)
