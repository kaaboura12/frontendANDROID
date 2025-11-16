package com.example.dam_android.models

import com.google.gson.annotations.SerializedName

/**
 * WebRTC signaling message types
 */
enum class SignalType {
    @SerializedName("offer")
    OFFER,

    @SerializedName("answer")
    ANSWER,

    @SerializedName("ice-candidate")
    ICE_CANDIDATE,

    @SerializedName("call-request")
    CALL_REQUEST,

    @SerializedName("call-accepted")
    CALL_ACCEPTED,

    @SerializedName("call-rejected")
    CALL_REJECTED,

    @SerializedName("call-ended")
    CALL_ENDED
}

/**
 * WebRTC signaling message
 */
data class SignalingMessage(
    @SerializedName("type")
    val type: SignalType,

    @SerializedName("roomId")
    val roomId: String,

    @SerializedName("senderId")
    val senderId: String,

    @SerializedName("targetId")
    val targetId: String? = null,

    @SerializedName("data")
    val data: String? = null
)

/**
 * Call state enumeration
 */
enum class CallState {
    IDLE,
    CALLING,
    INCOMING,
    CONNECTING,
    CONNECTED,
    ENDED,
    REJECTED,
    ERROR
}

/**
 * WebRTC Session Description
 */
data class SessionDescription(
    @SerializedName("type")
    val type: String, // "offer" or "answer"

    @SerializedName("sdp")
    val sdp: String
)

/**
 * FIXED WebRTC ICE Candidate (correct order)
 */
data class IceCandidate(
    @SerializedName("candidate")
    val candidate: String,

    @SerializedName("sdpMid")
    val sdpMid: String?,

    @SerializedName("sdpMLineIndex")
    val sdpMLineIndex: Int
)
