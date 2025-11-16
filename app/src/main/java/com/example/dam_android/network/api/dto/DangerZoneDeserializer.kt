package com.example.dam_android.network.api.dto

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom deserializer for DangerZoneResponse to handle inconsistent backend responses
 * Backend sometimes returns parent as string ID, sometimes as object
 */
class DangerZoneDeserializer : JsonDeserializer<DangerZoneResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): DangerZoneResponse {
        val jsonObject = json.asJsonObject
        
        // Extract parent ID - handle both string and object formats
        val parentId = when {
            !jsonObject.has("parent") || jsonObject.get("parent").isJsonNull -> null
            jsonObject.get("parent").isJsonPrimitive -> jsonObject.get("parent").asString
            jsonObject.get("parent").isJsonObject -> {
                jsonObject.getAsJsonObject("parent").get("_id")?.asString
            }
            else -> null
        }
        
        // Extract children IDs - handle both string array and object array formats
        val childrenIds = if (jsonObject.has("children") && !jsonObject.get("children").isJsonNull) {
            val childrenArray = jsonObject.getAsJsonArray("children")
            childrenArray.mapNotNull { element ->
                when {
                    element.isJsonPrimitive -> element.asString // Simple string ID
                    element.isJsonObject -> element.asJsonObject.get("_id")?.asString // Populated object
                    else -> null
                }
            }
        } else {
            emptyList()
        }
        
        return DangerZoneResponse(
            id = jsonObject.get("_id").asString,
            name = jsonObject.get("name").asString,
            description = jsonObject.get("description")?.takeIf { !it.isJsonNull }?.asString,
            parent = parentId,
            center = context.deserialize<LocationCoordinateDto>(jsonObject.get("center"), LocationCoordinateDto::class.java),
            radiusMeters = jsonObject.get("radiusMeters").asDouble,
            children = childrenIds,
            status = jsonObject.get("status")?.asString ?: "ACTIVE",
            notifyOnEntry = jsonObject.get("notifyOnEntry")?.asBoolean ?: true,
            notifyOnExit = jsonObject.get("notifyOnExit")?.asBoolean ?: false,
            createdAt = jsonObject.get("createdAt")?.takeIf { !it.isJsonNull }?.asString,
            updatedAt = jsonObject.get("updatedAt")?.takeIf { !it.isJsonNull }?.asString
        )
    }
}

