package com.example.dam_android.data.api.dto

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

// Login Response - retourne access_token et user
data class LoginResponse(
    val access_token: String,
    val user: UserResponse
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
    val role: String,
    val status: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
