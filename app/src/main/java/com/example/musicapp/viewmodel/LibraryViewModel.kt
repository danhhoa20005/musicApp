package com.example.musicapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicapp.data.song.SongFilter
import com.example.musicapp.data.song.SongRepository
import com.example.musicapp.data.model.Song
import com.example.musicapp.data.model.ArtistItem
import kotlinx.coroutines.launch

// Quản lý dữ liệu thư viện: danh sách bài hát + danh sách nghệ sĩ
class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    // danh sách bài hát hiển thị
    private val _songs = MutableLiveData<List<Song>>(emptyList())
    val songs: LiveData<List<Song>> = _songs

    // danh sách nghệ sĩ (hàng ngang)
    private val _artists = MutableLiveData<List<ArtistItem>>(emptyList())
    val artists: LiveData<List<ArtistItem>> = _artists

    // nạp bài hát theo filter (luôn chạy trong coroutine)
    fun load(filter: SongFilter) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val ds = SongRepository.getSongs(ctx, filter)
            _songs.value = ds

            if (filter == SongFilter.ALL) {
                _artists.value = buildArtistList(ds)
            }
        }
    }

    // nạp riêng danh sách nghệ sĩ
    fun loadArtists() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val ds = SongRepository.getSongs(ctx, SongFilter.ALL)
            _artists.value = buildArtistList(ds)
        }
    }

    // xóa dữ liệu
    fun clear() {
        _songs.value = emptyList()
        _artists.value = emptyList()
    }

    // gom bài hát thành danh sách nghệ sĩ
    private fun buildArtistList(ds: List<Song>): List<ArtistItem> {
        val bad = setOf("unknown", "<unknown>")

        return ds
            .mapNotNull { it.artist?.trim()?.takeIf { s -> s.isNotEmpty() } }
            .groupBy { it.lowercase() }
            .mapNotNull { (_, list) ->
                val display = list.groupingBy { it }.eachCount()
                    .maxByOrNull { it.value }?.key

                display?.let { name ->
                    if (name.lowercase() in bad) null
                    else ArtistItem(name = name, count = list.size)
                }
            }
            .sortedByDescending { it.count }
            .take(12)
    }
}
