package com.example.dam_android.network.api

import com.example.dam_android.models.User
import com.example.dam_android.network.api.dto.ForgotPasswordRequest
import com.example.dam_android.network.api.dto.ForgotPasswordResponse
import com.example.dam_android.network.api.dto.LoginRequest
import com.example.dam_android.network.api.dto.LoginResponse
import com.example.dam_android.network.api.dto.GoogleLoginRequest
import com.example.dam_android.network.api.dto.QrLoginRequest
import com.example.dam_android.network.api.dto.QrLoginResponse
import com.example.dam_android.network.api.dto.RegisterRequest
import com.example.dam_android.network.api.dto.RegisterResponse
import com.example.dam_android.network.api.dto.ResetPasswordRequest
import com.example.dam_android.network.api.dto.ResetPasswordResponse
import com.example.dam_android.network.api.dto.UpdateUserRequest
import com.example.dam_android.network.api.dto.UserResponse
import com.example.dam_android.network.api.dto.VerificationRequest
import com.example.dam_android.network.api.dto.VerificationResponse
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

    @POST("auth/login/qr")
    suspend fun loginWithQr(@Body request: QrLoginRequest): QrLoginResponse

    @POST("auth/login/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): LoginResponse

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
