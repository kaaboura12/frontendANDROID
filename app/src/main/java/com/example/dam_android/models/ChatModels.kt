package com.example.dam_android.models

import com.example.dam_android.network.api.dto.AudioMessageResponse
import com.example.dam_android.network.api.dto.ChildSummaryResponse
import com.example.dam_android.network.api.dto.LastMessageResponse
import com.example.dam_android.network.api.dto.MessageResponse
import com.example.dam_android.network.api.dto.ParentRoomResponse
import com.example.dam_android.network.api.dto.RoomDetailResponse

data class ParentChatRoom(
    val roomId: String,
    val childId: String?,
    val childName: String,
    val lastMessagePreview: String,
    val lastMessageTimestamp: String?,
    val unreadCount: Int,
    val avatarUrl: String?
)

fun ParentRoomResponse.toDomain(): ParentChatRoom {
    val derivedChildName = buildString {
        val first = child?.firstName?.takeIf { it.isNotBlank() }
        val last = child?.lastName?.takeIf { it.isNotBlank() }
        if (!first.isNullOrBlank()) append(first)
        if (!last.isNullOrBlank()) {
            if (isNotEmpty()) append(" ")
            append(last)
        }
    }.ifBlank { childName ?: "Mon enfant" }

    val preview = lastMessage.pickPreviewText().ifBlank { "Pas encore de message" }

    val timestamp = lastMessage?.createdAt
        ?: lastMessage?.updatedAt
        ?: lastMessage?.sentAt
        ?: lastMessageTimestamp
        ?: lastMessageAt

    return ParentChatRoom(
        roomId = id?.takeIf { it.isNotBlank() }
            ?: roomId?.takeIf { it.isNotBlank() }
            ?: childId?.takeIf { it.isNotBlank() }
            ?: child?.id?.takeIf { it.isNotBlank() }
            ?: "",
        childId = childId ?: child?.id,
        childName = derivedChildName,
        lastMessagePreview = preview,
        lastMessageTimestamp = timestamp,
        unreadCount = unreadCount ?: 0,
        avatarUrl = child?.avatarUrl
    )
}

private fun LastMessageResponse?.pickPreviewText(): String {
    if (this == null) return ""
    return when {
        !content.isNullOrBlank() -> content
        !text.isNullOrBlank() -> text
        audio != null -> "Message vocal"
        else -> ""
    }
}

data class ChatRoomDetail(
    val id: String,
    val childName: String,
    val childAvatar: String?,
    val participants: List<String>,
    val lastMessage: ChatMessage?
)

data class ChatMessage(
    val id: String,
    val roomId: String,
    val text: String?,
    val audio: AudioMessage?,
    val senderModel: String?,
    val senderId: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class AudioMessage(
    val url: String?,
    val durationSec: Double?,
    val mimeType: String?,
    val sizeBytes: Long?
)

fun RoomDetailResponse.toDomain(): ChatRoomDetail {
    val derivedChildName = child?.let {
        listOfNotNull(it.firstName, it.lastName)
            .filter { part -> !part.isNullOrBlank() }
            .joinToString(" ")
    }.orEmpty().ifBlank { "Mon enfant" }

    return ChatRoomDetail(
        id = id,
        childName = derivedChildName,
        childAvatar = child?.avatarUrl,
        participants = participants ?: emptyList(),
        lastMessage = lastMessage?.toChatMessage(id)
    )
}

fun MessageResponse.toDomain(): ChatMessage {
    return ChatMessage(
        id = id,
        roomId = roomId ?: "",
        text = text,
        audio = audio?.toDomain(),
        senderModel = senderModel,
        senderId = senderId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LastMessageResponse.toChatMessage(roomId: String?): ChatMessage {
    return ChatMessage(
        id = id ?: "",
        roomId = roomId ?: "",
        text = text ?: content,
        audio = audio?.toDomain(),
        senderModel = senderRole ?: senderModel,
        senderId = sender ?: senderId,
        createdAt = createdAt ?: sentAt,
        updatedAt = updatedAt
    )
}

fun AudioMessageResponse.toDomain(): AudioMessage? {
    val normalizedUrl = url ?: return null
    return AudioMessage(
        url = normalizedUrl,
        durationSec = durationSec,
        mimeType = mimeType,
        sizeBytes = sizeBytes
    )
}
