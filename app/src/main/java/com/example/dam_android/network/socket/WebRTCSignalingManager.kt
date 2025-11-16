package com.example.dam_android.network.socket

import android.util.Log
import com.example.dam_android.models.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

object WebRTCSignalingManager {

    private const val TAG = "WebRTCSignaling"
    private val gson = Gson()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentUserId: String? = null

    private val _incomingSignals = MutableSharedFlow<SignalingMessage>(
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingSignals: SharedFlow<SignalingMessage> = _incomingSignals.asSharedFlow()

    private val _callStateUpdates = MutableSharedFlow<Pair<String, CallState>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val callStateUpdates: SharedFlow<Pair<String, CallState>> = _callStateUpdates.asSharedFlow()

    fun initialize(userId: String) {
        if (currentUserId == null) {
            currentUserId = userId
            listenToIncomingSignals()
        }
    }

    private fun listenToIncomingSignals() {
        scope.launch {
            ChatSocketManager.incomingSignals.collect { json ->
                parseIncomingSignal(json)
            }
        }
    }

    private fun parseIncomingSignal(json: JSONObject) {
        try {
            val signalTypeStr = json.optString("signalType")
            val targetId = json.optString("targetId")

            if (!targetId.isNullOrEmpty() && targetId != currentUserId) return

            val type = when (signalTypeStr) {
                "call-request" -> SignalType.CALL_REQUEST
                "call-accepted" -> SignalType.CALL_ACCEPTED
                "call-rejected" -> SignalType.CALL_REJECTED
                "call-ended" -> SignalType.CALL_ENDED
                "offer" -> SignalType.OFFER
                "answer" -> SignalType.ANSWER
                "ice-candidate" -> SignalType.ICE_CANDIDATE
                else -> return
            }

            val message = SignalingMessage(
                type = type,
                roomId = json.optString("roomId"),
                senderId = json.optString("senderId"),
                targetId = targetId.ifEmpty { null },
                data = json.optString("data").ifEmpty { null }
            )

            scope.launch {
                _incomingSignals.emit(message)
                updateCallState(message)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming signal", e)
        }
    }

    private suspend fun updateCallState(msg: SignalingMessage) {
        when (msg.type) {
            SignalType.CALL_REQUEST -> _callStateUpdates.emit(msg.roomId to CallState.INCOMING)
            SignalType.CALL_ACCEPTED -> _callStateUpdates.emit(msg.roomId to CallState.CONNECTING)
            SignalType.CALL_REJECTED -> _callStateUpdates.emit(msg.roomId to CallState.REJECTED)
            SignalType.CALL_ENDED -> _callStateUpdates.emit(msg.roomId to CallState.ENDED)
            else -> {}
        }
    }

    // Room handled by ChatSocketManager
    fun joinRoom(roomId: String) = Log.d(TAG, "Using existing room: $roomId")
    fun leaveRoom(roomId: String) = Log.d(TAG, "Leaving room: $roomId")

    // ------------------------------------------------------------
    // SENDING SIGNALS (clean & unified)
    // ------------------------------------------------------------

    private fun sendSignal(
        roomId: String,
        targetId: String?,
        type: String,
        data: Any? = null
    ) {
        val userId = currentUserId ?: return
        val dataJson = data?.let { gson.toJson(it) }

        ChatSocketManager.sendWebRTCSignal(
            roomId = roomId,
            signalType = type,
            senderId = userId,
            targetId = targetId,
            data = dataJson
        )
        Log.d(TAG, "Sent signal type=$type to target=${targetId ?: "all"} room=$roomId")
    }

    fun sendCallRequest(roomId: String, targetId: String) =
        sendSignal(roomId, targetId, "call-request")

    fun acceptCall(roomId: String, targetId: String) =
        sendSignal(roomId, targetId, "call-accepted")

    fun rejectCall(roomId: String, targetId: String) =
        sendSignal(roomId, targetId, "call-rejected")

    fun endCall(roomId: String, targetId: String? = null) =
        sendSignal(roomId, targetId, "call-ended")

    fun sendOffer(roomId: String, targetId: String, sdp: String) =
        sendSignal(roomId, targetId, "offer", com.example.dam_android.models.SessionDescription("offer", sdp))

    fun sendAnswer(roomId: String, targetId: String, sdp: String) =
        sendSignal(roomId, targetId, "answer", com.example.dam_android.models.SessionDescription("answer", sdp))

    // -----------------------------------------
    // ICE CANDIDATE = ALWAYS SEND AS MODEL TYPES
    // -----------------------------------------

    fun sendIceCandidate(
        roomId: String,
        targetId: String,
        candidate: String,
        sdpMid: String?,
        sdpMLineIndex: Int
    ) {
        val ice = com.example.dam_android.models.IceCandidate(
            candidate = candidate,
            sdpMid = sdpMid ?: "",
            sdpMLineIndex = sdpMLineIndex
        )

        sendSignal(roomId, targetId, "ice-candidate", ice)
    }
}
