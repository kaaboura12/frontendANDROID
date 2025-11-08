package com.example.dam_android.screens

/**
 * Routes de navigation pour Jetpack Compose
 */
object NavRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{email}"
    const val VERIFICATION = "verification/{email}"
    const val HOME = "home"
    const val PARENT_HOME = "parent_home"
    const val CHILD_HOME = "child_home"
    const val PROFILE = "profile"
    const val GALLERY = "gallery"

    fun resetPassword(email: String) = "reset_password/$email"
    fun verification(email: String) = "verification/$email"
}

