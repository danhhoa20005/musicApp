package com.example.musicapp.data.song

// SongFilter - bộ lọc danh sách bài hát
enum class SongFilter {
    ALL,        // tất cả bài hát
    FAVORITE,   // chỉ bài hát yêu thích (isFavorite = true)
    RECENT      // bài hát phát gần đây (dựa theo lastPlayedAt)
}