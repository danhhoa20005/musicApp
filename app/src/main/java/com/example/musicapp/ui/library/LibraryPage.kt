package com.example.musicapp.ui.library

import androidx.annotation.StringRes
import com.example.musicapp.data.SongFilter

data class LibraryPage(
    @StringRes val titleRes: Int,
    val filter: SongFilter
)
