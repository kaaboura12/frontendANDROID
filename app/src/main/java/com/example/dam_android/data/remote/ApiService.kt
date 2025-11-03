package com.example.dam_android.data.remote

import com.example.dam_android.data.model.User
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("users/register")
    suspend fun registerUser(@Body user: User): Response<UserResponse>

    @POST("users/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<UserResponse>

    @GET("users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<User>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") userId: String, @Body user: User): Response<User>

    @GET("users")
    suspend fun getAllUsers(): Response<List<User>>
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val success: Boolean,
    val message: String,
    val user: User?
)

