package com.example.dam_android.models

/**
 * Représente l'état de validation d'un formulaire
 */
data class FormValidation(
    val isValid: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val nameError: String? = null
)

