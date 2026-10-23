package com.relay.app.matrix

import com.relay.app.data.MessageItem
import com.relay.app.data.RelayUiState

class QuickReplyManager(
    private val syncEngine: MatrixSyncEngine
) {
    /**
     * Optimistically updates [RelayUiState] when the user submits a quick reply:
     * - Marks the replied message item as read (`isUnread = false`) and updates snippet to `"You: $replyText"`.
     * - Decrements the unread counter on the corresponding [com.relay.app.data.ServiceSummary] and [com.relay.app.data.AccountItem].
     */
    fun applyOptimisticReply(
        currentState: RelayUiState,
        targetRoomId: String,
        replyText: String
    ): RelayUiState {
        val targetMsg: MessageItem = currentState.messages.find {
            it.roomId == targetRoomId || it.id == targetRoomId
        } ?: return currentState

        val wasUnread = targetMsg.isUnread
        val updatedMessages = currentState.messages.map { msg ->
            if (msg.roomId == targetRoomId || msg.id == targetRoomId) {
                msg.copy(
                    snippet = "You: $replyText",
                    timestamp = "Just now",
                    isUnread = false
                )
            } else msg
        }

        val updatedServices = if (wasUnread) {
            currentState.services.map { svc ->
                if (svc.type == targetMsg.serviceType) {
                    svc.copy(unreadCount = (svc.unreadCount - 1).coerceAtLeast(0))
                } else svc
            }
        } else currentState.services

        val updatedAccounts = if (wasUnread) {
            currentState.accounts.map { acc ->
                if (acc.id == targetMsg.accountId && acc.unreadCount > 0) {
                    acc.copy(unreadCount = (acc.unreadCount - 1).coerceAtLeast(0))
                } else acc
            }
        } else currentState.accounts

        return currentState.copy(
            messages = updatedMessages,
            services = updatedServices,
            accounts = updatedAccounts,
            quickReplyStates = currentState.quickReplyStates + (targetRoomId to QuickReplyStatus.SENT)
        )
    }

    /**
     * Dispatches the reply over Matrix REST API without waking any hibernated WebView.
     */
    suspend fun sendQuickReply(
        roomId: String,
        replyText: String
    ): Result<String> {
        val trimmed = replyText.trim()
        require(trimmed.isNotEmpty()) { "Quick reply text cannot be blank" }
        return syncEngine.sendTextMessage(roomId = roomId, body = trimmed)
    }
}
