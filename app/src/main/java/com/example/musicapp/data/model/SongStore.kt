package com.example.musicapp.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.musicapp.data.model.Song

object SongStore {

    fun loadDeviceSongs(context: Context): List<Song> {
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
            MediaStore.Audio.Media.DURATION
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC}=1 AND ${MediaStore.Audio.Media.DURATION}>=?"
        val selectionArgs = arrayOf(5_000.toString()) // bỏ file quá ngắn
        val sort = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, selectionArgs, sort)
            ?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val title = c.getString(iTitle) ?: "Unknown"
                    val artist = c.getString(iArtist)
                    val album = c.getString(iAlbum)
                    val uri = ContentUris.withAppendedId(collection, id)

                    out.add(
                        Song(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            uri = uri
                        )
                    )
                }
            }
        return out
    }
}
