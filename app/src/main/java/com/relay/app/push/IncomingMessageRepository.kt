package com.relay.app.push

import androidx.core.app.NotificationCompat
import com.relay.app.data.MessageItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

object IncomingMessageRepository {
    private val _messages = MutableStateFlow<List<MessageItem>>(emptyList())
    val messages: StateFlow<List<MessageItem>> = _messages.asStateFlow()

    private val replyActions = ConcurrentHashMap<String, NotificationCompat.Action>()

    fun addMessage(message: MessageItem) {
        _messages.update { current ->
            val existingIndex = current.indexOfFirst { it.id == message.id || it.roomId == message.roomId }
            if (existingIndex >= 0) {
                current.toMutableList().apply { set(existingIndex, message) }
            } else {
                (listOf(message) + current).take(100)
            }
        }
    }

    fun storeReplyAction(roomIdOrKey: String, action: NotificationCompat.Action) {
        replyActions[roomIdOrKey] = action
    }

    fun getReplyAction(roomIdOrKey: String): NotificationCompat.Action? {
        return replyActions[roomIdOrKey]
    }

    fun clear() {
        _messages.value = emptyList()
        replyActions.clear()
    }
}
