package com.relay.app.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.relay.app.MainActivity
import com.relay.app.R
import com.relay.app.data.MessengerType

object RelayNotificationManager {
    const val CHANNEL_HIGH_ID = "relay_messages_high"
    const val CHANNEL_QUIET_ID = "relay_messages_quiet"
    const val KEY_TEXT_REPLY = "key_relay_direct_reply"

    const val EXTRA_ROOM_ID = "extra_room_id"
    const val EXTRA_ACCOUNT_ID = "extra_account_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_SENDER_NAME = "extra_sender_name"
    const val EXTRA_SERVICE_NAME = "extra_service_name"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val highChannel = NotificationChannel(
            CHANNEL_HIGH_ID,
            "Relay Incoming Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High-priority alerts for incoming messenger chats with Direct Reply"
            enableVibration(true)
        }

        val quietChannel = NotificationChannel(
            CHANNEL_QUIET_ID,
            "Relay Quiet Hours Messages",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Silent notifications delivered during Quiet Hours (23:00–07:00)"
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannel(highChannel)
        nm.createNotificationChannel(quietChannel)
    }

    fun showIncomingMessageNotification(
        context: Context,
        roomId: String,
        accountId: String,
        sender: String,
        snippet: String,
        serviceType: MessengerType,
        isQuietHoursActive: Boolean = false
    ) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notificationId = (roomId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1001)
        val channelId = if (isQuietHoursActive) CHANNEL_QUIET_ID else CHANNEL_HIGH_ID

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_ACCOUNT_ID, accountId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Reply to $sender…")
            .build()

        val replyIntent = Intent(context, DirectReplyReceiver::class.java).apply {
            action = DirectReplyReceiver.ACTION_DIRECT_REPLY
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_ACCOUNT_ID, accountId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_SENDER_NAME, sender)
            putExtra(EXTRA_SERVICE_NAME, serviceType.displayName)
        }
        val replyPendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            replyIntent,
            replyPendingFlags
        )

        val replyAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()

        val titleText = "[${serviceType.letter}] $sender · ${serviceType.displayName}"
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(snippet)
            .setStyle(NotificationCompat.BigTextStyle().bigText(snippet))
            .setSubText(serviceType.displayName)
            .setColor(serviceType.badgeColorHex.toInt())
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(
                if (isQuietHoursActive) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH
            )
            .setSilent(isQuietHoursActive)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(replyAction)

        runCatching {
            nm.notify(notificationId, builder.build())
        }
    }

    fun updateNotificationReplied(
        context: Context,
        notificationId: Int,
        sender: String,
        serviceName: String,
        statusText: String
    ) {
        ensureChannels(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val builder = NotificationCompat.Builder(context, CHANNEL_QUIET_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$sender · $serviceName")
            .setContentText(statusText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .setTimeoutAfter(4_000L)

        runCatching {
            nm.notify(notificationId, builder.build())
        }
    }
}
