package com.relay.app.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.relay.app.matrix.MatrixAuthRepository
import com.relay.app.matrix.MatrixRoomParser
import com.relay.app.matrix.MatrixSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles Direct Reply actions (`RemoteInput`) from the Android lockscreen or notification shade,
 * dispatching an `m.room.message` event directly to Matrix Conduit without opening the app UI
 * or waking hibernated WebViews.
 */
class DirectReplyReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DIRECT_REPLY = "com.relay.app.ACTION_DIRECT_REPLY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent) ?: return
        val replyText = remoteInputBundle.getCharSequence(RelayNotificationManager.KEY_TEXT_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (replyText.isEmpty()) return

        val roomId = intent.getStringExtra(RelayNotificationManager.EXTRA_ROOM_ID).orEmpty()
        val notificationId = intent.getIntExtra(RelayNotificationManager.EXTRA_NOTIFICATION_ID, 1001)
        val senderName = intent.getStringExtra(RelayNotificationManager.EXTRA_SENDER_NAME) ?: "Chat"
        val serviceName = intent.getStringExtra(RelayNotificationManager.EXTRA_SERVICE_NAME) ?: "Relay"

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (roomId.isNotBlank() && !roomId.endsWith(":webview")) {
                    val httpClient = MatrixSyncEngine.createDefaultHttpClient()
                    val authRepo = MatrixAuthRepository(context.applicationContext, httpClient)
                    val syncEngine = MatrixSyncEngine(
                        authRepository = authRepo,
                        roomParser = MatrixRoomParser(),
                        httpClient = httpClient,
                        onSyncUpdate = {}
                    )
                    syncEngine.sendTextMessage(roomId = roomId, body = replyText)
                }
                RelayNotificationManager.updateNotificationReplied(
                    context = context,
                    notificationId = notificationId,
                    sender = senderName,
                    serviceName = serviceName,
                    statusText = "✓ Sent: $replyText"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
