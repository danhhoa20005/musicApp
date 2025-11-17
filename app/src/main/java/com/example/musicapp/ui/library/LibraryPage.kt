package com.example.musicapp.ui.library

import androidx.annotation.StringRes
import com.example.musicapp.data.song.SongFilter

// LibraryPage - mô tả 1 tab: tiêu đề + bộ lọc bài hát
data class LibraryPage(
    @StringRes val titleRes: Int, // titleRes - id chuỗi tiêu đề tab
    val filter: SongFilter         // filter - bộ lọc áp dụng cho tab
)
