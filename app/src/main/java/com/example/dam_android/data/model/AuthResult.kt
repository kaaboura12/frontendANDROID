package com.example.dam_android.data.model

sealed class AuthResult {
    data class Success(val user: User, val token: String? = null) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}
