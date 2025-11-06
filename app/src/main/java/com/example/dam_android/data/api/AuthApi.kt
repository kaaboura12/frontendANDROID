package com.example.dam_android.data.api

import com.example.dam_android.data.api.dto.*
import com.example.dam_android.data.model.User
import retrofit2.http.*

/**
 * Interface API pour l'authentification et gestion utilisateur WeldiWin Backend
 * Simplifiée pour retourner directement les objets au lieu de Response<>
 */
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/verify")
    suspend fun verify(@Body request: VerificationRequest): VerificationResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ForgotPasswordResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ResetPasswordResponse

    @GET("users")
    suspend fun getAllUsers(): List<UserResponse>

    /**
     * Mise à jour du profil utilisateur
     * Retourne directement l'objet User au lieu de Response<User>
     * Les erreurs HTTP sont gérées via HttpException dans le try-catch
     */
    @PATCH("users/{id}")
    suspend fun updateUser(
        @Path("id") userId: String,
        @Body request: UpdateUserRequest
    ): User
}
