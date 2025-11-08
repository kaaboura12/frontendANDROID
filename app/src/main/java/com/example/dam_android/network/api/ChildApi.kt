package com.example.dam_android.network.api

import com.example.dam_android.models.AddChildRequest
import com.example.dam_android.models.AddChildResponse
import com.example.dam_android.models.ChildModel
import retrofit2.Response
import retrofit2.http.*

interface ChildApi {

    @POST("children")
    suspend fun addChild(@Body request: AddChildRequest): Response<AddChildResponse>

    @GET("children")
    suspend fun getChildren(): Response<List<ChildModel>>

    @GET("children/{childId}")
    suspend fun getChild(@Path("childId") childId: String): Response<ChildModel>

    @DELETE("children/{childId}")
    suspend fun deleteChild(@Path("childId") childId: String): Response<Unit>
}

