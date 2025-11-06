package com.example.musicapp.data.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache

object ArtworkCache {
    private val cache = object : LruCache<String, Bitmap>(32) {}
    fun get(key: String) = cache.get(key)
    fun put(key: String, bmp: Bitmap) = cache.put(key, bmp)
}

object ArtworkUtils {
    fun loadEmbeddedArtwork(context: Context, uri: Uri): Bitmap? {
        val key = uri.toString()
        ArtworkCache.get(key)?.let { return it }

        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val data = mmr.embeddedPicture ?: return null
            val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
            if (bmp != null) ArtworkCache.put(key, bmp)
            bmp
        } catch (_: Exception) {
            null
        } finally {
            mmr.release()
        }
    }
}
