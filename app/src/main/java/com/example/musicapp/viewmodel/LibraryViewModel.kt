package com.example.musicapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.musicapp.data.SongFilter
import com.example.musicapp.data.SongStore
import com.example.musicapp.data.model.Song

// LibraryViewModel - dữ liệu thư viện nhạc theo bộ lọc, có cache
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    // songs - danh sách bài hát hiện có
    private val _songs = MutableLiveData<List<Song>>(emptyList())
    val songs: LiveData<List<Song>> = _songs

    // load - nạp danh sách theo bộ lọc (đồng bộ; nếu cần có thể chuyển sang coroutine)
    fun load(filter: SongFilter) {
        val ctx = getApplication<Application>()
        _songs.value = SongStore.loadDeviceSongs(ctx, filter)
    }

    // clear - xóa dữ liệu khi mất quyền/không có bài
    fun clear() { _songs.value = emptyList() }
}
