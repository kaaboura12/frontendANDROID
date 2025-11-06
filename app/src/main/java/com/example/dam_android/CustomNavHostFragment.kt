package com.example.dam_android

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment

/**
 * Custom NavHostFragment qui corrige le bug de double enregistrement
 * du SavedStateProvider lors de la restauration d'état
 */
class CustomNavHostFragment : NavHostFragment() {

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        // Workaround pour éviter le bug de double enregistrement du SavedStateProvider
        // On ne fait rien de spécial ici, juste laisser le parent gérer
        try {
            super.onViewCreated(view, savedInstanceState)
        } catch (e: IllegalArgumentException) {
            // Si on attrape l'erreur "SavedStateProvider already registered"
            // on l'ignore car c'est le bug connu de Navigation Component
            if (e.message?.contains("SavedStateProvider") == true &&
                e.message?.contains("already registered") == true) {
                // Ignorer silencieusement ce bug connu
            } else {
                // Si c'est une autre erreur, la relancer
                throw e
            }
        }
    }
}
