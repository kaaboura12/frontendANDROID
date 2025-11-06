package com.example.dam_android

import android.app.Application
import android.util.Log
import java.net.InetAddress

/**
 * Classe Application pour initialiser les services au démarrage
 */
class DamApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Force le pré-chargement du DNS pour Vercel
        preloadDns()
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
