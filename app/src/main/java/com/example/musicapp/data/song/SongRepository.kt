package com.example.musicapp.data.song

import android.content.Context
import com.example.musicapp.data.model.Song

// SongRepository – truy cập danh sách bài hát + filter/sort + meta (favorite, lastPlayed)
object SongRepository {

    private var cache: List<Song>? = null     // cache bài hát

    // getAllSongs – lấy toàn bộ bài hát (raw) + ghép meta (favorite, lastPlayed)
    suspend fun getAllSongs(context: Context): List<Song> {
        if (cache == null) {
            // base – danh sách gốc đọc từ MediaStore
            val base = SongStore.loadAllSongs(context)

            // meta – đọc trạng thái isFavorite + lastPlayed từ SharedPreferences
            val meta = SongMetaStore.loadAll(context)

            cache = base.map { s ->
                val m = meta[s.id]
                if (m != null) {
                    val (fav, last) = m
                    s.copy(
                        isFavorite = fav,
                        lastPlayedAt = last
                    )
                } else {
                    s
                }
            }
        }
        return cache!!
    }

    // filter – lọc theo SongFilter (chưa sắp xếp)
    fun filter(songs: List<Song>, filter: SongFilter): List<Song> {
        return when (filter) {
            SongFilter.ALL -> songs                                // tất cả bài
            SongFilter.FAVORITE -> songs.filter { it.isFavorite }  // yêu thích
            SongFilter.RECENT -> songs.filter { it.lastPlayedAt > 0L } // đã từng phát
        }
    }

    // sortByAddedDate – sắp xếp theo ngày thêm (mới nhất trước)
    private fun sortByAddedDate(songs: List<Song>): List<Song> {
        return songs.sortedByDescending { it.dateAddedSec }
    }

    // sortByLastPlayed – sắp xếp theo thời điểm phát gần nhất
    private fun sortByLastPlayed(songs: List<Song>): List<Song> {
        return songs.sortedByDescending { it.lastPlayedAt }
    }

    // getSongs – API cho ViewModel: lấy + filter + sort
    suspend fun getSongs(context: Context, filter: SongFilter): List<Song> {
        val all = getAllSongs(context)
        val filtered = filter(all, filter)

        return when (filter) {
            SongFilter.RECENT -> sortByLastPlayed(filtered)
            SongFilter.ALL,
            SongFilter.FAVORITE -> sortByAddedDate(filtered)
        }
    }

    // updateFavorite – cập nhật isFavorite trong cache + lưu xuống SharedPreferences
    fun updateFavorite(context: Context, id: String, isFavorite: Boolean) {
        cache = cache?.map { s ->
            if (s.id == id) s.copy(isFavorite = isFavorite) else s
        }
        SongMetaStore.setFavorite(context, id, isFavorite)
    }

    // updateLastPlayed – cập nhật lastPlayedAt trong cache + lưu xuống SharedPreferences
    fun updateLastPlayed(context: Context, id: String, time: Long) {
        cache = cache?.map { s ->
            if (s.id == id) s.copy(lastPlayedAt = time) else s
        }
        SongMetaStore.setLastPlayed(context, id, time)
    }
}