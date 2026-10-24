package com.relay.app.matrix

import com.relay.app.bridge.MetaBotReplyType
import com.relay.app.bridge.RelayBridgeListener
import com.relay.app.bridge.RelayJsBridge
import com.relay.app.bridge.RelaySessionManager
import com.relay.app.data.AccountItem
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.data.ServiceSummary
import com.relay.app.data.SetupPreferences
import com.relay.app.push.QuietHoursEvaluator
import com.relay.app.push.RelayNotificationListener
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

    @Test
    fun `mapRoomToAccount maps accurately for 0, 1, and 2 accounts per service`() {
        val waPersonal = AccountItem(id = "wa_personal", accountName = "WhatsApp · Personal", serviceType = MessengerType.WHATSAPP, statusText = "Connected", isConnected = true)
        val waWork = AccountItem(id = "wa_work", accountName = "WhatsApp · Work", serviceType = MessengerType.WHATSAPP, statusText = "Connected", isConnected = true)
        val tgPersonal = AccountItem(id = "tg_personal", accountName = "Telegram", serviceType = MessengerType.TELEGRAM, statusText = "Connected", isConnected = true)

        val accounts = listOf(waPersonal, waWork, tgPersonal)

        // 0 accounts for Instagram -> returns null
        val nullResult = MatrixRoomParser.mapRoomToAccount(
            roomId = "!ig_chat:max-os.mooo.com",
            roomService = MessengerType.INSTAGRAM,
            accounts = accounts
        )
        assertNull(nullResult)

        // 1 account for Telegram -> returns tgPersonal
        val tgResult = MatrixRoomParser.mapRoomToAccount(
            roomId = "!tg_chat:max-os.mooo.com",
            roomService = MessengerType.TELEGRAM,
            accounts = accounts
        )
        assertEquals(tgPersonal, tgResult)

        // 2 accounts for WhatsApp with explicitAccountId matching waWork -> returns waWork
        val explicitMatch = MatrixRoomParser.mapRoomToAccount(
            roomId = "!wa_chat:max-os.mooo.com",
            roomService = MessengerType.WHATSAPP,
            accounts = accounts,
            explicitAccountId = "wa_work"
        )
        assertEquals(waWork, explicitMatch)

        // 2 accounts for WhatsApp without explicit hint -> falls back to first account
        val firstFallback = MatrixRoomParser.mapRoomToAccount(
            roomId = "!wa_chat:max-os.mooo.com",
            roomService = MessengerType.WHATSAPP,
            accounts = accounts
        )
        assertEquals(waPersonal, firstFallback)
    }

    @Test
    fun `isScraperEnabled defaults to false on RelayJsBridge and SetupPreferences`() {
        val defaultPrefs = SetupPreferences()
        assertFalse(defaultPrefs.useWebViewScraperFallback)

        val dummyListener = object : RelayBridgeListener {
            override fun onUnreadCountChanged(sessionId: String, unreadCount: Int) {}
            override fun onNotificationReceived(sessionId: String, title: String, message: String, tag: String?) {}
            override fun onQrCodeStateChanged(sessionId: String, hasQrCode: Boolean) {}
            override fun onSessionReady(sessionId: String) {}
        }
        val bridge = RelayJsBridge("session_1", dummyListener)
        assertFalse(bridge.isScraperEnabled())

        val enabledBridge = RelayJsBridge("session_2", dummyListener, isScraperEnabledProvider = { true })
        assertTrue(enabledBridge.isScraperEnabled())
    }

    @Test
    fun `MessageItem sorts newest first by origin_server_ts`() {
        val msgOlder = com.relay.app.data.MessageItem(
            id = "!room1",
            sender = "Alice",
            snippet = "Older",
            timestamp = "10:00",
            serviceType = MessengerType.WHATSAPP,
            timestampMillis = 1700000000000L
        )
        val msgNewer = com.relay.app.data.MessageItem(
            id = "!room2",
            sender = "Bob",
            snippet = "Newer",
            timestamp = "10:05",
            serviceType = MessengerType.TELEGRAM,
            timestampMillis = 1700000300000L
        )

        val sorted = listOf(msgOlder, msgNewer).sortedByDescending { it.timestampMillis }
        assertEquals(msgNewer, sorted[0])
        assertEquals(msgOlder, sorted[1])
    }

    @Test
    fun `sync response with 0 accounts for a service produces 0 displayed messages for that service`() {
        // sampleMultiBridgeSyncJson has WhatsApp and Telegram rooms
        // We only provide a Telegram account -> WhatsApp messages must be omitted
        val onlyTelegram = listOf(
            AccountItem(id = "tg_personal", accountName = "Telegram", serviceType = MessengerType.TELEGRAM, statusText = "Connected", isConnected = true)
        )

        val batch = parser.parseRawSyncJson(
            rawJson = sampleMultiBridgeSyncJson,
            selfUserId = "@admin:max-os.mooo.com",
            accounts = onlyTelegram
        )

        assertEquals(1, batch.messages.size)
        assertEquals(MessengerType.TELEGRAM, batch.messages.first().serviceType)
        assertTrue(batch.messages.none { it.serviceType == MessengerType.WHATSAPP })
    }

    @Test
    fun `extractMetaCookiesFromMap returns null when required cookies are missing`() {
        val incompleteIg = mapOf("csrftoken" to "token123", "ds_user_id" to "12345") // missing sessionid
        assertNull(RelaySessionManager.extractMetaCookiesFromMap(incompleteIg, MessengerType.INSTAGRAM))

        val incompleteFb = mapOf("c_user" to "999", "datr" to "xyz") // missing xs
        assertNull(RelaySessionManager.extractMetaCookiesFromMap(incompleteFb, MessengerType.FACEBOOK))
    }

    @Test
    fun `parseCookieHeader and extractMetaCookiesFromMap parse and filter valid cookie sets`() {
        val rawIgCookie = "sessionid=ig_sess_abc; csrftoken=ig_csrf_xyz; ds_user_id=12345; rur=\"PRN\"; extra_unneeded=skip"
        val parsedMap = RelaySessionManager.parseCookieHeader(rawIgCookie)
        val extractedIg = RelaySessionManager.extractMetaCookiesFromMap(parsedMap, MessengerType.INSTAGRAM)
        assertNotNull(extractedIg)
        assertEquals("ig_sess_abc", extractedIg!!["sessionid"])
        assertEquals("ig_csrf_xyz", extractedIg["csrftoken"])
        assertEquals("12345", extractedIg["ds_user_id"])
        assertEquals("\"PRN\"", extractedIg["rur"])
        assertFalse(extractedIg.containsKey("extra_unneeded"))

        val rawFbCookie = "xs=2:fb_xs:123; c_user=98765; datr=d_val; fr=0abc; unneeded=val"
        val parsedFb = RelaySessionManager.parseCookieHeader(rawFbCookie)
        val extractedFb = RelaySessionManager.extractMetaCookiesFromMap(parsedFb, MessengerType.FACEBOOK)
        assertNotNull(extractedFb)
        assertEquals("2:fb_xs:123", extractedFb!!["xs"])
        assertEquals("98765", extractedFb["c_user"])
        assertEquals("d_val", extractedFb["datr"])
        assertEquals("0abc", extractedFb["fr"])
        assertFalse(extractedFb.containsKey("unneeded"))
    }

    @Test
    fun `serializeMetaCookiesToJson serializes and redactSensitiveStatus masks secret values`() {
        val cookies = mapOf("sessionid" to "secret_sess", "ds_user_id" to "42")
        val json = RelaySessionManager.serializeMetaCookiesToJson(cookies)
        assertTrue(json.startsWith("{") && json.endsWith("}"))
        assertTrue(json.contains("\"sessionid\":\"secret_sess\""))
        assertTrue(json.contains("\"ds_user_id\":\"42\""))

        val redacted = RelaySessionManager.redactSensitiveStatus("Bot response: \"sessionid\":\"secret_sess\" with ds_user_id=42")
        assertFalse(redacted.contains("secret_sess"))
        assertTrue(redacted.contains("\"sessionid\":\"[REDACTED]\""))
    }

    @Test
    fun `classifyMetaBotReply accurately categorizes bot prompt types`() {
        assertEquals(
            MetaBotReplyType.FLOW_PROMPT,
            RelaySessionManager.classifyMetaBotReply("Please specify a login flow, e.g. login android.\n* instagram - Login using cookies")
        )
        assertEquals(
            MetaBotReplyType.COOKIE_PROMPT,
            RelaySessionManager.classifyMetaBotReply("Enter a JSON object with your cookies, or a cURL command copied from browser devtools.")
        )
        assertEquals(
            MetaBotReplyType.SUCCESS,
            RelaySessionManager.classifyMetaBotReply("Logged in as john_doe")
        )
        assertEquals(
            MetaBotReplyType.SUCCESS,
            RelaySessionManager.classifyMetaBotReply("Connected")
        )
        assertEquals(
            MetaBotReplyType.ERROR,
            RelaySessionManager.classifyMetaBotReply("Error: checkpoint_required (challenge_required)")
        )
        assertEquals(
            MetaBotReplyType.OTHER,
            RelaySessionManager.classifyMetaBotReply("Waiting for input...")
        )
    }

    @Test
    fun `RelayNotificationListener maps all 6 target packages and rejects unknown and own package`() {
        assertEquals(MessengerType.WHATSAPP, RelayNotificationListener.mapPackageToMessengerType("com.whatsapp"))
        assertEquals(MessengerType.WHATSAPP, RelayNotificationListener.mapPackageToMessengerType("com.whatsapp.w4b"))
        assertEquals(MessengerType.TELEGRAM, RelayNotificationListener.mapPackageToMessengerType("org.telegram.messenger"))
        assertEquals(MessengerType.TELEGRAM, RelayNotificationListener.mapPackageToMessengerType("org.telegram.messenger.web"))
        assertEquals(MessengerType.INSTAGRAM, RelayNotificationListener.mapPackageToMessengerType("com.instagram.android"))
        assertEquals(MessengerType.FACEBOOK, RelayNotificationListener.mapPackageToMessengerType("com.facebook.orca"))

        assertNull(RelayNotificationListener.mapPackageToMessengerType("com.relay.app"))
        assertNull(RelayNotificationListener.mapPackageToMessengerType("com.other.app"))
    }

    @Test
    fun `RelayNotificationListener shouldIgnoreNotification filters ongoing, summaries and blanks`() {
        assertTrue(RelayNotificationListener.shouldIgnoreNotification(isOngoing = true, isGroupSummary = false, title = "Alice", text = "Hi"))
        assertTrue(RelayNotificationListener.shouldIgnoreNotification(isOngoing = false, isGroupSummary = true, title = "Alice", text = "Hi"))
        assertTrue(RelayNotificationListener.shouldIgnoreNotification(isOngoing = false, isGroupSummary = false, title = "", text = ""))
        assertTrue(RelayNotificationListener.shouldIgnoreNotification(isOngoing = false, isGroupSummary = false, title = null, text = null))

        assertFalse(RelayNotificationListener.shouldIgnoreNotification(isOngoing = false, isGroupSummary = false, title = "Alice", text = "Hi"))
        assertFalse(RelayNotificationListener.shouldIgnoreNotification(isOngoing = false, isGroupSummary = false, title = "Alice", text = ""))
    }

    @Test
    fun `RelayNotificationListener deduplicates against Matrix messages within 2 minute window`() {
        val now = 1761217000000L
        val matrixMessages = listOf(
            MessageItem(
                id = "!wa_chat",
                sender = "Alice",
                snippet = "Hello there!",
                timestamp = "10:00",
                serviceType = MessengerType.WHATSAPP,
                timestampMillis = now
            )
        )

        // Matching sender, snippet, service, within 30 seconds -> true (deduplicated)
        val recentDuplicate = MessageItem(
            id = "!nl_msg_1",
            sender = " Alice ",
            snippet = "hello there! ",
            timestamp = "10:00",
            serviceType = MessengerType.WHATSAPP,
            timestampMillis = now + 30_000L
        )
        assertTrue(RelayNotificationListener.shouldDeduplicateAgainstMatrix(recentDuplicate, matrixMessages))

        // Different snippet -> false
        val differentSnippet = recentDuplicate.copy(snippet = "Different message")
        assertFalse(RelayNotificationListener.shouldDeduplicateAgainstMatrix(differentSnippet, matrixMessages))

        // Different sender -> false
        val differentSender = recentDuplicate.copy(sender = "Bob")
        assertFalse(RelayNotificationListener.shouldDeduplicateAgainstMatrix(differentSender, matrixMessages))

        // More than 2 minutes apart (e.g. 150 seconds) -> false
        val olderMessage = recentDuplicate.copy(timestampMillis = now + 150_000L)
        assertFalse(RelayNotificationListener.shouldDeduplicateAgainstMatrix(olderMessage, matrixMessages))
    }
}
