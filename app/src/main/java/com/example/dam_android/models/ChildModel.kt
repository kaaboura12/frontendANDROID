package com.example.dam_android.models

import android.util.Log
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import java.lang.reflect.Type

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

/**
 * Custom deserializer for ChildModel that handles parent field
 * which can be either a String (ID) or an Object (full User object)
 */
class ChildModelDeserializer : JsonDeserializer<ChildModel> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ChildModel {
        try {
            if (json == null || !json.isJsonObject) {
                throw JsonParseException("Expected JSON object")
            }

            val jsonObject = json.asJsonObject
            
            // Extract _id
            val id = jsonObject.get("_id")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            
            // Extract firstName
            val firstName = jsonObject.get("firstName")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            
            // Extract lastName
            val lastName = jsonObject.get("lastName")?.takeIf { it.isJsonPrimitive }?.asString ?: ""
            
            // Handle parent field - can be String, Object, or Null
            val parentId = extractParentId(jsonObject)
            
            // Handle linkedParents - can be array of strings or array of objects
            val linkedParentsList = extractLinkedParents(jsonObject)
            
            // Extract avatarUrl
            val avatarUrl = jsonObject.get("avatarUrl")?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString
            
            // Extract location
            val location = extractLocation(jsonObject, context)
            
            // Extract deviceId
            val deviceId = jsonObject.get("deviceId")?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString
            
            // Extract deviceType
            val deviceType = jsonObject.get("deviceType")?.takeIf { it.isJsonPrimitive }?.asString ?: "PHONE"
            
            // Extract isOnline
            val isOnline = jsonObject.get("isOnline")?.takeIf { it.isJsonPrimitive }?.asBoolean ?: false
            
            // Extract status
            val status = jsonObject.get("status")?.takeIf { it.isJsonPrimitive }?.asString ?: "ACTIVE"
            
            // Extract qrCode
            val qrCode = jsonObject.get("qrCode")?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString
            
            return ChildModel(
                _id = id,
                firstName = firstName,
                lastName = lastName,
                parent = parentId,
                linkedParents = linkedParentsList,
                avatarUrl = avatarUrl,
                location = location,
                deviceId = deviceId,
                deviceType = deviceType,
                isOnline = isOnline,
                status = status,
                qrCode = qrCode
            )
        } catch (e: Exception) {
            throw JsonParseException("Error deserializing ChildModel: ${e.message}", e)
        }
    }
    
    private fun extractParentId(jsonObject: JsonObject): String {
        if (!jsonObject.has("parent")) {
            return ""
        }
        
        val parentElement = jsonObject.get("parent")
        if (parentElement.isJsonNull) {
            return ""
        }
        
        return try {
            when {
                parentElement.isJsonPrimitive -> {
                    // It's a string (ID)
                    parentElement.asString
                }
                parentElement.isJsonObject -> {
                    // It's an object, extract the _id
                    val parentObj = parentElement.asJsonObject
                    when {
                        parentObj.has("_id") -> {
                            val idElement = parentObj.get("_id")
                            if (idElement.isJsonPrimitive && !idElement.isJsonNull) {
                                idElement.asString
                            } else {
                                ""
                            }
                        }
                        parentObj.has("id") -> {
                            val idElement = parentObj.get("id")
                            if (idElement.isJsonPrimitive && !idElement.isJsonNull) {
                                idElement.asString
                            } else {
                                ""
                            }
                        }
                        else -> ""
                    }
                }
                else -> ""
            }
        } catch (e: Exception) {
            // Log error but return empty string to prevent crash
            Log.e("ChildModelDeserializer", "Error extracting parent ID: ${e.message}", e)
            ""
        }
    }
    
    private fun extractLinkedParents(jsonObject: JsonObject): List<String> {
        val linkedParentsList = mutableListOf<String>()
        
        if (!jsonObject.has("linkedParents")) {
            return linkedParentsList
        }
        
        val linkedParentsElement = jsonObject.get("linkedParents")
        if (linkedParentsElement.isJsonNull || !linkedParentsElement.isJsonArray) {
            return linkedParentsList
        }
        
        val linkedParentsArray = linkedParentsElement.asJsonArray
        linkedParentsArray.forEach { element ->
            when {
                element.isJsonPrimitive && !element.isJsonNull -> {
                    linkedParentsList.add(element.asString)
                }
                element.isJsonObject -> {
                    val linkedParentObj = element.asJsonObject
                    val linkedParentId = when {
                        linkedParentObj.has("_id") && !linkedParentObj.get("_id").isJsonNull -> {
                            linkedParentObj.get("_id").asString
                        }
                        linkedParentObj.has("id") && !linkedParentObj.get("id").isJsonNull -> {
                            linkedParentObj.get("id").asString
                        }
                        else -> ""
                    }
                    if (linkedParentId.isNotEmpty()) {
                        linkedParentsList.add(linkedParentId)
                    }
                }
            }
        }
        
        return linkedParentsList
    }
    
    private fun extractLocation(
        jsonObject: JsonObject,
        context: JsonDeserializationContext?
    ): Location? {
        if (!jsonObject.has("location")) {
            return null
        }
        
        val locationElement = jsonObject.get("location")
        if (locationElement.isJsonNull || !locationElement.isJsonObject) {
            return null
        }
        
        return try {
            context?.deserialize(locationElement, Location::class.java)
        } catch (e: Exception) {
            // If deserialization fails, try to extract manually
            val locationObj = locationElement.asJsonObject
            val lat = locationObj.get("lat")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
            val lng = locationObj.get("lng")?.takeIf { it.isJsonPrimitive }?.asDouble ?: 0.0
            val updatedAt = locationObj.get("updatedAt")?.takeIf { it.isJsonPrimitive && !it.isJsonNull }?.asString
            Location(lat = lat, lng = lng, updatedAt = updatedAt)
        }
    }
}

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

