package com.example.dam_android.viewmodel

// Ce fichier n'est plus utilisé car tous les écrans Compose utilisent l'état local avec remember
// au lieu de ViewModels. Il est conservé pour compatibilité mais peut être supprimé.

// Si vous avez besoin d'un ViewModel à l'avenir, décommentez ce code et créez les ViewModels correspondants.

/*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dam_android.network.repository.AuthRepository

class ViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        throw IllegalArgumentException("ViewModels non implémentés - Utiliser l'état local avec Compose")
    }
}
*/
