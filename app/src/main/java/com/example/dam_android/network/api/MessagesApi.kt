package com.example.dam_android.network.api

import com.example.dam_android.network.api.dto.MessageResponse
import com.example.dam_android.network.api.dto.ParentRoomResponse
import com.example.dam_android.network.api.dto.RoomDetailResponse
import com.example.dam_android.network.api.dto.SendTextRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MessagesApi {

    @GET("messages/rooms/parent/{parentId}")
    suspend fun getParentRooms(
        @Path("parentId") parentId: String
    ): List<ParentRoomResponse>

    @GET("messages/room/{roomId}")
    suspend fun getRoom(
        @Path("roomId") roomId: String
    ): RoomDetailResponse

    @GET("messages/room/child/{childId}")
    suspend fun getChildRoom(
        @Path("childId") childId: String
    ): RoomDetailResponse

    @GET("messages/room/{roomId}/messages")
    suspend fun getRoomMessages(
        @Path("roomId") roomId: String,
        @Query("limit") limit: Int? = null,
        @Query("beforeId") beforeId: String? = null
    ): List<MessageResponse>

    @POST("messages/room/{roomId}/text")
    suspend fun sendTextMessage(
        @Path("roomId") roomId: String,
        @Body request: SendTextRequest
    ): MessageResponse

    @Multipart
    @POST("messages/room/{roomId}/audio")
    suspend fun sendAudioMessage(
        @Path("roomId") roomId: String,
        @Part file: MultipartBody.Part,
        @Part("senderModel") senderModel: RequestBody,
        @Part("senderId") senderId: RequestBody,
        @Part("durationSec") durationSec: RequestBody? = null
    ): MessageResponse

    @GET("messages/room/{roomId}/audio")
    suspend fun listAudioMessages(
        @Path("roomId") roomId: String,
        @Query("sender") sender: String? = null
    ): List<MessageResponse>

}

