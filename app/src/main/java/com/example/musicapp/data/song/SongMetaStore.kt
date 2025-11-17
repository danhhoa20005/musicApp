package com.example.musicapp.data.song

import android.content.Context
import kotlin.collections.iterator

// SongMetaStore – lưu meta cho bài hát: yêu thích + thời điểm phát gần nhất
object SongMetaStore {

    // Tên file SharedPreferences
    private const val PREF = "song_meta"
    // Prefix key cho trạng thái yêu thích: "fav_idBaiHat"
    private const val KEY_FAV = "fav_"
    // Prefix key cho thời điểm phát gần nhất: "last_idBaiHat"
    private const val KEY_LAST = "last_"

    // Hàm tiện ích lấy SharedPreferences
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    // Đọc toàn bộ meta đã lưu
    // Trả về map: idBaiHat -> (isFavorite, lastPlayedAt)
    fun loadAll(ctx: Context): Map<String, Pair<Boolean, Long>> {
        val all = prefs(ctx).all           // lấy hết key-value trong SharedPreferences
        val favMap = mutableMapOf<String, Boolean>() // map tạm cho yêu thích
        val lastMap = mutableMapOf<String, Long>()   // map tạm cho thời gian phát

        for ((key, value) in all) {
            when {
                // Key dạng "fav_id" và value là Boolean
                key.startsWith(KEY_FAV) && value is Boolean -> {
                    val id = key.removePrefix(KEY_FAV) // tách id từ key
                    favMap[id] = value
                }
                // Key dạng "last_id" và value là Long
                key.startsWith(KEY_LAST) && value is Long -> {
                    val id = key.removePrefix(KEY_LAST)
                    lastMap[id] = value
                }
            }
        }

        val res = mutableMapOf<String, Pair<Boolean, Long>>()
        // Hợp tất cả id xuất hiện ở favMap hoặc lastMap
        val ids = favMap.keys + lastMap.keys
        for (id in ids) {
            val fav = favMap[id] ?: false  // mặc định chưa thích
            val last = lastMap[id] ?: 0L   // mặc định chưa từng phát
            res[id] = fav to last
        }
        return res
    }

    // Lưu trạng thái yêu thích cho 1 bài hát
    fun setFavorite(ctx: Context, id: String, isFavorite: Boolean) {
        prefs(ctx).edit()
            .putBoolean(KEY_FAV + id, isFavorite)
            .apply()
    }

    // Lưu thời điểm phát gần nhất cho 1 bài hát
    fun setLastPlayed(ctx: Context, id: String, time: Long) {
        prefs(ctx).edit()
            .putLong(KEY_LAST + id, time)
            .apply()
    }
}