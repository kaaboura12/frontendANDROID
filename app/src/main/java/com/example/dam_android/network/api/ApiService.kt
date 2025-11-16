package com.example.dam_android.network.api

import android.util.Log
import com.example.dam_android.network.api.dto.ForgotPasswordRequest
import com.example.dam_android.network.api.dto.GoogleLoginRequest
import com.example.dam_android.network.api.dto.LoginRequest
import com.example.dam_android.network.api.dto.QrLoginRequest
import com.example.dam_android.network.api.dto.RegisterRequest
import com.example.dam_android.network.api.dto.ResetPasswordRequest
import com.example.dam_android.network.api.dto.UpdateUserRequest
import com.example.dam_android.network.api.dto.VerificationRequest
import com.example.dam_android.network.api.dto.UserResponse
import com.example.dam_android.models.User
import com.example.dam_android.models.ParentChatRoom
import com.example.dam_android.models.ChatMessage
import com.example.dam_android.models.ChatRoomDetail
import com.example.dam_android.network.api.dto.SendTextRequest
import com.example.dam_android.models.toDomain
import com.google.gson.JsonSyntaxException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Service pour gérer les appels API d'authentification WeldiWin Backend
 */
object ApiService {

    private const val TAG = "ApiService"
    private val api = RetrofitClient.authApi
    private val messageApi = RetrofitClient.messagesApi
    private val dangerZoneApi = RetrofitClient.dangerZoneApi

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
                    roleString = userResponse.role ?: "PARENT"
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
                roleString = userResponse.role ?: "PARENT",
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
     * Connexion d'un enfant avec QR code
     * Retourne un Pair<User, String> où le String est le token d'authentification
     */
    suspend fun loginChildWithQr(qrCode: String): Result<Pair<User, String>> {
        return try {
            val request = QrLoginRequest(qrCode)
            Log.d(TAG, "📤 Envoi requête login QR: qrCode=${qrCode.take(10)}...")
            val loginResponse = api.loginWithQr(request)

            val childResponse = loginResponse.child
            // Convert child to User for consistency with the rest of the app
            val user = User(
                id = childResponse.id,
                name = childResponse.firstName,
                lastName = childResponse.lastName,
                email = childResponse.email ?: "${childResponse.firstName.lowercase()}.${childResponse.lastName.lowercase()}@child.weldiwin.com",
                phoneNumber = childResponse.phoneNumber ?: "",
                roleString = childResponse.role ?: "CHILD", // Default to CHILD if role is null
                password = ""
            )
            Log.d(TAG, "✅ Connexion QR réussie: ${user.name} ${user.lastName}, Token: ${loginResponse.access_token.take(20)}...")
            Result.success(Pair(user, loginResponse.access_token))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur connexion QR - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                errorBody?.let {
                    if (it.contains("message")) {
                        it.substringAfter("\"message\":\"").substringBefore("\"")
                    } else it
                } ?: "QR code invalide ou expiré"
            } catch (ex: Exception) {
                "QR code invalide ou expiré"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception loginChildWithQr: ${e.message}", e)
            Result.failure(Exception("Erreur de connexion: ${e.message}"))
        }
    }

    /**
     * Connexion avec Google
     * Retourne un Pair<User, String> où le String est le token d'authentification
     */
    suspend fun loginWithGoogle(idToken: String): Result<Pair<User, String>> {
        return try {
            val request = GoogleLoginRequest(idToken)
            Log.d(TAG, "📤 Envoi requête login Google: idToken=${idToken.take(20)}...")
            val loginResponse = api.loginWithGoogle(request)

            val userResponse = loginResponse.user
            val user = User(
                id = userResponse.id,
                name = userResponse.firstName,
                lastName = userResponse.lastName,
                email = userResponse.email,
                phoneNumber = userResponse.phoneNumber ?: "",
                roleString = userResponse.role ?: "PARENT", // Default to PARENT for Google sign-in
                password = ""
            )
            Log.d(TAG, "✅ Connexion Google réussie: ${user.email}, Token: ${loginResponse.access_token.take(20)}...")
            Result.success(Pair(user, loginResponse.access_token))
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur connexion Google - Code ${e.code()}: $errorBody")

            val errorMsg = try {
                errorBody?.let {
                    if (it.contains("message")) {
                        it.substringAfter("\"message\":\"").substringBefore("\"")
                    } else it
                } ?: "Token Google invalide"
            } catch (ex: Exception) {
                "Token Google invalide"
            }

            Result.failure(Exception(errorMsg))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception loginWithGoogle: ${e.message}", e)
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
                    roleString = userResponse.role ?: "PARENT"
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
                    roleString = userResponse.role ?: "PARENT",
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

    /**
     * Récupère la liste des rooms de discussion pour un parent
     */
    suspend fun getParentChatRooms(parentId: String): Result<List<ParentChatRoom>> {
        return try {
            val responses = messageApi.getParentRooms(parentId)
            val rooms = responses.map { it.toDomain() }
            Result.success(rooms)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur getParentChatRooms - Code ${e.code()}: $errorBody")

            val message = when (e.code()) {
                401 -> "Session expirée. Veuillez vous reconnecter."
                403 -> "Accès refusé."
                else -> errorBody ?: "Impossible de récupérer les conversations."
            }
            Result.failure(Exception(message))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout getParentChatRooms: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour getParentChatRooms", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getParentChatRooms: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    /**
     * Récupère (ou crée) la room de discussion pour un enfant
     */
    suspend fun getChildChatRoom(childId: String): Result<ChatRoomDetail> {
        return try {
            val response = messageApi.getChildRoom(childId)
            Result.success(response.toDomain())
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur getChildChatRoom - Code ${e.code()}: $errorBody")

            val message = when (e.code()) {
                401 -> "Session expirée. Veuillez vous reconnecter."
                403 -> "Accès refusé."
                404 -> "Conversation introuvable."
                else -> errorBody ?: "Impossible de récupérer la conversation."
            }
            Result.failure(Exception(message))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout getChildChatRoom: ${e.message}", e)
            Result.failure(Exception("Le serveur met trop de temps à répondre."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Pas de connexion internet (getChildChatRoom)", e)
            Result.failure(Exception("Pas de connexion internet."))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getChildChatRoom: ${e.message}", e)
            Result.failure(Exception("Erreur inattendue: ${e.message}"))
        }
    }

    suspend fun getChatRoom(roomId: String): Result<ChatRoomDetail> {
        return try {
            val response = messageApi.getRoom(roomId)
            Result.success<ChatRoomDetail>(response.toDomain())
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur getChatRoom - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Impossible de récupérer la conversation."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout getChatRoom: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour getChatRoom", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getChatRoom: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    suspend fun getChatMessages(roomId: String, limit: Int = 50, beforeId: String? = null): Result<List<ChatMessage>> {
        return try {
            val responses = messageApi.getRoomMessages(roomId, limit, beforeId)
            val messages: List<ChatMessage> = responses.map { it.toDomain() }
            Result.success<List<ChatMessage>>(messages)
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur getChatMessages - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Impossible de récupérer les messages."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout getChatMessages: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour getChatMessages", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getChatMessages: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    suspend fun sendChatTextMessage(
        roomId: String,
        text: String,
        senderModel: String,
        senderId: String
    ): Result<ChatMessage> {
        return try {
            val request = SendTextRequest(
                text = text,
                senderModel = senderModel,
                senderId = senderId
            )
            val response = messageApi.sendTextMessage(roomId, request)
            Result.success<ChatMessage>(response.toDomain())
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur sendChatTextMessage - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Impossible d'envoyer le message."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout sendChatTextMessage: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour sendChatTextMessage", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception sendChatTextMessage: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    suspend fun sendChatAudioMessage(
        roomId: String,
        file: java.io.File,
        durationSec: Double?,
        senderModel: String,
        senderId: String
    ): Result<ChatMessage> {
        return try {
            val mediaType = "audio/*".toMediaTypeOrNull()
            val fileBody = file.asRequestBody(mediaType)
            val filePart = MultipartBody.Part.createFormData("file", file.name, fileBody)
            val senderModelBody: RequestBody = senderModel.toRequestBody("text/plain".toMediaTypeOrNull())
            val senderIdBody: RequestBody = senderId.toRequestBody("text/plain".toMediaTypeOrNull())
            val durationBody: RequestBody? = durationSec?.let {
                it.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            }
            val response = messageApi.sendAudioMessage(
                roomId = roomId,
                file = filePart,
                senderModel = senderModelBody,
                senderId = senderIdBody,
                durationSec = durationBody
            )
            Result.success(response.toDomain())
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur sendChatAudioMessage - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Impossible d'envoyer le message audio."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout sendChatAudioMessage: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour sendChatAudioMessage", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception sendChatAudioMessage: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    suspend fun getChatAudioMessages(roomId: String, sender: String? = null): Result<List<ChatMessage>> {
        return try {
            val responses = messageApi.listAudioMessages(roomId, sender)
            Result.success(responses.map { it.toDomain() })
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur getChatAudioMessages - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Impossible de récupérer les messages audio."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout getChatAudioMessages: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour getChatAudioMessages", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getChatAudioMessages: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    /**
     * Récupère tous les enfants d'un parent avec leurs locations
     */
    suspend fun getParentChildren(): Result<List<com.example.dam_android.models.ChildModel>> {
        return try {
            val response = RetrofitClient.childApi.getChildren()
            if (response.isSuccessful && response.body() != null) {
                val children = response.body()!!
                Log.d(TAG, "✅ ${children.size} enfants récupérés")
                Result.success(children)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Erreur getParentChildren - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Impossible de récupérer les enfants"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Erreur getParentChildren - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Impossible de récupérer les enfants"))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "⏳ Timeout getParentChildren: ${e.message}", e)
            Result.failure(Exception("Le serveur est trop lent. Réessayez plus tard."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "🌐 Aucune connexion pour getParentChildren", e)
            Result.failure(Exception("Aucune connexion Internet"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getParentChildren: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Erreur inattendue"))
        }
    }

    suspend fun linkParentByQr(qrCode: String): Result<com.example.dam_android.models.LinkParentResponse> {
        return try {
            val request = com.example.dam_android.models.LinkParentRequest(qrCode = qrCode)
            val response = RetrofitClient.childApi.linkParentByQr(request)
            
            if (response.isSuccessful && response.body() != null) {
                val linkResponse = response.body()!!
                Log.d(TAG, "✅ Successfully linked to child: ${linkResponse.child.firstName}")
                Result.success(linkResponse)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error linkParentByQr - Code ${response.code()}: $errorBody")
                
                // Parse error message
                val errorMessage = try {
                    if (errorBody != null) {
                        val json = org.json.JSONObject(errorBody)
                        json.optString("message", "Failed to link to child")
                    } else {
                        "Failed to link to child"
                    }
                } catch (e: Exception) {
                    "Failed to link to child"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error linkParentByQr - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to link to child"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception linkParentByQr: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteChild(childId: String): Result<Unit> {
        return try {
            val response = RetrofitClient.childApi.deleteChild(childId)
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Successfully deleted child: $childId")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error deleteChild - Code ${response.code()}: $errorBody")
                
                // Parse error message
                val errorMessage = try {
                    if (errorBody != null) {
                        val json = org.json.JSONObject(errorBody)
                        json.optString("message", "Failed to delete child")
                    } else {
                        "Failed to delete child"
                    }
                } catch (e: Exception) {
                    "Failed to delete child"
                }
                
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error deleteChild - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to delete child"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception deleteChild: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ==================== DANGER ZONE API METHODS ====================

    /**
     * Create a new danger zone
     */
    suspend fun createDangerZone(
        name: String,
        description: String?,
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Double,
        children: List<String> = emptyList(),
        notifyOnEntry: Boolean = true,
        notifyOnExit: Boolean = false
    ): Result<com.example.dam_android.models.DangerZone> {
        return try {
            val request = com.example.dam_android.network.api.dto.CreateDangerZoneRequestDto(
                name = name,
                description = description,
                center = com.example.dam_android.network.api.dto.LocationCoordinateDto(centerLat, centerLng),
                radiusMeters = radiusMeters,
                children = children,
                notifyOnEntry = notifyOnEntry,
                notifyOnExit = notifyOnExit
            )
            
            Log.d(TAG, "📤 Creating danger zone: $name at ($centerLat, $centerLng) radius=$radiusMeters")
            val response = dangerZoneApi.createDangerZone(request)
            
            if (response.isSuccessful && response.body() != null) {
                val zone = response.body()!!.toDomain()
                Log.d(TAG, "✅ Danger zone created successfully: ${zone.id}")
                Result.success(zone)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error creating danger zone - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to create danger zone"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error createDangerZone - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to create danger zone"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception createDangerZone: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error creating danger zone"))
        }
    }

    /**
     * Get all danger zones for the authenticated parent
     */
    suspend fun getAllDangerZones(): Result<List<com.example.dam_android.models.DangerZone>> {
        return try {
            Log.d(TAG, "📤 Fetching all danger zones")
            val response = dangerZoneApi.getAllDangerZones()
            
            if (response.isSuccessful && response.body() != null) {
                val zones = response.body()!!.map { it.toDomain() }
                Log.d(TAG, "✅ Fetched ${zones.size} danger zones")
                Result.success(zones)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error fetching danger zones - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to fetch danger zones"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error getAllDangerZones - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to fetch danger zones"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getAllDangerZones: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error fetching danger zones"))
        }
    }

    /**
     * Get a specific danger zone by ID
     */
    suspend fun getDangerZoneById(zoneId: String): Result<com.example.dam_android.models.DangerZone> {
        return try {
            Log.d(TAG, "📤 Fetching danger zone: $zoneId")
            val response = dangerZoneApi.getDangerZoneById(zoneId)
            
            if (response.isSuccessful && response.body() != null) {
                val zone = response.body()!!.toDomain()
                Log.d(TAG, "✅ Fetched danger zone: ${zone.name}")
                Result.success(zone)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error fetching danger zone - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to fetch danger zone"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error getDangerZoneById - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to fetch danger zone"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getDangerZoneById: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error fetching danger zone"))
        }
    }

    /**
     * Update a danger zone
     */
    suspend fun updateDangerZone(
        zoneId: String,
        name: String? = null,
        description: String? = null,
        centerLat: Double? = null,
        centerLng: Double? = null,
        radiusMeters: Double? = null,
        children: List<String>? = null,
        status: String? = null,
        notifyOnEntry: Boolean? = null,
        notifyOnExit: Boolean? = null
    ): Result<com.example.dam_android.models.DangerZone> {
        return try {
            val center = if (centerLat != null && centerLng != null) {
                com.example.dam_android.network.api.dto.LocationCoordinateDto(centerLat, centerLng)
            } else null
            
            val request = com.example.dam_android.network.api.dto.UpdateDangerZoneRequestDto(
                name = name,
                description = description,
                center = center,
                radiusMeters = radiusMeters,
                children = children,
                status = status,
                notifyOnEntry = notifyOnEntry,
                notifyOnExit = notifyOnExit
            )
            
            Log.d(TAG, "📤 Updating danger zone: $zoneId")
            val response = dangerZoneApi.updateDangerZone(zoneId, request)
            
            if (response.isSuccessful && response.body() != null) {
                val zone = response.body()!!.toDomain()
                Log.d(TAG, "✅ Danger zone updated successfully: ${zone.name}")
                Result.success(zone)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error updating danger zone - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to update danger zone"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error updateDangerZone - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to update danger zone"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception updateDangerZone: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error updating danger zone"))
        }
    }

    /**
     * Delete a danger zone
     */
    suspend fun deleteDangerZone(zoneId: String): Result<String> {
        return try {
            Log.d(TAG, "📤 Deleting danger zone: $zoneId")
            val response = dangerZoneApi.deleteDangerZone(zoneId)
            
            if (response.isSuccessful && response.body() != null) {
                val message = response.body()!!.message
                Log.d(TAG, "✅ Danger zone deleted: $message")
                Result.success(message)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error deleting danger zone - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to delete danger zone"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error deleteDangerZone - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to delete danger zone"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception deleteDangerZone: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error deleting danger zone"))
        }
    }

    /**
     * Get event history for a danger zone
     */
    suspend fun getDangerZoneEvents(zoneId: String): Result<List<com.example.dam_android.models.DangerZoneEvent>> {
        return try {
            Log.d(TAG, "📤 Fetching events for danger zone: $zoneId")
            val response = dangerZoneApi.getDangerZoneEvents(zoneId)
            
            if (response.isSuccessful && response.body() != null) {
                val events = response.body()!!.map { it.toDomain() }
                Log.d(TAG, "✅ Fetched ${events.size} events")
                Result.success(events)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error fetching events - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to fetch events"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error getDangerZoneEvents - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to fetch events"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getDangerZoneEvents: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error fetching events"))
        }
    }

    /**
     * Get all active danger zones for a specific child
     */
    suspend fun getChildActiveDangerZones(childId: String): Result<List<com.example.dam_android.models.DangerZone>> {
        return try {
            Log.d(TAG, "📤 Fetching active zones for child: $childId")
            val response = dangerZoneApi.getChildActiveDangerZones(childId)
            
            if (response.isSuccessful && response.body() != null) {
                val zones = response.body()!!.map { it.toDomain() }
                Log.d(TAG, "✅ Fetched ${zones.size} active zones for child")
                Result.success(zones)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "❌ Error fetching child zones - Code ${response.code()}: $errorBody")
                Result.failure(Exception(errorBody ?: "Failed to fetch child zones"))
            }
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "❌ Error getChildActiveDangerZones - Code ${e.code()}: $errorBody")
            Result.failure(Exception(errorBody ?: "Failed to fetch child zones"))
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception getChildActiveDangerZones: ${e.message}", e)
            Result.failure(Exception(e.message ?: "Error fetching child zones"))
        }
    }

}
