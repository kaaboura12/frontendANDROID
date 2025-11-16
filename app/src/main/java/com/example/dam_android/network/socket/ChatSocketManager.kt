package com.example.dam_android.network.socket

import android.util.Log
import com.example.dam_android.models.ChatMessage
import com.example.dam_android.network.api.dto.MessageResponse
import com.example.dam_android.network.api.dto.SendTextRequest
import com.example.dam_android.models.toDomain
import com.example.dam_android.network.local.SessionManager
import com.google.gson.Gson
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.IO.Options
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.URISyntaxException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ChatSocketManager {

    private const val TAG = "ChatSocket"
    private const val SOCKET_URL = "http://10.0.2.2:3005"

    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    private var sessionManager: SessionManager? = null
    private var socket: Socket? = null
    @Volatile
    private var isConnecting: Boolean = false
    @Volatile
    private var listenersBound: Boolean = false
    @Volatile
    private var lastAuthToken: String? = null

    private val joinedRooms = ConcurrentHashMap.newKeySet<String>()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val incomingMessages: SharedFlow<ChatMessage> = _incomingMessages.asSharedFlow()

    // WebRTC signaling through existing chat socket
    private val _incomingSignals = MutableSharedFlow<org.json.JSONObject>(extraBufferCapacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val incomingSignals: SharedFlow<org.json.JSONObject> = _incomingSignals.asSharedFlow()

    fun initialize(sessionManager: SessionManager) {
        if (this.sessionManager == null) {
            this.sessionManager = sessionManager
        }
    }

    @Synchronized
    fun connect() {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected")
            return
        }
        if (isConnecting) {
            Log.d(TAG, "Socket connection already in progress")
            return
        }

        val token = sessionManager?.getAuthToken()
        if (token.isNullOrBlank()) {
            Log.e(TAG, "No auth token available for socket connection")
            return
        }

        // Reuse existing socket if present; only recreate when none exists or token changed
        if (socket == null || lastAuthToken != token) {
            // If token changed, tear down existing socket before recreating
            if (socket != null && lastAuthToken != token) {
                Log.d(TAG, "Auth token changed, recreating socket")
                try {
                    socket?.off() // remove all listeners
                    socket?.disconnect()
                } catch (_: Exception) { }
                socket = null
                listenersBound = false
                _connectionState.value = false
            }

            val opts = Options().apply {
                forceNew = false // reuse connection within same Manager
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                reconnectionAttempts = Int.MAX_VALUE
                timeout = 10000
                transports = arrayOf("websocket", "polling")
                query = "token=$token" // send JWT for auth
            }
            try {
                socket = IO.socket(SOCKET_URL, opts)
                lastAuthToken = token
            } catch (e: URISyntaxException) {
                Log.e(TAG, "Invalid socket URL", e)
                return
            }
        }

        // Bind listeners only once per socket instance
        if (!listenersBound) {
            socket?.on(Socket.EVENT_CONNECT) {
                isConnecting = false
                Log.d(TAG, "✅ Socket connected successfully")
                _connectionState.value = true
                // Rejoin previously joined rooms after reconnect
                joinedRooms.forEach { roomId ->
                    Log.d(TAG, "Rejoining room after reconnect: $roomId")
                    emitJoin(roomId)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) { args ->
                isConnecting = false
                val reason = args.getOrNull(0)?.toString() ?: "unknown"
                Log.w(TAG, "⚠️ Socket disconnected - reason: $reason")
                _connectionState.value = false
            }
            
            socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
                isConnecting = false
                val error = args.getOrNull(0)
                Log.e(TAG, "❌ Socket connection error: $error")
            }
            
            socket?.on("presence") { args ->
                if (args.isNotEmpty()) {
                    Log.d(TAG, "👥 Presence event: ${args[0]}")
                }
            }

            socket?.on("newMessage") { args ->
                Log.d(TAG, "📨 Received newMessage event")
                if (args.isNullOrEmpty()) {
                    Log.w(TAG, "newMessage event has no payload")
                    return@on
                }
                val payload = args[0]
                scope.launch {
                    try {
                        val json = when (payload) {
                            is JSONObject -> payload
                            is String -> JSONObject(payload)
                            else -> {
                                Log.w(TAG, "Unknown payload type: ${payload?.javaClass?.name}")
                                return@launch
                            }
                        }
                        
                        Log.d(TAG, "📨 Message payload: ${json.toString().take(100)}...")
                        
                        // Check if this is a WebRTC signaling message
                        val messageType = json.optString("messageType", "")
                        if (messageType == "webrtc_signal") {
                            Log.d(TAG, "🔊 WebRTC signal received: ${json.optString("signalType")}")
                            _incomingSignals.emit(json)
                        } else {
                            // Regular chat message
                            Log.d(TAG, "💬 Regular chat message received")
                            val messageResponse = gson.fromJson(json.toString(), MessageResponse::class.java)
                            val domain = messageResponse.toDomain()
                            Log.d(TAG, "💬 Emitting message for room: ${domain.roomId}, text: ${domain.text?.take(30)}")
                            _incomingMessages.emit(domain)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to parse incoming message", e)
                        e.printStackTrace()
                    }
                }
            }
            listenersBound = true
        }

        isConnecting = true
        socket?.connect()
    }

    fun disconnectIfIdle() {
        if (joinedRooms.isEmpty()) {
            socket?.disconnect()
            socket = null
            listenersBound = false
            isConnecting = false
            _connectionState.value = false
        }
    }

    fun joinRoom(roomId: String) {
        connect()
        if (roomId.isBlank()) return
        if (!joinedRooms.contains(roomId)) {
            joinedRooms.add(roomId)
        }
        emitJoin(roomId)
    }

    private fun emitJoin(roomId: String) {
        // Backend expects just roomId, but joins with "room:" prefix internally
        val payload = JSONObject(mapOf("roomId" to roomId))
        socket?.emit("joinRoom", payload, Ack { args ->
            if (args.isNotEmpty()) {
                val response = args[0]
                Log.d(TAG, "✅ Join room response: $response")
            } else {
                Log.d(TAG, "✅ Joined room: $roomId (no response)")
            }
        })
        Log.d(TAG, "Emitted joinRoom for: $roomId")
    }

    fun leaveRoom(roomId: String) {
        if (roomId.isBlank()) return
        val payload = JSONObject(mapOf("roomId" to roomId))
        socket?.emit("leaveRoom", payload)
        joinedRooms.remove(roomId)
        if (joinedRooms.isEmpty()) {
            disconnectIfIdle()
        }
    }

    suspend fun sendTextMessage(roomId: String, text: String, senderModel: String, senderId: String): Result<Unit> {
        // Ensure connected (will wait briefly for connection)
        if (!waitUntilConnected()) {
            return Result.failure(IllegalStateException("Socket not connected"))
        }
        // Ensure room joined before sending to receive broadcast immediately
        if (!joinedRooms.contains(roomId)) {
            joinedRooms.add(roomId)
            emitJoin(roomId)
        }
        val request = SendTextRequest(text = text, senderModel = senderModel, senderId = senderId)
        val json = JSONObject().apply {
            put("roomId", roomId)
            put("text", request.text)
            put("senderModel", request.senderModel)
            put("senderId", request.senderId)
        }
        return suspendCancellableCoroutine { continuation ->
            socket?.emit("sendText", json, Ack { arguments ->
                if (arguments.isEmpty()) {
                    continuation.resume(Result.failure(IllegalStateException("No response")))
                    return@Ack
                }
                val response = arguments[0]
                when (response) {
                    is JSONObject -> {
                        if (response.has("error")) {
                            continuation.resume(Result.failure(IllegalStateException(response.optString("error"))))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }

                    is String -> {
                        if (response.contains("error")) {
                            continuation.resume(Result.failure(IllegalStateException(response)))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }

                    else -> continuation.resume(Result.success(Unit))
                }
            })

            continuation.invokeOnCancellation {
                // No special cancellation handling
            }
        }
    }

    /**
     * Send WebRTC signaling message through existing chat infrastructure
     * No backend changes needed - uses existing message system
     */
    fun sendWebRTCSignal(roomId: String, signalType: String, senderId: String, targetId: String? = null, data: String? = null) {
        // Ensure connection
        if (socket?.connected() != true) {
            Log.w(TAG, "Socket disconnected, reconnecting...")
            connect()
        }

        val json = JSONObject().apply {
            put("roomId", roomId)
            put("messageType", "webrtc_signal")
            put("signalType", signalType)
            put("senderId", senderId)
            if (targetId != null) put("targetId", targetId)
            if (data != null) put("data", data)
            put("senderModel", "User") // Required field for backend
            put("text", "") // Backend expects text field (can be empty for signals)
        }

        // Try to send even if not connected yet - socket.io will queue it
        socket?.emit("sendText", json)
        Log.d(TAG, "✅ Sent WebRTC signal: $signalType to room $roomId (socket connected: ${socket?.connected()})")
    }

    /**
     * Forcefully tear down the socket and listeners.
     * Use when logging out or explicitly wanting to drop the connection.
     */
    @Synchronized
    fun release() {
        try {
            socket?.off()
            socket?.disconnect()
        } catch (_: Exception) { }
        socket = null
        listenersBound = false
        isConnecting = false
        _connectionState.value = false
        joinedRooms.clear()
        lastAuthToken = null
    }

    /**
     * Wait up to a few seconds for the socket to become connected.
     * Returns true if connected, false otherwise.
     */
    private suspend fun waitUntilConnected(timeoutMs: Long = 8000L): Boolean {
        if (socket?.connected() == true) return true
        connect()
        return try {
            withTimeout(timeoutMs) {
                // Fast path: if already true, returns immediately
                if (connectionState.value) return@withTimeout true
                connectionState.first { it }
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}
