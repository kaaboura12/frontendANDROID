package com.example.dam_android

import android.app.Application
import android.preference.PreferenceManager
import android.util.Log
import org.osmdroid.config.Configuration
import java.net.InetAddress

/**
 * Classe Application pour initialiser les services au démarrage
 */
class DamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize osmdroid configuration ONCE globally
        initOsmdroid()

        // Force le pré-chargement du DNS pour Vercel
        preloadDns()
    }

    /**
     * Initialize osmdroid to prevent grey tiles and enable tile downloads
     */
    private fun initOsmdroid() {
        try {
            val ctx = applicationContext
            Configuration.getInstance().load(
                ctx,
                PreferenceManager.getDefaultSharedPreferences(ctx)
            )
            // Set user agent to app package name
            Configuration.getInstance().userAgentValue = packageName
            Log.d("DamApplication", "✅ osmdroid initialized successfully")
        } catch (e: Exception) {
            Log.e("DamApplication", "❌ Failed to initialize osmdroid: ${e.message}", e)
        }
    }

    /**
     * Pré-charge la résolution DNS pour éviter les problèmes sur émulateur
     */
    private fun preloadDns() {
        Thread {
            try {
                Log.d("DamApplication", "🔄 Pré-chargement DNS pour weldiwinbackend.vercel.app...")
                val addresses = InetAddress.getAllByName("weldiwinbackend.vercel.app")
                Log.d("DamApplication", "✅ DNS résolu: ${addresses.joinToString { it.hostAddress ?: "unknown" }}")
            } catch (e: Exception) {
                Log.e("DamApplication", "❌ Échec résolution DNS: ${e.message}")
                Log.e("DamApplication", "⚠️ Vérifiez que l'émulateur a accès à Internet")
            }
        }.start()
    }
}
