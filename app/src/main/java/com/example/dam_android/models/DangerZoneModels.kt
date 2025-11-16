package com.example.dam_android.models

import com.example.dam_android.network.api.dto.*
import com.google.gson.annotations.SerializedName

/**
 * Domain models for danger zones
 */
data class DangerZone(
    val id: String,
    val name: String,
    val description: String?,
    val center: LocationCoordinate,
    val radiusMeters: Double,
    val children: List<String>,
    val status: ZoneStatus,
    val notifyOnEntry: Boolean,
    val notifyOnExit: Boolean,
    val parentId: String?,
    val createdAt: String?,
    val updatedAt: String?
)

data class LocationCoordinate(
    val lat: Double,
    val lng: Double
)

enum class ZoneStatus {
    ACTIVE, INACTIVE
}

data class DangerZoneEvent(
    val id: String,
    val childId: String,
    val childName: String,
    val dangerZoneId: String,
    val dangerZoneName: String,
    val type: EventType,
    val location: LocationCoordinate,
    val notificationSent: Boolean,
    val createdAt: String
)

enum class EventType {
    ENTER, EXIT
}

data class CreateDangerZoneRequest(
    val name: String,
    val description: String?,
    val center: LocationCoordinate,
    val radiusMeters: Double,
    val children: List<String> = emptyList(),
    val notifyOnEntry: Boolean = true,
    val notifyOnExit: Boolean = false
)

data class UpdateDangerZoneRequest(
    val name: String? = null,
    val description: String? = null,
    val center: LocationCoordinate? = null,
    val radiusMeters: Double? = null,
    val children: List<String>? = null,
    val status: String? = null,
    val notifyOnEntry: Boolean? = null,
    val notifyOnExit: Boolean? = null
)

// Extension functions to convert from DTOs to domain models
fun DangerZoneResponse.toDomain(): DangerZone {
    return DangerZone(
        id = id,
        name = name,
        description = description,
        center = LocationCoordinate(center.lat, center.lng),
        radiusMeters = radiusMeters,
        children = children ?: emptyList(),
        status = if (status == "ACTIVE") ZoneStatus.ACTIVE else ZoneStatus.INACTIVE,
        notifyOnEntry = notifyOnEntry,
        notifyOnExit = notifyOnExit,
        parentId = parent,  // parent is now a string ID
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun DangerZoneEventResponse.toDomain(): DangerZoneEvent {
    val childFirstName = child?.firstName ?: ""
    val childLastName = child?.lastName ?: ""
    val childFullName = "$childFirstName $childLastName".trim()
    
    return DangerZoneEvent(
        id = id,
        childId = child?.id ?: childId,
        childName = childFullName.ifEmpty { "Unknown Child" },
        dangerZoneId = dangerZone?.id ?: dangerZoneId,
        dangerZoneName = dangerZone?.name ?: "Unknown Zone",
        type = if (type == "ENTER") EventType.ENTER else EventType.EXIT,
        location = LocationCoordinate(location.lat, location.lng),
        notificationSent = notificationSent,
        createdAt = createdAt
    )
}

