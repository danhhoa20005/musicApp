// com/example/musicapp/ui/util/CoverResolver.kt
package com.example.musicapp.ui.util

import android.net.Uri
import com.example.musicapp.data.artist.ArtistCovers
import com.example.musicapp.data.model.Song

object CoverResolver {

    /**
     * Ưu tiên: ảnh theo nghệ sĩ (map) -> artworkUri của bài -> defaultCoverUrl
     * Không lấy từ song.uri (vì đó là file âm thanh).
     */
    fun resolveArtwork(song: Song?, defaultCoverUrl: String? = null): Any? {
        if (song == null) return defaultCoverUrl
        coverForArtist(song.artist)?.let { return it }
        song.artworkUri?.let { return it }
        return defaultCoverUrl
    }

    fun coverForArtist(artistName: String?): Uri? {
        if (artistName.isNullOrBlank()) return null
        val url = ArtistCovers.covers[artistName]  // khớp đúng tên
        return url?.let(Uri::parse)
    }
}
