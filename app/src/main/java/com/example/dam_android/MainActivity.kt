package com.example.dam_android

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.dam_android.databinding.ActivityMainBinding
import com.example.dam_android.data.local.SessionManager
import com.example.dam_android.data.api.RetrofitClient

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "onCreate: Démarrage MainActivity")

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            Log.d(TAG, "onCreate: View binding OK")

            // Initialiser RetrofitClient avec SessionManager pour le support des tokens
            val sessionManager = SessionManager.getInstance(this)
            RetrofitClient.init(sessionManager)
            Log.d(TAG, "onCreate: RetrofitClient initialisé avec SessionManager")

            // Masquer l'ActionBar
            supportActionBar?.hide()

            Log.d(TAG, "onCreate: MainActivity prête")

        } catch (e: Exception) {
            Log.e(TAG, "onCreate: ERREUR CRITIQUE", e)
            e.printStackTrace()
        }
    }
}