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

// Service phát nhạc chạy nền: quản lý ExoPlayer, MediaSession, notification
class MusicService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_channel"   // id kênh thông báo
        private const val NOTIFICATION_ID = 1001         // id notification
    }

    // Binder cho Activity/Fragment bind tới Service
    inner class LocalBinder : Binder() { fun getService() = this@MusicService }
    private val binder = LocalBinder()

    // Thành phần chính
    private lateinit var player: ExoPlayer               // engine phát nhạc
    private var session: MediaSession? = null            // MediaSession để hệ thống điều khiển
    private var notifier: PlayerNotificationManager? = null // cập nhật notification media
    private var playlist: List<Song> = emptyList()       // danh sách bài đang dùng

    override fun onCreate() {
        super.onCreate()

        // Tạo kênh thông báo (nhẹ, không rung/âm thanh)
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
                enableLights(false)
            }
            nm.createNotificationChannel(ch)
        }

        // Khởi tạo ExoPlayer + cấu hình audio
        player = ExoPlayer.Builder(this).build().apply {
            val attrs = androidx.media3.common.AudioAttributes
                .Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(attrs, true)              // xin audio focus khi phát
        }

        // MediaSession để hiển thị/điều khiển từ hệ thống/BT
        session = MediaSession.Builder(this, player).build()

        // Tạo notifier hiển thị notification media
        notifier = PlaybackNotification.create(
            context = this,
            session = session!!,
            player = player,
            channelId = CHANNEL_ID,
            notificationId = NOTIFICATION_ID
        )

        // Lắng nghe trạng thái player để cập nhật notification/foreground
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // Đang phát: gắn player vào notifier + đưa lên foreground
                    notifier?.setPlayer(player)
                    showForegroundNow("Đang phát nhạc")
                } else {
                    // Tạm dừng: GIỮ notification để người dùng phát lại, hạ foreground
                    notifier?.setPlayer(player)
                    stopForeground(false)                // không xóa notification
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                // Khi Idle/Ended: ẩn notification hẳn
                if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    notifier?.setPlayer(null)
                    hideNotification()
                }
            }
        })
        // Không startForeground ở đây; chỉ khi bắt đầu phát
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Giữ service sống khi bị dọn; pause không làm mất service
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        // Dọn tài nguyên khi service bị hủy
        hideNotification()
        notifier?.setPlayer(null)
        session?.release()
        player.release()
        super.onDestroy()
    }

    // ---------- API điều khiển phát nhạc ----------

    // Gán playlist vào player, chuẩn bị và có thể phát ngay
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

    fun play() = player.play()                          // phát
    fun pause() = player.pause()                        // tạm dừng (không tắt service)
    fun toggle() = if (player.isPlaying) pause() else play()
    fun next() = player.seekToNext()                    // bài kế
    fun previous() = player.seekToPrevious()            // bài trước
    fun seekTo(ms: Int) = player.seekTo(ms.toLong())    // tua

    fun isPlaying() = player.isPlaying                  // trạng thái phát
    fun duration() = player.duration.toInt().coerceAtLeast(0)
    fun position() = player.currentPosition.toInt()
    fun currentSong() = playlist.getOrNull(player.currentMediaItemIndex)
    fun getPlaylist() = playlist

    fun addPlayerListener(l: Player.Listener) { player.addListener(l) }    // lắng nghe từ UI
    fun removePlayerListener(l: Player.Listener) { player.removeListener(l) }

    fun toggleShuffle() { player.shuffleModeEnabled = !player.shuffleModeEnabled } // ngẫu nhiên
    fun isShuffleOn(): Boolean = player.shuffleModeEnabled

    fun cycleRepeat() {                                   // đổi chế độ lặp: OFF -> ONE -> ALL
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }
    fun getRepeatMode(): Int = player.repeatMode

    // Dừng hẳn (tuỳ chọn gắn vào nút Close)
    fun stopAllAndQuit() {
        runCatching { player.stop() }
        stopForeground(true)                               // xóa notification
        stopSelf()                                         // tắt service
    }



    // Đưa service lên foreground bằng một thông báo tối giản
    private fun showForegroundNow(text: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Hòa đẹp trai siêu cấp vippro 9x ")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .build()
        startForeground(NOTIFICATION_ID, n)
    }

    // Hạ foreground và hủy notification
    private fun hideNotification() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }
}
