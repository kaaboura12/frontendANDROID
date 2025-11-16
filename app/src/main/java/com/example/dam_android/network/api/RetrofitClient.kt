package com.example.dam_android.network.api

import android.util.Log
import com.example.dam_android.network.local.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Service Retrofit pour communiquer avec le backend API
 * Configuration simplifiée
 */
object RetrofitClient {

    // Backend hébergé sur Vercel
    //private const val BASE_URL = "https://weldiwinbackend.vercel.app/"
    private const val BASE_URL = "http://10.0.2.2:3005/"

    private const val TAG = "RetrofitClient"

    private var sessionManager: SessionManager? = null

    fun init(sessionManager: SessionManager) {
        this.sessionManager = sessionManager
    }

    private val authInterceptor = Interceptor { chain ->
        val token = sessionManager?.getAuthToken()
        val request = chain.request()

        val newRequest = if (token != null) {
            Log.d(TAG, "🔑 Adding auth token to request: ${request.url}")
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()
        } else {
            Log.w(TAG, "⚠️ No auth token available for request: ${request.url}")
            request
        }

        chain.proceed(newRequest)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    init {
        Log.d(TAG, "🌐 Configuration API:")
        Log.d(TAG, "   Base URL: $BASE_URL")
        Log.d(TAG, "   Timeouts: 60s")
    }

    // Configure Gson to NOT serialize null values (like iOS probably does)
    // Also register custom deserializers to handle inconsistent backend responses
    private val gson = GsonBuilder()
        .registerTypeAdapter(com.example.dam_android.models.ChildModel::class.java, com.example.dam_android.models.ChildModelDeserializer())
        .registerTypeAdapter(com.example.dam_android.network.api.dto.DangerZoneResponse::class.java, com.example.dam_android.network.api.dto.DangerZoneDeserializer())
        .create() // By default, Gson doesn't serialize nulls

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val childApi: ChildApi = retrofit.create(ChildApi::class.java)
    val messagesApi: MessagesApi = retrofit.create(MessagesApi::class.java)
    val dangerZoneApi: DangerZoneApi = retrofit.create(DangerZoneApi::class.java)

}
