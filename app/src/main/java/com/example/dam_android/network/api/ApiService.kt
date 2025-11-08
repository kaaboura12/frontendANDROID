package com.example.dam_android.network.api

import android.util.Log
import com.example.dam_android.network.api.dto.ForgotPasswordRequest
import com.example.dam_android.network.api.dto.LoginRequest
import com.example.dam_android.network.api.dto.RegisterRequest
import com.example.dam_android.network.api.dto.ResetPasswordRequest
import com.example.dam_android.network.api.dto.UpdateUserRequest
import com.example.dam_android.network.api.dto.VerificationRequest
import com.example.dam_android.network.api.dto.UserResponse
import com.example.dam_android.models.User
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Service pour gérer les appels API d'authentification WeldiWin Backend
 */
object ApiService {

    private const val TAG = "ApiService"
    private val api = RetrofitClient.authApi

    /**
     * Inscription d'un nouvel utilisateur via l'API
     * Le backend attend: firstName, lastName, email, phoneNumber, password, role
     */
    suspend fun registerUser(firstName: String, lastName: String, email: String, password: String, role: String): Result<User> {
        return try {
            // Générer un numéro de téléphone par défaut (requis par le backend)
            val phoneNumber = "+1234567890" // À remplacer par un vrai numéro

            val request = RegisterRequest(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phoneNumber,
                password = password,
                role = role
            )

            Log.d(TAG, "📤 Envoi requête inscription: firstName=$firstName, lastName=$lastName, email=$email, role=$role")
            val registerResponse = api.register(request)

            if (registerResponse.user != null) {
                val userResponse = registerResponse.user
                val user = User(
                    id = userResponse.id,
                    name = userResponse.firstName,
                    lastName = userResponse.lastName,
                    email = userResponse.email,
                    phoneNumber = userResponse.phoneNumber ?: "",
                    password = "",
                    roleString = userResponse.role
                )
                Log.d(TAG, "✅ Inscription réussie: ${user.email}, Message: ${registerResponse.message}")
                Result.success(user)
            } else {
                Log.e(TAG, "❌ Erreur: User null dans la réponse")
                Result.failure(Exception("Erreur lors de l'inscription"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur inscription - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                errorBody?.let {
                    if (it.contains("message")) {
                        it.substringAfter("\"message\":\"").substringBefore("\"")
                    } else it
                } ?: "Erreur d'inscription"
            } catch (ex: Exception) {
                "Erreur d'inscription - Code ${e.code()}"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception registerUser: ${e.message}", e)
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Connexion d'un utilisateur via l'API
     * Retourne un Pair<User, String> où le String est le token d'authentification
     */
    suspend fun loginUser(email: String, password: String): Result<Pair<User, String>> {
        return try {
            val request = LoginRequest(email, password)
            Log.d(TAG, "📤 Envoi requête login: email=$email")
            val loginResponse = api.login(request)

            val userResponse = loginResponse.user
            val user = User(
                id = userResponse.id,
                name = userResponse.firstName,
                lastName = userResponse.lastName,
                email = userResponse.email,
                phoneNumber = userResponse.phoneNumber ?: "",
                roleString = userResponse.role,
                password = ""
            )
            Log.d(TAG, "✅ Connexion réussie: ${user.email}, Token: ${loginResponse.access_token.take(20)}...")
            Result.success(Pair(user, loginResponse.access_token))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur connexion - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                errorBody?.let {
                    if (it.contains("message")) {
                        it.substringAfter("\"message\":\"").substringBefore("\"")
                    } else it
                } ?: "Email ou mot de passe incorrect"
            } catch (ex: Exception) {
                "Email ou mot de passe incorrect"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception loginUser: ${e.message}", e)
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Vérification du code à 6 chiffres
     */
    suspend fun verifyCode(email: String, code: String): Result<User> {
        return try {
            val request = VerificationRequest(email, code)
            Log.d(TAG, "📤 Envoi requête vérification: email=$email, code=$code")
            val verificationResponse = api.verify(request)

            if (verificationResponse.user != null) {
                val userResponse = verificationResponse.user
                val user = User(
                    id = userResponse.id,
                    name = userResponse.firstName,
                    lastName = userResponse.lastName,
                    email = userResponse.email,
                    phoneNumber = userResponse.phoneNumber ?: "",
                    password = "",
                    roleString = userResponse.role
                )
                Log.d(TAG, "✅ Vérification réussie: ${user.email}")
                Result.success(user)
            } else {
                Log.e(TAG, "❌ Erreur: User null dans la réponse")
                Result.failure(Exception("Erreur lors de la vérification"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur vérification - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                errorBody?.let {
                    if (it.contains("message")) {
                        it.substringAfter("\"message\":\"").substringBefore("\"")
                    } else it
                } ?: "Code invalide ou expiré"
            } catch (ex: Exception) {
                "Code invalide ou expiré"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception verifyCode: ${e.message}", e)
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Récupérer tous les utilisateurs via l'API
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val userResponses: List<UserResponse> = api.getAllUsers()
            val users = userResponses.map { userResponse ->
                User(
                    id = userResponse.id,
                    name = userResponse.firstName,
                    lastName = userResponse.lastName,
                    email = userResponse.email,
                    phoneNumber = userResponse.phoneNumber ?: "",
                    roleString = userResponse.role,
                    password = ""
                )
            }

            Log.d(TAG, "✅ ${users.size} utilisateurs récupérés")
            Result.success(users)
        } catch (e: HttpException) {
            Log.e(TAG, "❌ Erreur getAllUsers: ${e.code()}")
            Result.failure(Exception("Erreur lors de la récupération des utilisateurs"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getAllUsers: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Mot de passe oublié - Envoie un email de réinitialisation
     */
    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val request = ForgotPasswordRequest(email)
            Log.d(TAG, "📤 Envoi requête mot de passe oublié: email=$email")

            val forgotPasswordResponse = api.forgotPassword(request)
            Log.d(TAG, "✅ Email de réinitialisation envoyé: ${forgotPasswordResponse.message}")
            Result.success(forgotPasswordResponse.message)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur forgot-password - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                if (errorBody != null) {
                    val jsonPattern = """"message"\s*:\s*"([^"]+)"""".toRegex()
                    val match = jsonPattern.find(errorBody)
                    val backendMessage = match?.groupValues?.get(1)

                    when {
                        backendMessage?.contains("not found", ignoreCase = true) == true ||
                        backendMessage?.contains("n'existe pas", ignoreCase = true) == true ->
                            "Aucun compte trouvé avec cet email"

                        backendMessage?.contains("not verified", ignoreCase = true) == true ||
                        backendMessage?.contains("non vérifié", ignoreCase = true) == true ->
                            "Compte non vérifié. Veuillez d'abord vérifier votre email."

                        backendMessage != null -> backendMessage

                        e.code() == 404 -> "Aucun compte trouvé avec cet email"
                        e.code() == 400 -> "Email invalide"

                        else -> "Erreur lors de l'envoi (Code: ${e.code()})"
                    }
                } else {
                    "Erreur serveur (Code: ${e.code()})"
                }
            } catch (ex: Exception) {
                Log.e(TAG, "❌ Erreur parsing: ${ex.message}", ex)
                "Erreur lors de l'envoi"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception forgotPassword: ${e.message}", e)
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Réinitialiser le mot de passe avec le code reçu par email
     */
    suspend fun resetPasswordWithCode(email: String, code: String, newPassword: String): Result<String> {
        return try {
            val request = ResetPasswordRequest(email, code, newPassword)
            Log.d(TAG, "📤 Envoi requête reset-password: email=$email, code=$code")

            val resetPasswordResponse = api.resetPassword(request)
            Log.d(TAG, "✅ Mot de passe réinitialisé: ${resetPasswordResponse.message}")
            Result.success(resetPasswordResponse.message)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur reset-password - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                if (errorBody != null) {
                    val jsonPattern = """"message"\s*:\s*"([^"]+)"""".toRegex()
                    val match = jsonPattern.find(errorBody)
                    val backendMessage = match?.groupValues?.get(1)

                    when {
                        backendMessage?.contains("invalid", ignoreCase = true) == true ||
                        backendMessage?.contains("incorrect", ignoreCase = true) == true ->
                            "Code invalide ou expiré"

                        backendMessage?.contains("expired", ignoreCase = true) == true ->
                            "Le code a expiré. Demandez un nouveau code."

                        backendMessage != null -> backendMessage

                        e.code() == 400 -> "Code invalide"
                        e.code() == 404 -> "Compte non trouvé"

                        else -> "Erreur lors de la réinitialisation"
                    }
                } else {
                    "Erreur serveur"
                }
            } catch (ex: Exception) {
                Log.e(TAG, "❌ Erreur parsing: ${ex.message}", ex)
                "Erreur lors de la réinitialisation"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception resetPasswordWithCode: ${e.message}", e)
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Mettre à jour le profil utilisateur
     * Version simplifiée qui retourne directement l'utilisateur mis à jour
     * Utilise HttpException pour gérer les erreurs HTTP
     */
    @Suppress("RETURN_TYPE_MISMATCH_ON_OVERRIDE")
    suspend fun updateUser(userId: String, firstName: String?, lastName: String?, phoneNumber: String?, password: String?): Result<User> {
        val request = UpdateUserRequest(
            firstName = firstName,
            lastName = lastName,
            phoneNumber = phoneNumber,
            password = password
        )

        Log.d(TAG, "📤 Envoi requête mise à jour profil: userId=$userId")
        Log.d(TAG, "📤 Données: firstName=$firstName, lastName=$lastName, phoneNumber=$phoneNumber")

        return try {
            val user: User = api.updateUser(userId, request)

            Log.d(TAG, "✅ Profil mis à jour avec succès!")
            Log.d(TAG, "✅ Utilisateur: ${user.fullName} (${user.email})")
            Log.d(TAG, "✅ Téléphone: ${user.phoneNumber}")
            Log.d(TAG, "✅ ID: ${user.id}")

            Result.success<User>(user)

        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur HTTP ${e.code()}: $errorBody")

            val errorMsg = try {
                errorBody?.let {
                    if (it.contains("message")) {
                        it.substringAfter("\"message\":\"").substringBefore("\"")
                    } else it
                } ?: when (e.code()) {
                    401 -> "Non autorisé. Veuillez vous reconnecter."
                    403 -> "Accès interdit"
                    404 -> "Utilisateur non trouvé"
                    else -> "Erreur lors de la mise à jour"
                }
            } catch (_: Exception) {
                "Erreur lors de la mise à jour (HTTP ${e.code()})"
            }

            return Result.failure<User>(Exception(errorMsg))

        } catch (e: UnknownHostException) {
            Log.e(TAG, "❌ Pas de connexion internet", e)
            return Result.failure<User>(Exception("Pas de connexion internet"))

        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "❌ Délai d'attente dépassé", e)
            return Result.failure<User>(Exception("Délai d'attente dépassé"))

        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "❌ Erreur de parsing JSON", e)
            return Result.failure<User>(Exception("Erreur de format de données"))

        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception updateUser: ${e.javaClass.simpleName} - ${e.message}", e)
            return Result.failure<User>(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Alias pour resetPasswordWithCode - pour compatibilité
     */
    suspend fun resetPassword(email: String, code: String, newPassword: String): Result<String> {
        return resetPasswordWithCode(email, code, newPassword)
    }
}
