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
     * Web Client ID - Hardcoded directly to fix the issue
     * This matches your backend .env file
     */
    private const val HARDCODED_WEB_CLIENT_ID = "874691787165-j3qrph4i6dcgk744f92t0pfsrqp0iqg0.apps.googleusercontent.com"
    
    /**
     * Get the Web Client ID from strings.xml or use hardcoded fallback
     * Returns empty string if not configured
     */
    private fun getWebClientIdFromResources(context: Context): String {
        // First try to read from strings.xml
        val fromResources = try {
            val resourceId = R.string.GOOGLE_CLIENT_ID
            val webClientId = context.getString(resourceId)
            Log.d(TAG, "🔍 Reading from strings.xml: length=${webClientId.length}")
            if (webClientId.isNotBlank()) {
                val trimmed = webClientId.trim()
                Log.d(TAG, "✅ Web Client ID found in strings.xml: ${trimmed.take(30)}...")
                trimmed
            } else {
                Log.w(TAG, "⚠️ strings.xml has empty value, using hardcoded fallback")
                null
            }
        } catch (e: android.content.res.Resources.NotFoundException) {
            Log.w(TAG, "⚠️ Resource GOOGLE_CLIENT_ID not found in strings.xml, using hardcoded fallback")
            null
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ Error reading from strings.xml: ${e.message}, using hardcoded fallback")
            null
        }
        
        // Use hardcoded value as fallback
        return fromResources ?: run {
            Log.d(TAG, "✅ Using hardcoded Web Client ID: ${HARDCODED_WEB_CLIENT_ID.take(30)}...")
            HARDCODED_WEB_CLIENT_ID
        }
    }
    
    /**
     * Get Google Sign-In client with configuration
     * Uses hardcoded Web Client ID to ensure it always works
     * 
     * @param context The application context
     * @param webClientId Optional Web Client ID. If not provided, uses hardcoded value
     */
    fun getGoogleSignInClient(context: Context, webClientId: String? = null): GoogleSignInClient {
        // Use provided Web Client ID, or try from resources, or use hardcoded value (guaranteed)
        val clientId = webClientId?.takeIf { it.isNotBlank() } 
            ?: getWebClientIdFromResources(context)
            ?: HARDCODED_WEB_CLIENT_ID
        
        Log.d(TAG, "🔧 Creating GoogleSignInClient with Web Client ID...")
        Log.d(TAG, "🔧 Client ID length: ${clientId.length} characters")
        Log.d(TAG, "🔧 Client ID starts with: ${clientId.take(20)}...")
        Log.d(TAG, "🔧 Client ID ends with: ...${clientId.takeLast(20)}")
        
        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestIdToken(clientId) // Always request idToken with the Web Client ID
        
        Log.d(TAG, "✅ Google Sign-In configured with Web Client ID")
        Log.d(TAG, "✅ Full Web Client ID: $clientId")
        
        val gso = builder.build()
        val client = GoogleSignIn.getClient(context, gso)
        Log.d(TAG, "✅ GoogleSignInClient created successfully")
        return client
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

