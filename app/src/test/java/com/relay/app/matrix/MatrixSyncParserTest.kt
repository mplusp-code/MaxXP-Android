package com.relay.app.matrix

import com.relay.app.data.AccountItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.data.ServiceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MatrixSyncParserTest {

    private val parser = MatrixRoomParser()

    @Test
    fun `parseRawSyncJson deserializes Conduit sync payload and maps Mautrix service badges`() {
        val batch = parser.parseRawSyncJson(
            rawJson = MatrixAuthRepository.INITIAL_CACHED_SYNC_JSON,
            selfUserId = "@admin:max-os.mooo.com"
        )

        // 7 bridged rooms in initial sync snapshot
        assertEquals(7, batch.messages.size)

        // Most recent room is Marta K. on WhatsApp Personal
        val firstMsg = batch.messages.first()
        assertEquals("!wa_marta:max-os.mooo.com", firstMsg.roomId)
        assertEquals("Marta K.", firstMsg.sender)
        assertEquals("Are you free at 17:00?", firstMsg.snippet)
        assertEquals(MessengerType.WHATSAPP, firstMsg.serviceType)
        assertEquals("wa_personal", firstMsg.accountId)
        assertTrue(firstMsg.isUnread)

        // Verify aggregated unread counts per service (WhatsApp: 6 + 6 = 12, Telegram: 7, Instagram: 3, Facebook: 2, GMessages: 4, Viber: 1)
        assertEquals(12, batch.serviceUnreadTotals[MessengerType.WHATSAPP])
        assertEquals(7, batch.serviceUnreadTotals[MessengerType.TELEGRAM])
        assertEquals(3, batch.serviceUnreadTotals[MessengerType.INSTAGRAM])
        assertEquals(2, batch.serviceUnreadTotals[MessengerType.FACEBOOK])
        assertEquals(4, batch.serviceUnreadTotals[MessengerType.GOOGLE_MESSAGES])
        assertEquals(1, batch.serviceUnreadTotals[MessengerType.VIBER])
        assertEquals(29, batch.serviceUnreadTotals.values.sum())
    }

    @Test
    fun `formatMessageSnippet converts media msgtypes into PDA compact labels`() {
        val mediaSyncJson = """
            {
              "next_batch": "s200",
              "rooms": {
                "join": {
                  "!tg_media:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 1 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "Telegram Media" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_audio",
                          "type": "m.room.message",
                          "sender": "@telegram_user:max-os.mooo.com",
                          "origin_server_ts": 1761217000000,
                          "content": { "msgtype": "m.audio", "body": "voice.ogg" }
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val batch = parser.parseRawSyncJson(mediaSyncJson)
        assertEquals(1, batch.messages.size)
        assertEquals(MessengerType.TELEGRAM, batch.messages.first().serviceType)
        assertEquals("[Voice message]", batch.messages.first().snippet)
    }

    @Test
    fun `parseSyncResponse extracts Mautrix WhatsApp bot QR code events`() {
        val botSyncJson = """
            {
              "next_batch": "s300",
              "rooms": {
                "join": {
                  "!dm_wabot:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 1 },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_qr",
                          "type": "m.room.message",
                          "sender": "@whatsappbot:max-os.mooo.com",
                          "origin_server_ts": 1761218000000,
                          "content": {
                            "msgtype": "m.image",
                            "body": "QR code",
                            "url": "mxc://max-os.mooo.com/QrCodeMediaId123"
                          }
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val batch = parser.parseRawSyncJson(botSyncJson)
        assertEquals(1, batch.bridgeBotUpdates.size)
        val qrUpdate = batch.bridgeBotUpdates.first()
        assertEquals(MessengerType.WHATSAPP, qrUpdate.serviceType)
        assertEquals("@whatsappbot:max-os.mooo.com", qrUpdate.botSender)
        assertEquals("mxc://max-os.mooo.com/QrCodeMediaId123", qrUpdate.mxcUrl)
        assertNotNull(qrUpdate.qrAsciiOrData)
    }

    @Test
    fun `QuickReplyManager applyOptimisticReply updates snippet and decrements unread counters`() {
        val batch = parser.parseRawSyncJson(MatrixAuthRepository.INITIAL_CACHED_SYNC_JSON)
        val dummyAuth = MatrixAuthRepository(null, MatrixSyncEngine.createDefaultHttpClient())
        val dummyEngine = MatrixSyncEngine(dummyAuth, parser) {}
        val quickReplyManager = QuickReplyManager(dummyEngine)

        val initialState = RelayUiState(
            messages = batch.messages,
            services = listOf(
                ServiceSummary(MessengerType.WHATSAPP, accountCount = 2, unreadCount = 12)
            ),
            accounts = listOf(
                AccountItem(
                    id = "wa_personal",
                    accountName = "WhatsApp · Personal",
                    serviceType = MessengerType.WHATSAPP,
                    statusText = "Connected",
                    isConnected = true,
                    unreadCount = 6
                )
            )
        )

        val updatedState = quickReplyManager.applyOptimisticReply(
            currentState = initialState,
            targetRoomId = "!wa_marta:max-os.mooo.com",
            replyText = "See you at 17:00!"
        )

        val repliedMessage = updatedState.messages.first { it.roomId == "!wa_marta:max-os.mooo.com" }
        assertEquals("You: See you at 17:00!", repliedMessage.snippet)
        assertFalse(repliedMessage.isUnread)
        assertEquals(11, updatedState.services.first().unreadCount)
        assertEquals(5, updatedState.accounts.first().unreadCount)
        assertEquals(QuickReplyStatus.SENT, updatedState.quickReplyStates["!wa_marta:max-os.mooo.com"])
    }
}
