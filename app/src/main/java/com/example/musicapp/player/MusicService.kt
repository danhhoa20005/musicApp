@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.musicapp.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.example.musicapp.data.model.Song

// Service phát nhạc chạy nền
class MusicService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_channel"
        private const val NOTIFICATION_ID = 1001
    }

    // Cho Activity/Fragment bind vào Service
    inner class LocalBinder : Binder() { fun getService() = this@MusicService }
    private val binder = LocalBinder()

    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private var notifier: PlayerNotificationManager? = null
    private var playlist: List<Song> = emptyList()

    override fun onCreate() {
        super.onCreate()

        // Kênh thông báo (bắt buộc từ Android 8+)
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Music Playback",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        // Tạo ExoPlayer và khai báo phát nhạc
        player = ExoPlayer.Builder(this).build().apply {
            val attrs = androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(attrs, true)
        }

        // Tạo MediaSession để hệ thống điều khiển (màn khóa, tai nghe…)
        session = MediaSession.Builder(this, player).build()

        // Tạo thông báo media gắn với session + player
        notifier = PlaybackNotification.create(
            context = this,
            session = session!!,
            player = player,
            channelId = CHANNEL_ID,
            notificationId = NOTIFICATION_ID
        )

        // Đưa Service lên foreground với thông báo tối thiểu
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
        notifier?.setPlayer(null) // ngắt liên kết thông báo
        session?.release()        // giải phóng session
        player.release()          // tắt player
        super.onDestroy()
    }

    // Nạp danh sách bài và phát
    fun setPlaylist(list: List<Song>, startIndex: Int = 0, playNow: Boolean = false) {
        playlist = list

        val items = list.map { s ->
            val meta = MediaMetadata.Builder()
                .setTitle(s.title)
                .setArtist(s.artist)
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

    // Điều khiển
    fun play() = player.play()
    fun pause() = player.pause()
    fun toggle() = if (player.isPlaying) pause() else play()
    fun next() = player.seekToNext()
    fun previous() = player.seekToPrevious()
    fun seekTo(ms: Int) = player.seekTo(ms.toLong())

    // Trạng thái/thông tin
    fun isPlaying() = player.isPlaying
    fun duration() = player.duration.toInt().coerceAtLeast(0)
    fun position() = player.currentPosition.toInt()
    fun currentSong() = playlist.getOrNull(player.currentMediaItemIndex)
    fun getPlaylist() = playlist

    fun addPlayerListener(l: Player.Listener) { player.addListener(l) }
    fun removePlayerListener(l: Player.Listener) { player.removeListener(l) }


    // Shuffle
    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }
    fun isShuffleOn(): Boolean = player.shuffleModeEnabled

    // Repeat: OFF -> ONE -> ALL -> OFF ...
    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun getRepeatMode(): Int = player.repeatMode

}
