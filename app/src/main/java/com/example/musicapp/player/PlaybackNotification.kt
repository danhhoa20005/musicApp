package com.example.musicapp.player

import android.app.PendingIntent
import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import android.app.NotificationManager


object PlaybackNotification {

    fun create(
        context: Context,
        session: MediaSession,
        player: ExoPlayer,
        channelId: String,
        notificationId: Int
    ): PlayerNotificationManager {
        return PlayerNotificationManager.Builder(context, notificationId, channelId)
            .setChannelImportance(NotificationManager.IMPORTANCE_LOW)
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {

                // Tiêu đề bài hát (fallback "MusicApp" nếu trống)
                override fun getCurrentContentTitle(p: androidx.media3.common.Player) =
                    p.mediaMetadata.title ?: "MusicApp"

                // Khi chạm vào thông báo → mở lại ứng dụng
                override fun createCurrentContentIntent(p: androidx.media3.common.Player): PendingIntent? {
                    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    return PendingIntent.getActivity(
                        context, 0, launch, PendingIntent.FLAG_IMMUTABLE
                    )
                }

                // Tên nghệ sĩ/ca sĩ
                override fun getCurrentContentText(p: androidx.media3.common.Player) =
                    p.mediaMetadata.artist

                // Ảnh bìa lớn: hiện tạm null; bạn có thể tải Bitmap và gọi cb.onBitmap(bitmap)
                override fun getCurrentLargeIcon(
                    p: androidx.media3.common.Player,
                    cb: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .build().apply {
                // Icon nhỏ trên thanh trạng thái
                setSmallIcon(android.R.drawable.ic_media_play)

                // Quan trọng: dùng sessionCompatToken (không dùng session.token)
                setMediaSessionToken(session.sessionCompatToken)

                // Gắn player để notifier tự cập nhật tiêu đề, nghệ sĩ, nút điều khiển
                setPlayer(player)
            }
    }
}
