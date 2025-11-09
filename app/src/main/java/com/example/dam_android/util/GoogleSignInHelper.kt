package com.example.dam_android.util

import android.content.Context
import android.util.Log
import com.example.dam_android.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

/**
 * Helper class for Google Sign-In
 * 
 * IMPORTANT: To use Google Sign-In, you need to:
 * 1. Create a project in Google Cloud Console
 * 2. Enable Google Sign-In API
 * 3. Create OAuth 2.0 credentials (Web application type)
 * 4. Get your Web Client ID
     * 5. Add your Web Client ID to strings.xml (GOOGLE_CLIENT_ID)
 * 
 * For development/testing without a Web Client ID, the app will still work
 * but you won't be able to get the idToken needed for backend authentication.
 */
object GoogleSignInHelper {
    private const val TAG = "GoogleSignInHelper"
    
    /**
     * Get the Web Client ID from strings.xml
     * Returns empty string if not configured
     */
    private fun getWebClientIdFromResources(context: Context): String {
        return try {
            val webClientId = context.getString(R.string.GOOGLE_CLIENT_ID)
            if (webClientId.isBlank()) {
                Log.w(TAG, "⚠️ GOOGLE_CLIENT_ID is empty in strings.xml")
                Log.w(TAG, "⚠️ Please add your Web Client ID to res/values/strings.xml")
                ""
            } else {
                Log.d(TAG, "✅ Web Client ID found in strings.xml")
                webClientId.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error reading GOOGLE_CLIENT_ID from strings.xml", e)
            ""
        }
    }
    
    /**
     * Get Google Sign-In client with configuration
     * If Web Client ID is not provided, it will still work but idToken will be null
     * 
     * @param context The application context
     * @param webClientId Optional Web Client ID. If not provided, will read from strings.xml
     */
    fun getGoogleSignInClient(context: Context, webClientId: String? = null): GoogleSignInClient {
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
        
        // Get Web Client ID from parameter or from strings.xml
        val clientId = webClientId?.takeIf { it.isNotBlank() } 
            ?: getWebClientIdFromResources(context)
        
        // Only request idToken if Web Client ID is provided and valid
        if (clientId.isNotBlank() && clientId != "YOUR_WEB_CLIENT_ID_HERE") {
            builder.requestIdToken(clientId)
            Log.d(TAG, "✅ Google Sign-In configured with Web Client ID: ${clientId.take(20)}...")
        } else {
            Log.e(TAG, "❌ Web Client ID not configured - Google Sign-In will fail with status code 10")
            Log.e(TAG, "❌ Please add your Web Client ID to res/values/strings.xml")
            Log.e(TAG, "❌ Get it from: https://console.cloud.google.com/apis/credentials")
            Log.e(TAG, "❌ Make sure it's a Web application type OAuth 2.0 Client ID")
        }
        
        val gso = builder.build()
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * Get ID token from GoogleSignInAccount
     */
    fun getIdToken(account: GoogleSignInAccount): String? {
        val idToken = account.idToken
        if (idToken == null) {
            Log.w(TAG, "⚠️ idToken is null - Web Client ID might not be configured")
        }
        return idToken
    }
}

