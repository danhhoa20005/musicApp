@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.musicapp.player

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerNotificationManager

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
                    return PendingIntent.getActivity(
                        context, 0, launch, PendingIntent.FLAG_IMMUTABLE
                    )
                }

                override fun getCurrentContentText(p: androidx.media3.common.Player) =
                    p.mediaMetadata.artist

                override fun getCurrentLargeIcon(
                    p: androidx.media3.common.Player,
                    cb: PlayerNotificationManager.BitmapCallback
                ) = null
            })
            .build().apply {
                setSmallIcon(android.R.drawable.ic_media_play)

                @Suppress("DEPRECATION") // Media3 hiện vẫn dùng compat token cho PlayerNotificationManager
                setMediaSessionToken(session.sessionCompatToken)

                setPlayer(player)
            }
    }
}
