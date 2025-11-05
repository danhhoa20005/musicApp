@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.musicapp.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.example.musicapp.data.model.Song

class MusicService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 1001
    }

    inner class LocalBinder : Binder() { fun getService() = this@MusicService }
    private val binder = LocalBinder()

    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var notifier: PlayerNotificationManager? = null
    private var playlist: List<Song> = emptyList()

    override fun onCreate() {
        super.onCreate()

        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        player = ExoPlayer.Builder(this).build().apply {
            val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
        }

        session = MediaSession.Builder(this, player).build()

        notifier = PlaybackNotification.create(
            context = this,
            session = session!!,
            player = player,
            channelId = CHANNEL_ID,
            notificationId = NOTIFICATION_ID
        )

        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("MusicApp")
                .setContentText("Đang phát nhạc")
                .build()
        )
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        notifier?.setPlayer(null)
        session?.release()
        player.release()
        super.onDestroy()
    }

    fun setPlaylist(list: List<Song>, startIndex: Int = 0, playNow: Boolean = false) {
        playlist = list
        val items = list.map { s ->
            val meta = MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
                // Nếu đang dùng Media3 < 1.4.x mà dòng này báo lỗi, có thể comment tạm:
                .setAlbumTitle(s.album ?: "")
                .build()

            MediaItem.Builder()
                .setUri(s.uri)
                .setMediaId(s.id)
                .setMediaMetadata(meta)
                .build()
        }
        player.setMediaItems(items, startIndex, 0)
        player.prepare()
        if (playNow) player.play()
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun toggle() = if (player.isPlaying) pause() else play()
    fun next() = player.seekToNext()
    fun previous() = player.seekToPrevious()
    fun seekTo(ms: Int) = player.seekTo(ms.toLong())

    fun isPlaying() = player.isPlaying
    fun duration() = player.duration.toInt().coerceAtLeast(0)
    fun position() = player.currentPosition.toInt()
    fun currentSong() = playlist.getOrNull(player.currentMediaItemIndex)
    fun getPlaylist() = playlist
}
