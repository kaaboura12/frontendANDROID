package com.example.dam_android.data.api

import android.util.Log
import com.example.dam_android.data.local.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
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
    private const val BASE_URL = "https://weldiwinbackend.vercel.app/"
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

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
}
