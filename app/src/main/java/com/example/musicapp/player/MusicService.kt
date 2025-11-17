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
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import com.example.musicapp.data.song.SongRepository          // dữ liệu bài hát + meta
import com.example.musicapp.data.model.Song

// MusicService - Service phát nhạc giữ ExoPlayer, MediaSession, notification
class MusicService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_channel"   // id kênh thông báo
        private const val NOTIFICATION_ID = 1001         // id notification
    }

    // LocalBinder - binder cho Activity/Fragment bind vào Service
    inner class LocalBinder : Binder() {
        fun getService() = this@MusicService
    }
    private val binder = LocalBinder()

    // player - ExoPlayer chính dùng để phát nhạc
    private lateinit var player: ExoPlayer

    // session - MediaSession để hệ thống (lockscreen, notification, headset…) điều khiển
    private var session: MediaSession? = null

    // notifier - quản lý notification điều khiển nhạc (play/pause, next/prev)
    private var notifier: PlayerNotificationManager? = null

    // playlist - danh sách bài hát hiện tại đang nạp vào player
    private var playlist: List<Song> = emptyList()

    // onCreate - khởi tạo player, session, notification channel, listener
    override fun onCreate() {
        super.onCreate()

        // 1. Tạo kênh thông báo cho việc phát nhạc (bắt buộc từ Android 8+)
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

        // 2. Khởi tạo ExoPlayer + audio attributes để báo cho hệ thống đây là media
        player = ExoPlayer.Builder(this).build().apply {
            val attrs = androidx.media3.common.AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            // tham số thứ 2 = true: tự xin/tự trả audio focus
            setAudioAttributes(attrs, true)
        }

        // 3. Tạo MediaSession để liên kết player với system UI (lockscreen, BT, car…)
        session = MediaSession.Builder(this, player).build()

        // 4. Tạo notification điều khiển playback gắn với MediaSession + Player
        notifier = PlaybackNotification.create(
            context = this,
            session = session!!,
            player = player,
            channelId = CHANNEL_ID,
            notificationId = NOTIFICATION_ID
        )

        // 5. Lắng nghe sự kiện từ player để:
        //    - bật/tắt foreground khi play/pause
        //    - ẩn notification khi dừng
        //    - cập nhật lastPlayed khi chuyển bài
        player.addListener(object : Player.Listener {

            // onIsPlayingChanged - player chuyển giữa play/pause
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // đảm bảo notifier đang trỏ tới player hiện tại
                notifier?.setPlayer(player)

                if (isPlaying) {
                    // đang phát → đưa service vào foreground để không bị kill
                    showForegroundNow("Đang phát nhạc")
                } else {
                    // pause → bỏ foreground nhưng giữ notification (stopForeground(false))
                    stopForeground(false)
                }
            }

            // onPlaybackStateChanged - trạng thái tổng thể của player (IDLE, BUFFERING, READY, ENDED)
            override fun onPlaybackStateChanged(state: Int) {
                // STATE_ENDED/IDLE → dừng phát hoàn toàn, bỏ player khỏi notifier và ẩn notif
                if (state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                    notifier?.setPlayer(null)
                    hideNotification()
                }
            }

            // onMediaItemTransition - chuyển sang MediaItem mới (bao gồm lần đầu play)
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = player.currentMediaItemIndex
                val song = playlist.getOrNull(idx) ?: return

                // mỗi khi sang bài mới → lưu lại thời điểm nghe gần nhất vào DB
                SongRepository.updateLastPlayed(
                    applicationContext,
                    song.id,
                    System.currentTimeMillis()
                )
            }
        })
    }

    // onStartCommand - giữ Service chạy (START_STICKY) khi hệ thống restart
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    // onBind - trả về binder cho Activity/Fragment
    override fun onBind(intent: Intent?): IBinder = binder

    // onDestroy - giải phóng player, session, notification khi Service bị hủy
    override fun onDestroy() {
        hideNotification()
        notifier?.setPlayer(null)
        session?.release()
        player.release()
        super.onDestroy()
    }

    // ----------------- API phát nhạc cho UI gọi -----------------

    // setPlaylist - nạp playlist vào player, chọn index bắt đầu, và play nếu được yêu cầu
    fun setPlaylist(list: List<Song>, startIndex: Int = 0, playNow: Boolean = false) {
        playlist = list

        // chuyển List<Song> → List<MediaItem> kèm metadata (title, artist, album)
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

        // set danh sách mới vào player, chọn index bắt đầu
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()

        // nếu playNow = true thì play luôn
        if (playNow) player.play()

        // không gọi updateLastPlayed ở đây, để onMediaItemTransition xử lý cho đồng bộ
    }

    // play - tiếp tục phát
    fun play() = player.play()

    // pause - tạm dừng phát
    fun pause() = player.pause()

    // toggle - nếu đang play thì pause, đang pause thì play
    fun toggle() = if (player.isPlaying) pause() else play()

    // next - chuyển sang bài tiếp theo, nếu hết danh sách thì quay về bài đầu
    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNext()
        } else if (player.mediaItemCount > 0) {
            // không còn next → quay về index 0
            player.seekToDefaultPosition(0)
        }
        // onMediaItemTransition sẽ được gọi → updateLastPlayed
    }

    // previous - chuyển sang bài trước, nếu không có thì nhảy về bài cuối
    fun previous() {
        if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        } else {
            val last = player.mediaItemCount - 1
            if (last >= 0) player.seekToDefaultPosition(last)
        }
        // onMediaItemTransition sẽ được gọi → updateLastPlayed
    }

    // seekTo - tua tới vị trí (ms) chỉ định
    fun seekTo(ms: Int) = player.seekTo(ms.toLong())

    // isPlaying - trả về trạng thái player đang play hay không
    fun isPlaying() = player.isPlaying

    // duration - tổng thời lượng bài hiện tại (ms, ép >= 0)
    fun duration() = player.duration.toInt().coerceAtLeast(0)

    // position - vị trí hiện tại trong bài (ms)
    fun position() = player.currentPosition.toInt()

    // currentSong - trả về Song hiện tại tương ứng với mediaItemIndex
    fun currentSong() = playlist.getOrNull(player.currentMediaItemIndex)

    // getPlaylist - trả về playlist hiện tại
    fun getPlaylist() = playlist

    // addPlayerListener - cho UI đăng ký lắng nghe event player trực tiếp
    fun addPlayerListener(l: Player.Listener) = player.addListener(l)

    // removePlayerListener - huỷ đăng ký listener
    fun removePlayerListener(l: Player.Listener) = player.removeListener(l)

    // isShuffleOn - trạng thái shuffle hiện tại
    fun isShuffleOn() = player.shuffleModeEnabled

    // cycleRepeat - xoay vòng chế độ repeat: OFF → ONE → ALL → OFF
    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // setShuffle - bật/tắt shuffle, có thể reshuffle lại thứ tự
    fun setShuffle(on: Boolean, reshuffle: Boolean = false) {
        player.shuffleModeEnabled = on
        if (on && reshuffle) reshuffleKeepCurrent()
    }

    // toggleShuffle - đảo trạng thái shuffle, nếu bật thì reshuffle lại
    fun toggleShuffle() {
        val turnedOn = !player.shuffleModeEnabled
        setShuffle(turnedOn, reshuffle = turnedOn)
    }

    // reshuffleKeepCurrent - tạo shuffle order mới, giữ nguyên bài hiện tại
    fun reshuffleKeepCurrent() {
        val n = player.mediaItemCount
        if (n <= 1) return
        val seed = System.currentTimeMillis()
        player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(n, seed))
    }

    // shufflePlayAll - nạp playlist, bật shuffle và phát toàn bộ ngẫu nhiên
    fun shufflePlayAll(list: List<Song>) {
        setPlaylist(list, startIndex = 0, playNow = true)
        setShuffle(on = true, reshuffle = true)
        val first = player.currentMediaItemIndex.takeIf { it != C.INDEX_UNSET } ?: 0
        player.seekToDefaultPosition(first)
    }

    // setRepeatOne - bật/tắt chế độ repeat 1 bài
    fun setRepeatOne(on: Boolean) {
        player.repeatMode = if (on) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // toggleRepeatOne - đảo trạng thái repeat 1
    fun toggleRepeatOne() {
        val isOne = player.repeatMode == Player.REPEAT_MODE_ONE
        setRepeatOne(!isOne)
    }

    // getRepeatMode - trả về repeatMode hiện tại
    fun getRepeatMode(): Int = player.repeatMode

    // stopAllAndQuit - dừng hẳn player, bỏ foreground và tự tắt Service
    fun stopAllAndQuit() {
        runCatching { player.stop() }
        stopForeground(true)
        stopSelf()
    }

    // showForegroundNow - đưa Service lên foreground với 1 notification đơn giản
    private fun showForegroundNow(text: String) {
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Hòa đẹp trai siêu cấp vippro 9x ")
            .setContentText(text)
            .setOngoing(true)   // kéo không tắt được, chỉ control bằng media button
            .setSilent(true)    // không rung, không kêu
            .build()
        startForeground(NOTIFICATION_ID, n)
    }

    // hideNotification - bỏ foreground và huỷ notification
    private fun hideNotification() {
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    // hasPrevious - UI kiểm tra có bài trước hay không (để enable/disable nút)
    fun hasPrevious() = player.hasPreviousMediaItem()

    // hasNext - UI kiểm tra có bài sau hay không (để enable/disable nút)
    fun hasNext() = player.hasNextMediaItem()
}
