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

    private val sampleMultiBridgeSyncJson = """
        {
          "next_batch": "s1042_conduit_maxos",
          "rooms": {
            "join": {
              "!wa_marta:max-os.mooo.com": {
                "unread_notifications": { "notification_count": 6, "highlight_count": 1 },
                "state": {
                  "events": [
                    { "type": "m.room.name", "content": { "name": "Marta K." } },
                    { "type": "m.room.bridged", "content": { "bridge": "whatsapp", "account_id": "wa_personal" } }
                  ]
                },
                "timeline": {
                  "events": [
                    {
                      "event_id": "${'$'}ev_wa_1",
                      "type": "m.room.message",
                      "sender": "@whatsapp_48501200300:max-os.mooo.com",
                      "origin_server_ts": 1761216960000,
                      "content": { "msgtype": "m.text", "body": "Are you free at 17:00?" }
                    }
                  ]
                }
              },
              "!tg_devteam:max-os.mooo.com": {
                "unread_notifications": { "notification_count": 7, "highlight_count": 2 },
                "state": {
                  "events": [
                    { "type": "m.room.name", "content": { "name": "Dev Team" } },
                    { "type": "m.room.bridged", "content": { "bridge": "telegram", "account_id": "tg_personal" } }
                  ]
                },
                "timeline": {
                  "events": [
                    {
                      "event_id": "${'$'}ev_tg_1",
                      "type": "m.room.message",
                      "sender": "@telegram_9918273:max-os.mooo.com",
                      "origin_server_ts": 1761216240000,
                      "content": { "msgtype": "m.text", "body": "Build 142 is green, ready to merge" }
                    }
                  ]
                }
              }
            }
          }
        }
    """.trimIndent()

    @Test
    fun `initial cached sync snapshot starts completely empty with zero test rooms`() {
        val batch = parser.parseRawSyncJson(MatrixAuthRepository.INITIAL_CACHED_SYNC_JSON)
        assertTrue(batch.messages.isEmpty())
        assertTrue(batch.roomUnreadCounts.isEmpty())
    }

    @Test
    fun `parseSyncResponse filters out Conduit Admin Room and appservice registered messages`() {
        val adminRoomJson = """
            {
              "next_batch": "s_admin",
              "rooms": {
                "join": {
                  "!conduit_admin:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 2 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "Admin Room" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_admin_1",
                          "type": "m.room.message",
                          "sender": "@conduit:max-os.mooo.com",
                          "origin_server_ts": 1761217000000,
                          "content": { "msgtype": "m.text", "body": "Appservice registered: whatsapp" }
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val batch = parser.parseRawSyncJson(adminRoomJson, selfUserId = "@admin:max-os.mooo.com")
        assertTrue(batch.messages.isEmpty())
        assertTrue(batch.serviceUnreadTotals.isEmpty())
    }

    @Test
    fun `parseRawSyncJson deserializes Conduit sync payload and maps Mautrix service badges`() {
        val batch = parser.parseRawSyncJson(
            rawJson = sampleMultiBridgeSyncJson,
            selfUserId = "@admin:max-os.mooo.com"
        )

        assertEquals(2, batch.messages.size)

        val firstMsg = batch.messages.first()
        assertEquals("!wa_marta:max-os.mooo.com", firstMsg.roomId)
        assertEquals("Marta K.", firstMsg.sender)
        assertEquals("Are you free at 17:00?", firstMsg.snippet)
        assertEquals(MessengerType.WHATSAPP, firstMsg.serviceType)
        assertEquals("wa_personal", firstMsg.accountId)
        assertTrue(firstMsg.isUnread)

        assertEquals(6, batch.serviceUnreadTotals[MessengerType.WHATSAPP])
        assertEquals(7, batch.serviceUnreadTotals[MessengerType.TELEGRAM])
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
        val batch = parser.parseRawSyncJson(sampleMultiBridgeSyncJson)
        val dummyAuth = MatrixAuthRepository(null, MatrixSyncEngine.createDefaultHttpClient())
        val dummyEngine = MatrixSyncEngine(dummyAuth, parser) {}
        val quickReplyManager = QuickReplyManager(dummyEngine)

        val initialState = RelayUiState(
            messages = batch.messages,
            services = listOf(
                ServiceSummary(MessengerType.WHATSAPP, accountCount = 1, unreadCount = 6)
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
        assertEquals(5, updatedState.services.first().unreadCount)
        assertEquals(5, updatedState.accounts.first().unreadCount)
        assertEquals(QuickReplyStatus.SENT, updatedState.quickReplyStates["!wa_marta:max-os.mooo.com"])
    }
}
