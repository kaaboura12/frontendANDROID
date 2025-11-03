package com.example.dam_android.data.repository

import com.example.dam_android.data.local.UserFileManager
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.User
import com.example.dam_android.data.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class AuthRepository(private val userFileManager: UserFileManager) {

    fun signIn(email: String, password: String): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        delay(500) // Simule un délai

        try {
            // Authentifier l'utilisateur depuis le fichier
            val user = userFileManager.authenticateUser(email, password)

            if (user != null) {
                emit(AuthResult.Success(user))
            } else {
                emit(AuthResult.Error("Email ou mot de passe incorrect"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Erreur: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun signUp(name: String, email: String, password: String, role: UserRole): Flow<AuthResult> = flow {
        emit(AuthResult.Loading)
        delay(500) // Simule un délai

        try {
            // Créer l'objet utilisateur
            val newUser = User(
                id = email.hashCode().toString(),
                email = email,
                name = name,
                password = password,
                role = role
            )

            // Sauvegarder dans le fichier
            val saved = userFileManager.saveUser(newUser)

            if (saved) {
                emit(AuthResult.Success(newUser))
            } else {
                emit(AuthResult.Error("Cet email est déjà utilisé"))
            }
        } catch (e: Exception) {
            emit(AuthResult.Error("Erreur: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    fun resetPassword(email: String): Flow<Boolean> = flow {
        delay(500) // Simule un délai
        val userExists = userFileManager.userExists(email)
        emit(userExists)
    }.flowOn(Dispatchers.IO)

    companion object {
        @Volatile
        private var instance: AuthRepository? = null

        fun getInstance(userFileManager: UserFileManager): AuthRepository {
            return instance ?: synchronized(this) {
                instance ?: AuthRepository(userFileManager).also { instance = it }
            }
        }
    }
}
