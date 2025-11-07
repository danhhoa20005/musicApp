package com.example.musicapp.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.musicapp.data.model.Song

object SongStore {

    private const val MIN_DURATION_MS = 5_000

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

        // Chỉ lấy nhạc thật và đủ dài
        val selection = "${MediaStore.Audio.Media.IS_MUSIC}=1 AND ${MediaStore.Audio.Media.DURATION}>=?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())

        // Sắp xếp A→Z theo tiêu đề
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
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
                // displayName vẫn đọc nhưng không dùng thêm bộ lọc nào
                // val displayName = cursor.getString(iDisplayName)

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

        return out
    }
}
