package com.relay.app.matrix

import com.relay.app.data.AccountItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.data.ServiceSummary
import com.relay.app.push.QuietHoursEvaluator
import com.relay.app.push.RelayPushReceiver
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun `QuietHoursEvaluator evaluates overnight 23_00 to 07_00 window accurately`() {
        // 23:30 (1410m) and 02:15 (135m) are inside 23:00..07:00
        assertTrue(QuietHoursEvaluator.isWithinQuietHours(23 * 60 + 30, "23:00", "07:00"))
        assertTrue(QuietHoursEvaluator.isWithinQuietHours(2 * 60 + 15, "23:00", "07:00"))
        // 14:45 (885m) and 07:00 (420m) are outside 23:00..07:00
        assertFalse(QuietHoursEvaluator.isWithinQuietHours(14 * 60 + 45, "23:00", "07:00"))
        assertFalse(QuietHoursEvaluator.isWithinQuietHours(7 * 60, "23:00", "07:00"))
    }

    @Test
    fun `MatrixPusherSetRequest serializes HTTP pusher payload for Conduit`() {
        val endpoint = "https://max-os.mooo.com:8449/up_device_topic_abc?up=1"
        val req = MatrixSyncEngine.buildPusherSetRequest(pushkey = endpoint)
        val json = MatrixSyncEngine.sharedJson.encodeToString(req)
        assertEquals(endpoint, req.pushkey)
        assertEquals("http", req.kind)
        assertEquals("https://max-os.mooo.com:8449/_matrix/push/v1/notify", req.data.url)
        assertEquals("event_id_only", req.data.format)
        assertTrue(json.contains("\"app_id\":\"com.relay.messenger\""))
        assertTrue(json.contains("\"kind\":\"http\""))
        assertTrue(json.contains("\"event_id_only\""))

        val unregisterReq = MatrixSyncEngine.buildPusherSetRequest(pushkey = endpoint, enable = false)
        assertNull(unregisterReq.kind)

        val authRepo = MatrixAuthRepository(null, MatrixSyncEngine.createDefaultHttpClient())
        assertNull(authRepo.getPushEndpoint())
        authRepo.savePushEndpoint(endpoint)
        assertEquals(endpoint, authRepo.getPushEndpoint())
    }

    @Test
    fun `RelayPushReceiver parsePushPayload extracts room_id and event_id from event_id_only payloads`() {
        val nestedGatewayJson = """
            {
              "notification": {
                "event_id": "${'$'}ev_meta_99",
                "room_id": "!ig_portal:max-os.mooo.com",
                "counts": { "unread": 2 }
              }
            }
        """.trimIndent()

        val parsedNested = RelayPushReceiver.parsePushPayload(nestedGatewayJson)
        assertEquals("!ig_portal:max-os.mooo.com", parsedNested.roomId)
        assertEquals("${'$'}ev_meta_99", parsedNested.eventId)
        assertNull(parsedNested.body)

        val flatJson = """{"event_id":"${'$'}ev_flat_1","room_id":"!fb_room:max-os.mooo.com"}"""
        val parsedFlat = RelayPushReceiver.parsePushPayload(flatJson)
        assertEquals("!fb_room:max-os.mooo.com", parsedFlat.roomId)
        assertEquals("${'$'}ev_flat_1", parsedFlat.eventId)
        assertNull(parsedFlat.body)
    }
}
