package com.example.dam_android.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.dam_android.data.model.User
import com.example.dam_android.data.model.UserRole

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "UserSession"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_LAST_NAME = "user_last_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_AUTH_TOKEN = "auth_token"

        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun saveUser(user: User, token: String? = null) {
        prefs.edit().apply {
            putString(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_LAST_NAME, user.lastName)
            putString(KEY_USER_PHONE, user.phoneNumber)
            putString(KEY_USER_ROLE, user.role.name)
            putBoolean(KEY_IS_LOGGED_IN, true)
            token?.let { putString(KEY_AUTH_TOKEN, it) }
            apply()
        }
    }

    fun getUser(): User? {
        if (!isLoggedIn()) return null

        val roleString = prefs.getString(KEY_USER_ROLE, UserRole.CHILD.name) ?: UserRole.CHILD.name

        return User(
            id = prefs.getString(KEY_USER_ID, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            name = prefs.getString(KEY_USER_NAME, "") ?: "",
            lastName = prefs.getString(KEY_USER_LAST_NAME, "") ?: "",
            phoneNumber = prefs.getString(KEY_USER_PHONE, "") ?: "",
            roleString = roleString
        )
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
