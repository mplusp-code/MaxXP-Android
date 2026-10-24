package com.relay.app.push

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.matrix.MatrixAuthRepository
import com.relay.app.matrix.MatrixSyncEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class RelayNotificationListener : NotificationListenerService() {

    companion object {
        const val RELAY_PACKAGE = "com.relay.app"

        fun mapPackageToMessengerType(packageName: String): MessengerType? {
            if (packageName.equals(RELAY_PACKAGE, ignoreCase = true)) return null
            return when (packageName) {
                "com.whatsapp", "com.whatsapp.w4b" -> MessengerType.WHATSAPP
                "org.telegram.messenger", "org.telegram.messenger.web" -> MessengerType.TELEGRAM
                "com.instagram.android" -> MessengerType.INSTAGRAM
                "com.facebook.orca" -> MessengerType.FACEBOOK
                else -> null
            }
        }

        fun shouldIgnoreNotification(
            isOngoing: Boolean,
            isGroupSummary: Boolean,
            title: String?,
            text: String?
        ): Boolean {
            if (isOngoing) return true
            if (isGroupSummary) return true
            return title.isNullOrBlank() && text.isNullOrBlank()
        }

        fun normalizeSnippet(s: String): String {
            return s.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        }

        fun shouldDeduplicateAgainstMatrix(
            listenerMsg: MessageItem,
            matrixMessages: List<MessageItem>,
            windowMillis: Long = 120_000L
        ): Boolean {
            val listenerNormSnippet = normalizeSnippet(listenerMsg.snippet)
            val listenerSender = listenerMsg.sender.trim()

            return matrixMessages.any { matrix ->
                if (matrix.serviceType != listenerMsg.serviceType) return@any false
                val sameSender = matrix.sender.trim().equals(listenerSender, ignoreCase = true)
                val sameSnippet = normalizeSnippet(matrix.snippet) == listenerNormSnippet
                val timeDiff = if (matrix.timestampMillis > 0L && listenerMsg.timestampMillis > 0L) {
                    abs(matrix.timestampMillis - listenerMsg.timestampMillis)
                } else 0L

                sameSender && sameSnippet && timeDiff <= windowMillis
            }
        }

        fun extractQuickReplyAction(notification: Notification): NotificationCompat.Action? {
            val count = NotificationCompat.getActionCount(notification)
            for (i in 0 until count) {
                val action = NotificationCompat.getAction(notification, i)
                if (action?.remoteInputs != null && action.remoteInputs!!.isNotEmpty()) {
                    return action
                }
            }
            return null
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val pkg = sbn.packageName.orEmpty()
        val serviceType = mapPackageToMessengerType(pkg) ?: return

        val authRepo = MatrixAuthRepository(applicationContext, MatrixSyncEngine.createDefaultHttpClient())
        val prefs = authRepo.loadSetupPreferences()
        if (!prefs.useNotificationListenerFallback) return

        val notification = sbn.notification ?: return
        val isOngoing = sbn.isOngoing
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)
        val rawTitle = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val rawText = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()

        val sender: String
        val snippet: String

        if (messagingStyle != null && messagingStyle.messages.isNotEmpty()) {
            val latestMessage = messagingStyle.messages.last()
            sender = latestMessage.person?.name?.toString()?.trim()
                ?: messagingStyle.conversationTitle?.toString()?.trim()
                ?: rawTitle.orEmpty().ifBlank { serviceType.displayName }
            snippet = latestMessage.text?.toString()?.trim() ?: rawText.orEmpty()
        } else {
            sender = rawTitle.orEmpty().ifBlank { serviceType.displayName }
            snippet = rawText.orEmpty()
        }

        if (shouldIgnoreNotification(isOngoing, isGroupSummary, sender, snippet)) {
            return
        }

        val action = extractQuickReplyAction(notification)
        val senderHash = sender.lowercase(Locale.ROOT).hashCode()
        val roomId = "!nl_${pkg}_$senderHash"

        if (action != null) {
            IncomingMessageRepository.storeReplyAction(roomId, action)
        }

        val savedAccounts = authRepo.loadSavedAccounts()
        val matchingAccount = savedAccounts.firstOrNull { it.serviceType == serviceType }
        val accountId = matchingAccount?.id ?: "${serviceType.letter.lowercase()}_main"

        val postTime = if (sbn.postTime > 0L) sbn.postTime else System.currentTimeMillis()
        val timeLabel = SimpleDateFormat("HH:mm", Locale.US).format(Date(postTime))

        val messageItem = MessageItem(
            id = roomId,
            roomId = roomId,
            sender = sender,
            snippet = snippet,
            timestamp = timeLabel,
            serviceType = serviceType,
            accountId = accountId,
            isUnread = true,
            timestampMillis = postTime
        )

        IncomingMessageRepository.addMessage(messageItem)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: keep messages in list so user can see recent history
    }
}
