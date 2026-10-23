package com.relay.app.matrix

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

data class MatrixSessionCredentials(
    val homeserverUrl: String = MatrixAuthRepository.DEFAULT_HOMESERVER_URL,
    val accessToken: String? = MatrixAuthRepository.DEFAULT_ACCESS_TOKEN,
    val userId: String? = MatrixAuthRepository.DEFAULT_USER_ID,
    val sinceToken: String? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
}

class MatrixAuthRepository(
    context: Context?,
    private val httpClient: HttpClient
) {
    companion object {
        const val DEFAULT_HOMESERVER_URL = "https://max-os.mooo.com:8448"
        const val DEFAULT_USER_ID = "@admin:max-os.mooo.com"
        const val DEFAULT_ACCESS_TOKEN = "rfs7toyq6UeMCmT7W7layNQsWPuyznTG"

        private const val PREFS_FILE = "relay_matrix_secure_prefs"
        private const val KEY_HOMESERVER_URL = "homeserver_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SINCE_TOKEN = "since_token"
        private const val KEY_CACHED_SYNC_JSON = "cached_sync_json"
        private const val KEY_BOT_ROOM_PREFIX = "bot_room_"

        /**
         * Initial cached Matrix `/_matrix/client/v3/sync` payload representing bridged rooms
         * from Mautrix-WhatsApp, Mautrix-Telegram, Instagram, Facebook, Google Messages, and Viber
         * on `max-os.mooo.com`. Used on cold start / offline cache before the first live `/sync` completes.
         */
        val INITIAL_CACHED_SYNC_JSON = """
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
                  },
                  "!ig_studio:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 3, "highlight_count": 0 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "@studio.north" } },
                        { "type": "m.room.bridged", "content": { "bridge": "instagram", "account_id": "ig_studio" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_ig_1",
                          "type": "m.room.message",
                          "sender": "@instagram_studionorth:max-os.mooo.com",
                          "origin_server_ts": 1761215460000,
                          "content": { "msgtype": "m.text", "body": "Sent a reel · \"check lighting\"" }
                        }
                      ]
                    }
                  },
                  "!wa_work_ops:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 6, "highlight_count": 1 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "Work · Ops" } },
                        { "type": "m.room.bridged", "content": { "bridge": "whatsapp", "account_id": "wa_work" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_wa_2",
                          "type": "m.room.message",
                          "sender": "@whatsapp_ops:max-os.mooo.com",
                          "origin_server_ts": 1761213120000,
                          "content": { "msgtype": "m.text", "body": "Deploy window moved to 22:00" }
                        }
                      ]
                    }
                  },
                  "!fb_mom:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 2, "highlight_count": 0 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "Mom" } },
                        { "type": "m.room.bridged", "content": { "bridge": "facebook", "account_id": "fb_main" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_fb_1",
                          "type": "m.room.message",
                          "sender": "@facebook_mom:max-os.mooo.com",
                          "origin_server_ts": 1761210600000,
                          "content": { "msgtype": "m.text", "body": "Call me when you get home" }
                        }
                      ]
                    }
                  },
                  "!gm_dhl:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 4, "highlight_count": 0 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "DHL" } },
                        { "type": "m.room.bridged", "content": { "bridge": "gmessages", "account_id": "gm_main" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_gm_1",
                          "type": "m.room.message",
                          "sender": "@gmessages_dhl:max-os.mooo.com",
                          "origin_server_ts": 1761206940000,
                          "content": { "msgtype": "m.text", "body": "Parcel 4412 arrives today 14-16" }
                        }
                      ]
                    }
                  },
                  "!vb_courier:max-os.mooo.com": {
                    "unread_notifications": { "notification_count": 1, "highlight_count": 0 },
                    "state": {
                      "events": [
                        { "type": "m.room.name", "content": { "name": "Viber Courier" } },
                        { "type": "m.room.bridged", "content": { "bridge": "viber", "account_id": "vb_main" } }
                      ]
                    },
                    "timeline": {
                      "events": [
                        {
                          "event_id": "${'$'}ev_vb_1",
                          "type": "m.room.message",
                          "sender": "@viber_courier:max-os.mooo.com",
                          "origin_server_ts": 1761204000000,
                          "content": { "msgtype": "m.text", "body": "Package left at locker #9" }
                        }
                      ]
                    }
                  }
                }
              }
            }
        """.trimIndent()
    }

    private val prefs: SharedPreferences? = context?.let { ctx ->
        try {
            val masterKey = MasterKey.Builder(ctx)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                ctx,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            ctx.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    private var inMemoryCreds = MatrixSessionCredentials()
    private var inMemoryCachedSync: String = INITIAL_CACHED_SYNC_JSON
    private val inMemoryBotRooms = mutableMapOf<String, String>()

    fun getCredentials(): MatrixSessionCredentials {
        val p = prefs ?: return inMemoryCreds
        return MatrixSessionCredentials(
            homeserverUrl = p.getString(KEY_HOMESERVER_URL, DEFAULT_HOMESERVER_URL) ?: DEFAULT_HOMESERVER_URL,
            accessToken = p.getString(KEY_ACCESS_TOKEN, DEFAULT_ACCESS_TOKEN) ?: DEFAULT_ACCESS_TOKEN,
            userId = p.getString(KEY_USER_ID, DEFAULT_USER_ID) ?: DEFAULT_USER_ID,
            sinceToken = p.getString(KEY_SINCE_TOKEN, null)
        )
    }

    fun saveSinceToken(sinceToken: String) {
        inMemoryCreds = inMemoryCreds.copy(sinceToken = sinceToken)
        prefs?.edit()?.putString(KEY_SINCE_TOKEN, sinceToken)?.apply()
    }

    fun saveCredentials(homeserverUrl: String, accessToken: String, userId: String) {
        val cleanUrl = homeserverUrl.trimEnd('/')
        inMemoryCreds = MatrixSessionCredentials(
            homeserverUrl = cleanUrl,
            accessToken = accessToken,
            userId = userId,
            sinceToken = null
        )
        prefs?.edit()
            ?.putString(KEY_HOMESERVER_URL, cleanUrl)
            ?.putString(KEY_ACCESS_TOKEN, accessToken)
            ?.putString(KEY_USER_ID, userId)
            ?.remove(KEY_SINCE_TOKEN)
            ?.apply()
    }

    fun saveCachedSyncJson(rawJson: String) {
        inMemoryCachedSync = rawJson
        prefs?.edit()?.putString(KEY_CACHED_SYNC_JSON, rawJson)?.apply()
    }

    fun getCachedSyncJson(): String {
        return prefs?.getString(KEY_CACHED_SYNC_JSON, INITIAL_CACHED_SYNC_JSON) ?: inMemoryCachedSync
    }

    fun getBotRoomId(botMxid: String): String? {
        return prefs?.getString("$KEY_BOT_ROOM_PREFIX$botMxid", null) ?: inMemoryBotRooms[botMxid]
    }

    fun saveBotRoomId(botMxid: String, roomId: String) {
        inMemoryBotRooms[botMxid] = roomId
        prefs?.edit()?.putString("$KEY_BOT_ROOM_PREFIX$botMxid", roomId)?.apply()
    }

    fun clearSession() {
        inMemoryCreds = MatrixSessionCredentials(accessToken = null, userId = null, sinceToken = null)
        prefs?.edit()?.clear()?.apply()
    }

    suspend fun login(
        homeserverUrl: String = DEFAULT_HOMESERVER_URL,
        username: String,
        password: String
    ): Result<MatrixSessionCredentials> = runCatching {
        val cleanUrl = homeserverUrl.trimEnd('/')
        val cleanUsername = username.removePrefix("@").substringBefore(":")
        val response: MatrixLoginResponse = httpClient.post("$cleanUrl/_matrix/client/v3/login") {
            contentType(ContentType.Application.Json)
            setBody(
                MatrixLoginRequest(
                    identifier = MatrixUserIdentifier(user = cleanUsername),
                    password = password
                )
            )
        }.body()
        saveCredentials(cleanUrl, response.accessToken, response.userId)
        getCredentials()
    }

    suspend fun verifyWhoAmI(): Result<MatrixWhoAmIResponse> = runCatching {
        val creds = getCredentials()
        require(creds.isAuthenticated) { "Missing Matrix access token" }
        httpClient.get("${creds.homeserverUrl}/_matrix/client/v3/account/whoami") {
            bearerAuth(creds.accessToken!!)
        }.body()
    }
}
