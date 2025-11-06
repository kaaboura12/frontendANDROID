package com.example.dam_android.data.repository

import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.User
import com.example.dam_android.data.model.UserRole
import com.example.dam_android.data.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository d'authentification utilisant l'API backend
 * Communique avec le serveur Node.js + MongoDB
 */
class AuthRepository {

    fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)

        try {
            val result = ApiService.loginUser(email, password)

            if (result.isSuccess) {
                val (user, token) = result.getOrNull()!!
                // Retourner un AuthResult qui inclut le token
                emit(AuthResult.Success(user, token))
            } else {
                emit(AuthResult.Error(result.exceptionOrNull()?.message ?: "Échec de connexion"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Erreur de connexion: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun signUp(name: String, lastName: String, email: String, password: String, role: UserRole): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)

        try {
            val result = ApiService.registerUser(name, lastName, email, password, role.name)

            if (result.isSuccess) {
                val user = result.getOrNull()!!
                emit(AuthResult.Success(user))
            } else {
                emit(AuthResult.Error(result.exceptionOrNull()?.message ?: "Échec d'inscription"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Erreur d'inscription: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun resetPassword(email: String): Flow<Result<String>> = flow {
        try {
            val result = ApiService.forgotPassword(email)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    fun resetPasswordWithCode(email: String, code: String, newPassword: String): Flow<Result<String>> = flow {
        try {
            val result = ApiService.resetPasswordWithCode(email, code, newPassword)
            emit(result)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    // Récupérer tous les utilisateurs
    suspend fun getAllUsers(): List<User> {
        return try {
            val result = ApiService.getAllUsers()
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
