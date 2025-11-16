package com.example.dam_android.network.api.dto

import com.google.gson.annotations.SerializedName

/**
 * Request/Response DTOs pour l'API WeldiWin Backend
 * Correspondent exactement à la structure du backend NestJS
 */

// Register Request - correspond au backend
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String,
    val password: String,
    val role: String
)

// Register Response - le backend retourne {user: {...}, message: "..."}
data class RegisterResponse(
    val user: UserResponse,
    val message: String
)

// Login Request
data class LoginRequest(
    val email: String,
    val password: String
)

// QR Login Request
data class QrLoginRequest(
    val qrCode: String
)

// Google Login Request
data class GoogleLoginRequest(
    val idToken: String
)

// Login Response - retourne access_token et user
data class LoginResponse(
    val access_token: String,
    val user: UserResponse
)

// QR Login Response - retourne access_token et child (pas user)
data class QrLoginResponse(
    val access_token: String,
    val child: ChildResponse
)

// Child Response - structure pour les enfants
data class ChildResponse(
    @SerializedName("_id")
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null, // Nullable because backend might not always send it
    val status: String? = null,
    val avatarUrl: String? = null,
    val qrCode: String? = null,
    val parent: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// Verification Request
data class VerificationRequest(
    val email: String,
    val code: String
)

// Verification Response
data class VerificationResponse(
    val message: String,
    val user: UserResponse? = null
)

// Forgot Password Request
data class ForgotPasswordRequest(
    val email: String
)

// Forgot Password Response
data class ForgotPasswordResponse(
    val message: String
)

// Reset Password Request
data class ResetPasswordRequest(
    val email: String,
    val code: String,
    val newPassword: String
)

// Reset Password Response
data class ResetPasswordResponse(
    val message: String
)

// Update User Request
data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val password: String? = null
)

// Update User Response
data class UpdateUserResponse(
    val message: String,
    val user: UserResponse
)

// User Response
data class UserResponse(
    @SerializedName("_id")
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phoneNumber: String? = null,
    val role: String? = null, // Nullable for safety, backend should always send it
    val status: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// --- Messages / Chat DTOs ---

data class ParentRoomResponse(
    @SerializedName("_id")
    val id: String? = null,
    @SerializedName("roomId")
    val roomId: String? = null,
    @SerializedName("childId")
    val childId: String? = null,
    @SerializedName("childName")
    val childName: String? = null,
    @SerializedName("child")
    val child: ChildSummaryResponse? = null,
    @SerializedName("lastMessage")
    val lastMessage: LastMessageResponse? = null,
    @SerializedName("unreadCount")
    val unreadCount: Int? = null,
    @SerializedName("lastMessageAt")
    val lastMessageAt: String? = null,
    @SerializedName("lastMessageTimestamp")
    val lastMessageTimestamp: String? = null
)

data class ChildSummaryResponse(
    @SerializedName("_id")
    val id: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null
)

data class LastMessageResponse(
    @SerializedName("_id")
    val id: String? = null,
    val content: String? = null,
    val text: String? = null,
    val sender: String? = null,
    val senderRole: String? = null,
    val senderModel: String? = null,
    val senderId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val sentAt: String? = null,
    val audio: AudioMessageResponse? = null
)

data class AudioMessageResponse(
    val url: String? = null,
    val durationSec: Double? = null,
    val mimeType: String? = null,
    val sizeBytes: Long? = null
)

data class RoomDetailResponse(
    @SerializedName("_id")
    val id: String,
    val childId: String? = null,
    val child: ChildSummaryResponse? = null,
    val participants: List<String>? = null,
    val lastMessage: LastMessageResponse? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class MessageResponse(
    @SerializedName("_id")
    val id: String,
    @SerializedName(value = "roomId", alternate = ["room"])
    val roomId: String? = null,
    val text: String? = null,
    val audio: AudioMessageResponse? = null,
    val senderModel: String? = null,
    val senderId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class SendTextRequest(
    val text: String,
    val senderModel: String,
    val senderId: String
)

// --- Danger Zone DTOs ---

data class LocationCoordinateDto(
    val lat: Double,
    val lng: Double
)

data class DangerZoneResponse(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val description: String?,
    val parent: String? = null,  // Backend returns parent as string ID, not object
    val center: LocationCoordinateDto,
    val radiusMeters: Double,
    val children: List<String>? = emptyList(),
    val status: String = "ACTIVE",
    val notifyOnEntry: Boolean = true,
    val notifyOnExit: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class SimpleUserResponse(
    @SerializedName("_id")
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
)

data class SimpleChildResponse(
    @SerializedName("_id")
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null
)

data class SimpleDangerZoneResponse(
    @SerializedName("_id")
    val id: String,
    val name: String
)

data class DangerZoneEventResponse(
    @SerializedName("_id")
    val id: String,
    val child: SimpleChildResponse? = null,
    val childId: String = "",
    val dangerZone: SimpleDangerZoneResponse? = null,
    val dangerZoneId: String = "",
    val type: String,
    val location: LocationCoordinateDto,
    val notificationSent: Boolean = false,
    val createdAt: String
)

data class CreateDangerZoneRequestDto(
    val name: String,
    val description: String?,
    val center: LocationCoordinateDto,
    val radiusMeters: Double,
    val children: List<String>,
    val notifyOnEntry: Boolean,
    val notifyOnExit: Boolean
)

data class UpdateDangerZoneRequestDto(
    val name: String? = null,
    val description: String? = null,
    val center: LocationCoordinateDto? = null,
    val radiusMeters: Double? = null,
    val children: List<String>? = null,
    val status: String? = null,
    val notifyOnEntry: Boolean? = null,
    val notifyOnExit: Boolean? = null
)

data class DeleteDangerZoneResponse(
    val message: String
)
