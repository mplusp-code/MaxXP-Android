package com.relay.app.matrix

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import kotlin.math.min

class MatrixSyncEngine(
    private val authRepository: MatrixAuthRepository,
    private val roomParser: MatrixRoomParser,
    private val httpClient: HttpClient = createDefaultHttpClient(),
    private val onSyncUpdate: (ParsedSyncBatch) -> Unit
) {
    companion object {
        val sharedJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }

        fun buildPusherSetRequest(
            pushkey: String,
            gatewayUrl: String = MatrixAuthRepository.DEFAULT_PUSH_GATEWAY_URL,
            enable: Boolean = true
        ): MatrixPusherSetRequest = MatrixPusherSetRequest(
            pushkey = pushkey,
            kind = if (enable) "http" else null,
            data = MatrixPusherData(url = gatewayUrl, format = "event_id_only")
        )

        fun createDefaultHttpClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(sharedJson)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 40_000L
                connectTimeoutMillis = 8_000L
                socketTimeoutMillis = 40_000L
            }
        }
    }

    private val _syncStatus = MutableStateFlow(MatrixSyncStatus.CONNECTING)
    val syncStatus: StateFlow<MatrixSyncStatus> = _syncStatus.asStateFlow()

    private var syncJob: Job? = null
    private var pusherRegistered = false

    /**
     * Parses and delivers the locally cached `/sync` JSON snapshot immediately on startup
     * so `TodayScreen` and `InboxScreen` display real room items before network I/O completes.
     */
    fun loadCachedSnapshot() {
        runCatching {
            val rawCached = authRepository.getCachedSyncJson()
            val creds = authRepository.getCredentials()
            val batch = roomParser.parseRawSyncJson(rawCached, creds.userId)
            onSyncUpdate(batch)
        }
    }

    fun startSync(scope: CoroutineScope) {
        if (syncJob?.isActive == true) return
        loadCachedSnapshot()

        syncJob = scope.launch {
            var backoffMs = 1_500L
            while (isActive) {
                val creds = authRepository.getCredentials()
                if (!creds.isAuthenticated) {
                    _syncStatus.value = MatrixSyncStatus.OFFLINE
                    delay(5_000L)
                    continue
                }

                if (_syncStatus.value != MatrixSyncStatus.SYNCED) {
                    _syncStatus.value = MatrixSyncStatus.CONNECTING
                }

                try {
                    val httpResponse = httpClient.get("${creds.homeserverUrl}/_matrix/client/v3/sync") {
                        bearerAuth(creds.accessToken!!)
                        parameter("timeout", 30_000)
                        creds.sinceToken?.let { parameter("since", it) }
                    }

                    if (!httpResponse.status.isSuccess()) {
                        _syncStatus.value = MatrixSyncStatus.OFFLINE
                        delay(backoffMs)
                        backoffMs = min(backoffMs * 2, 30_000L)
                        continue
                    }

                    val rawBody = httpResponse.bodyAsText()
                    val response = sharedJson.decodeFromString<MatrixSyncResponse>(rawBody)

                    if (creds.sinceToken == null && !response.rooms?.join.isNullOrEmpty()) {
                        authRepository.saveCachedSyncJson(rawBody)
                    }
                    authRepository.saveSinceToken(response.nextBatch)
                    _syncStatus.value = MatrixSyncStatus.SYNCED
                    backoffMs = 1_500L

                    if (!pusherRegistered) {
                        val savedEndpoint = authRepository.getPushEndpoint()
                        if (!savedEndpoint.isNullOrBlank()) {
                            pusherRegistered = true
                            runCatching {
                                registerPusher(
                                    pushkey = savedEndpoint,
                                    gatewayUrl = MatrixAuthRepository.DEFAULT_PUSH_GATEWAY_URL
                                )
                            }
                        }
                    }

                    val parsedBatch = roomParser.parseSyncResponse(
                        syncResponse = response,
                        selfUserId = creds.userId
                    )
                    if (parsedBatch.messages.isNotEmpty() || parsedBatch.bridgeBotUpdates.isNotEmpty()) {
                        onSyncUpdate(parsedBatch)
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Throwable) {
                    _syncStatus.value = MatrixSyncStatus.OFFLINE
                    delay(backoffMs)
                    backoffMs = min(backoffMs * 2, 30_000L)
                }
            }
        }
    }

    fun restartSync(scope: CoroutineScope) {
        stopSync()
        startSync(scope)
    }

    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        _syncStatus.value = MatrixSyncStatus.OFFLINE
    }

    /**
     * Dispatches an `m.room.message` (`m.text`) event directly to the Matrix homeserver
     * without waking any hibernated WebView.
     */
    suspend fun sendTextMessage(
        roomId: String,
        body: String,
        txnId: String = "relay_${System.currentTimeMillis()}"
    ): Result<String> = runCatching {
        val creds = authRepository.getCredentials()
        require(creds.isAuthenticated) { "Not authenticated with Matrix homeserver" }
        val encodedRoomId = URLEncoder.encode(roomId, "UTF-8")
        val response: MatrixSendEventResponse = httpClient.put(
            "${creds.homeserverUrl}/_matrix/client/v3/rooms/$encodedRoomId/send/m.room.message/$txnId"
        ) {
            bearerAuth(creds.accessToken!!)
            contentType(ContentType.Application.Json)
            setBody(MatrixSendMessageRequest(msgtype = "m.text", body = body))
        }.body()
        response.eventId
    }

    /**
     * Finds or creates a 1-on-1 management DM room with a Mautrix bridge bot
     * (e.g., `@whatsappbot:max-os.mooo.com` or `@telegrambot:max-os.mooo.com`)
     * and sends a bridge command (such as `"login qr"` or `"login"`).
     */
    suspend fun sendBridgeBotCommand(
        botMxid: String,
        command: String = "login"
    ): Result<String> = runCatching {
        val creds = authRepository.getCredentials()
        require(creds.isAuthenticated) { "Not authenticated with Matrix homeserver" }

        val existingRoomId = authRepository.getBotRoomId(botMxid)
        val roomId = if (!existingRoomId.isNullOrBlank()) {
            existingRoomId
        } else {
            val created: MatrixCreateRoomResponse = httpClient.post(
                "${creds.homeserverUrl}/_matrix/client/v3/createRoom"
            ) {
                bearerAuth(creds.accessToken!!)
                contentType(ContentType.Application.Json)
                setBody(MatrixCreateRoomRequest(invite = listOf(botMxid)))
            }.body()
            authRepository.saveBotRoomId(botMxid, created.roomId)
            created.roomId
        }

        sendTextMessage(roomId = roomId, body = command).getOrThrow()
    }

    /**
     * Registers an HTTP Matrix Pusher (`POST /_matrix/client/v3/pushers/set`) on Conduit
     * pointing to the self-hosted `ntfy` UnifiedPush gateway (`https://max-os.mooo.com:8449/_matrix/push/v1/notify`).
     */
    suspend fun registerPusher(
        pushkey: String,
        gatewayUrl: String = MatrixAuthRepository.DEFAULT_PUSH_GATEWAY_URL,
        enable: Boolean = true
    ): Result<Unit> = runCatching {
        require(pushkey.isNotBlank()) { "Push endpoint (pushkey) cannot be blank" }
        val creds = authRepository.getCredentials()
        require(creds.isAuthenticated) { "Not authenticated with Matrix homeserver" }
        val request = buildPusherSetRequest(
            pushkey = pushkey,
            gatewayUrl = gatewayUrl,
            enable = enable
        )
        val response = httpClient.post("${creds.homeserverUrl}/_matrix/client/v3/pushers/set") {
            bearerAuth(creds.accessToken!!)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        require(response.status.isSuccess()) { "Failed to register Matrix pusher: HTTP ${response.status.value}" }
    }

    /**
     * Fetches a single room event (`GET /_matrix/client/v3/rooms/{roomId}/event/{eventId}`)
     * for `event_id_only` Matrix push notifications.
     */
    suspend fun fetchEventDetails(roomId: String, eventId: String): MatrixEvent? = runCatching {
        val creds = authRepository.getCredentials()
        require(creds.isAuthenticated) { "Not authenticated with Matrix homeserver" }
        val encRoom = URLEncoder.encode(roomId, "UTF-8")
        val encEvent = URLEncoder.encode(eventId, "UTF-8")
        val response = httpClient.get("${creds.homeserverUrl}/_matrix/client/v3/rooms/$encRoom/event/$encEvent") {
            bearerAuth(creds.accessToken!!)
        }
        if (!response.status.isSuccess()) return@runCatching null
        sharedJson.decodeFromString<MatrixEvent>(response.bodyAsText())
    }.getOrNull()

    /**
     * Fetches the room's `m.room.name` state event if not already present in the cached sync snapshot.
     */
    suspend fun fetchRoomName(roomId: String): String? = runCatching {
        val creds = authRepository.getCredentials()
        require(creds.isAuthenticated) { "Not authenticated with Matrix homeserver" }
        val encRoom = URLEncoder.encode(roomId, "UTF-8")
        val response = httpClient.get("${creds.homeserverUrl}/_matrix/client/v3/rooms/$encRoom/state/m.room.name") {
            bearerAuth(creds.accessToken!!)
        }
        if (!response.status.isSuccess()) return@runCatching null
        val obj = sharedJson.parseToJsonElement(response.bodyAsText()).jsonObject
        obj["name"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
