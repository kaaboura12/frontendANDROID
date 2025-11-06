package com.example.dam_android.data.model

import com.google.gson.annotations.SerializedName

enum class UserRole {
    PARENT,
    CHILD
}

/**
 * Modèle User complet correspondant exactement à la structure MongoDB du backend
 * Tous les champs sont mappés avec @SerializedName pour une correspondance exacte
 */
data class User(
    @SerializedName("_id")
    val id: String = "",

    @SerializedName("firstName")
    val name: String = "",

    @SerializedName("lastName")
    val lastName: String = "",

    @SerializedName("email")
    val email: String = "",

    @SerializedName("phoneNumber")
    val phoneNumber: String = "",

    @SerializedName("role")
    val roleString: String = "CHILD",

    @SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @SerializedName("status")
    val status: String? = "ACTIVE",

    @SerializedName("isVerified")
    val isVerified: Boolean = false,

    @SerializedName("verificationCode")
    val verificationCode: String? = null,

    @SerializedName("verificationCodeExpiresAt")
    val verificationCodeExpiresAt: String? = null,

    @SerializedName("verificationChannel")
    val verificationChannel: String? = "email",

    @SerializedName("passwordResetCode")
    val passwordResetCode: String? = null,

    @SerializedName("passwordResetExpiresAt")
    val passwordResetExpiresAt: String? = null,

    @SerializedName("lastCodeSentAt")
    val lastCodeSentAt: String? = null,

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("__v")
    val version: Int? = 0,

    // Champ non sérialisé pour stocker le mot de passe (jamais envoyé par l'API)
    @Transient
    val password: String = ""
) {
    /**
     * Propriété calculée pour obtenir le rôle en tant qu'enum
     * Utilise un try-catch pour gérer les valeurs inconnues
     */
    val role: UserRole
        get() = try {
            UserRole.valueOf(roleString)
        } catch (e: IllegalArgumentException) {
            UserRole.CHILD
        }

    /**
     * Nom complet de l'utilisateur
     */
    val fullName: String
        get() = "$name $lastName".trim()
}
