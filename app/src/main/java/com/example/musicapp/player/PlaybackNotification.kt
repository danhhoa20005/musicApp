@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.musicapp.player

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Helper tạo PlayerNotificationManager cho thông báo và màn hình khóa
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

                override fun getCurrentContentTitle(p: androidx.media3.common.Player) =
                    p.mediaMetadata.title ?: "MusicApp"

                override fun createCurrentContentIntent(p: androidx.media3.common.Player): PendingIntent? {
                    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    return PendingIntent.getActivity(context, 0, launch, PendingIntent.FLAG_IMMUTABLE)
                }

                override fun getCurrentContentText(p: androidx.media3.common.Player) =
                    p.mediaMetadata.artist

                // ✅ Lấy ảnh bìa nhúng trong MP3 nếu có (chạy nền, callback khi xong)
                override fun getCurrentLargeIcon(
                    p: androidx.media3.common.Player,
                    cb: PlayerNotificationManager.BitmapCallback
                ): android.graphics.Bitmap? {
                    val item = p.currentMediaItem ?: return null
                    val uri = item.localConfiguration?.uri ?: return null

                    CoroutineScope(Dispatchers.IO).launch {
                        val bmp = com.example.musicapp.data.artwork.ArtworkUtils
                            .loadEmbeddedArtwork(context, uri)
                        if (bmp != null) withContext(Dispatchers.Main) {
                            cb.onBitmap(bmp)
                        }
                    }
                    // Trả null ngay; khi ảnh đọc xong sẽ gọi cb.onBitmap(bmp)
                    return null
                }
            })
            .build().apply {
                setSmallIcon(android.R.drawable.ic_media_play)
                @Suppress("DEPRECATION")
                setMediaSessionToken(session.sessionCompatToken)
                setPlayer(player)
            }
    }
}
