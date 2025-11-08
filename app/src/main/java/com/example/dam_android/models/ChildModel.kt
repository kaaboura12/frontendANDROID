package com.example.dam_android.models

import com.google.gson.annotations.SerializedName

data class Location(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val updatedAt: String? = null
)

data class ChildModel(
    val _id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val parent: String = "",
    val linkedParents: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val location: Location? = null,
    val deviceId: String? = null,
    val deviceType: String = "PHONE",
    val isOnline: Boolean = false,
    val status: String = "ACTIVE",
    val qrCode: String? = null
)

enum class DeviceType {
    PHONE, WATCH
}

data class AddChildRequest(
    val firstName: String,
    val lastName: String,
    val deviceType: String
)

data class AddChildResponse(
    val _id: String,
    val firstName: String,
    val lastName: String,
    val parent: String,
    val linkedParents: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val location: Location? = null,
    val deviceId: String? = null,
    val deviceType: String,
    val isOnline: Boolean = false,
    val status: String = "ACTIVE",
    val qrCode: String
)

