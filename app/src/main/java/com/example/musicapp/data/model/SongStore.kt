package com.example.musicapp.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.musicapp.data.model.Song
import java.util.concurrent.TimeUnit

object SongStore {

    private const val MIN_DURATION_MS = 5_000
    private val MP3_MIME_TYPES = arrayOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/mpeg3",
        "audio/x-mpeg"
    )
    private val RECENT_WINDOW_SECONDS = TimeUnit.DAYS.toSeconds(30)

    fun loadDeviceSongs(context: Context, filter: SongFilter = SongFilter.ALL): List<Song> {
        val out = ArrayList<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        val selectionParts = mutableListOf(
            "${MediaStore.Audio.Media.IS_MUSIC}=1",
            "${MediaStore.Audio.Media.DURATION}>=?"
        )
        val selectionArgs = mutableListOf(MIN_DURATION_MS.toString())

        when (filter) {
            SongFilter.MP3_ONLY -> {
                val mimeClause = MP3_MIME_TYPES.joinToString(" OR ") {
                    "${MediaStore.Audio.Media.MIME_TYPE}=?"
                }
                selectionParts += "($mimeClause)"
                selectionArgs += MP3_MIME_TYPES
            }

            SongFilter.RECENT -> {
                val recentThreshold = (System.currentTimeMillis() / 1000) - RECENT_WINDOW_SECONDS
                selectionParts += "${MediaStore.Audio.Media.DATE_ADDED}>=?"
                selectionArgs += recentThreshold.toString()
            }

            else -> Unit
        }

        val sortOrder = when (filter) {
            SongFilter.RECENT -> "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            else -> "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        }

        context.contentResolver.query(
            collection,
            projection,
            selectionParts.joinToString(" AND "),
            selectionArgs.toTypedArray(),
            sortOrder
        )?.use { cursor ->
            val iId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val iTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val iArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val iAlbum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val iDuration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val iMime = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val iDateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val iDisplayName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(iId)
                val title = cursor.getString(iTitle) ?: "Unknown"
                val artist = cursor.getString(iArtist)
                val album = cursor.getString(iAlbum)
                val duration = cursor.getLong(iDuration).coerceAtLeast(0L)
                val mime = cursor.getString(iMime)
                val dateAdded = cursor.getLong(iDateAdded)
                val displayName = cursor.getString(iDisplayName)

                if (filter == SongFilter.MP3_ONLY && !isMp3(mime, displayName)) {
                    continue
                }

                val uri = ContentUris.withAppendedId(collection, id)

                out.add(
                    Song(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        uri = uri,
                        durationMs = duration,
                        mimeType = mime,
                        dateAddedSec = dateAdded
                    )
                )
            }
        }

        return when (filter) {
            SongFilter.RECENT -> out.sortedByDescending { it.dateAddedSec }
            else -> out
        }
    }

    private fun isMp3(mime: String?, displayName: String?): Boolean {
        val normalizedMime = mime?.lowercase()
        if (normalizedMime != null && MP3_MIME_TYPES.any { it == normalizedMime }) {
            return true
        }
        return displayName?.endsWith(".mp3", ignoreCase = true) == true
    }
}
