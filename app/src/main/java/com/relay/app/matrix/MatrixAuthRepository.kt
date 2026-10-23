package com.relay.app.matrix

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.relay.app.data.AccountItem
import com.relay.app.data.MessengerType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

data class MatrixSessionCredentials(
    val homeserverUrl: String = MatrixAuthRepository.DEFAULT_HOMESERVER_URL,
    val accessToken: String? = MatrixAuthRepository.DEFAULT_ACCESS_TOKEN,
    val userId: String? = MatrixAuthRepository.DEFAULT_USER_ID,
    val sinceToken: String? = null
) {
    val isAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank() && !userId.isNullOrBlank()
}

@Serializable
data class PersistedAccountDto(
    val id: String,
    val accountName: String,
    val serviceTypeName: String,
    val isNotifyEnabled: Boolean = true
)

class MatrixAuthRepository(
    context: Context?,
    private val httpClient: HttpClient
) {
    companion object {
        const val DEFAULT_HOMESERVER_URL = "https://max-os.mooo.com:8448"
        const val DEFAULT_USER_ID = "@admin:max-os.mooo.com"
        const val DEFAULT_ACCESS_TOKEN = "rfs7toyq6UeMCmT7W7layNQsWPuyznTG"

        private const val PREFS_FILE = "relay_matrix_secure_prefs_v2"
        private const val KEY_HOMESERVER_URL = "homeserver_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SINCE_TOKEN = "since_token"
        private const val KEY_CACHED_SYNC_JSON = "cached_sync_json_v2"
        private const val KEY_SAVED_ACCOUNTS_JSON = "saved_accounts_json_v2"
        private const val KEY_BOT_ROOM_PREFIX = "bot_room_"

        /**
         * Empty initial Matrix `/sync` snapshot (no fake/demo rooms).
         */
        const val INITIAL_CACHED_SYNC_JSON = """{"next_batch":"","rooms":{"join":{}}}"""

        fun defaultBotForService(serviceType: MessengerType): String = when (serviceType) {
            MessengerType.WHATSAPP -> "@whatsappbot:max-os.mooo.com"
            MessengerType.TELEGRAM -> "@telegrambot:max-os.mooo.com"
            MessengerType.INSTAGRAM -> "@instagrambot:max-os.mooo.com"
            MessengerType.FACEBOOK -> "@facebookbot:max-os.mooo.com"
            MessengerType.GOOGLE_MESSAGES -> "@gmessagesbot:max-os.mooo.com"
            MessengerType.VIBER -> "@viberbot:max-os.mooo.com"
        }
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

    fun loadSavedAccounts(): List<AccountItem> {
        val raw = prefs?.getString(KEY_SAVED_ACCOUNTS_JSON, null) ?: return emptyList()
        return runCatching {
            val dtos = MatrixSyncEngine.sharedJson.decodeFromString<List<PersistedAccountDto>>(raw)
            dtos.mapNotNull { dto ->
                val svc = MessengerType.entries.find { it.name == dto.serviceTypeName } ?: return@mapNotNull null
                AccountItem(
                    id = dto.id,
                    accountName = dto.accountName,
                    serviceType = svc,
                    statusText = "Tap Open or Relink to sign in",
                    isConnected = false,
                    isNeedsAttention = true,
                    isNotifyEnabled = dto.isNotifyEnabled,
                    unreadCount = 0,
                    botMxid = defaultBotForService(svc)
                )
            }
        }.getOrDefault(emptyList())
    }

    fun saveAccounts(accounts: List<AccountItem>) {
        runCatching {
            val dtos = accounts.map {
                PersistedAccountDto(
                    id = it.id,
                    accountName = it.accountName,
                    serviceTypeName = it.serviceType.name,
                    isNotifyEnabled = it.isNotifyEnabled
                )
            }
            val raw = MatrixSyncEngine.sharedJson.encodeToString(dtos)
            prefs?.edit()?.putString(KEY_SAVED_ACCOUNTS_JSON, raw)?.apply()
        }
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
