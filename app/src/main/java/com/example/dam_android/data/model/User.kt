package com.example.dam_android.data.model

import com.google.gson.annotations.SerializedName

enum class UserRole {
    PARENT,
    CHILD
}

data class User(
    @SerializedName("_id")
    val id: String = "",
    val email: String = "",
    val name: String = "",
    @SerializedName("password")
    val password: String = "",
    val role: UserRole = UserRole.CHILD
)
