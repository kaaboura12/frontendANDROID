package com.example.dam_android.network.api

import com.example.dam_android.network.api.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Danger Zone API interface for Retrofit
 * All endpoints require authentication via Bearer token
 */
interface DangerZoneApi {

    /**
     * Create a new danger zone
     * POST /danger-zones
     */
    @POST("danger-zones")
    suspend fun createDangerZone(
        @Body request: CreateDangerZoneRequestDto
    ): Response<DangerZoneResponse>

    /**
     * Get all danger zones for the authenticated parent
     * GET /danger-zones
     */
    @GET("danger-zones")
    suspend fun getAllDangerZones(): Response<List<DangerZoneResponse>>

    /**
     * Get a specific danger zone by ID
     * GET /danger-zones/:id
     */
    @GET("danger-zones/{id}")
    suspend fun getDangerZoneById(
        @Path("id") zoneId: String
    ): Response<DangerZoneResponse>

    /**
     * Update a danger zone
     * PATCH /danger-zones/:id
     */
    @PATCH("danger-zones/{id}")
    suspend fun updateDangerZone(
        @Path("id") zoneId: String,
        @Body request: UpdateDangerZoneRequestDto
    ): Response<DangerZoneResponse>

    /**
     * Delete a danger zone
     * DELETE /danger-zones/:id
     */
    @DELETE("danger-zones/{id}")
    suspend fun deleteDangerZone(
        @Path("id") zoneId: String
    ): Response<DeleteDangerZoneResponse>

    /**
     * Get event history for a specific danger zone
     * GET /danger-zones/:id/events
     */
    @GET("danger-zones/{id}/events")
    suspend fun getDangerZoneEvents(
        @Path("id") zoneId: String
    ): Response<List<DangerZoneEventResponse>>

    /**
     * Get all active danger zones monitoring a specific child
     * GET /danger-zones/child/:childId/active
     */
    @GET("danger-zones/child/{childId}/active")
    suspend fun getChildActiveDangerZones(
        @Path("childId") childId: String
    ): Response<List<DangerZoneResponse>>
}

