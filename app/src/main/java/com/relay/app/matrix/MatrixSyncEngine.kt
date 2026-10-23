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
}
