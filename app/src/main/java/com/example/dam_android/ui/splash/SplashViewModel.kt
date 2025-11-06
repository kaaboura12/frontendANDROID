package com.example.dam_android.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran splash
 */
class SplashViewModel : ViewModel() {

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _loadingProgress = MutableLiveData<Int>(0)
    val loadingProgress: LiveData<Int> = _loadingProgress

    init {
        startSplashTimer()
    }

    private fun startSplashTimer() {
        viewModelScope.launch {
            // Simuler le chargement avec progression
            for (i in 0..100 step 10) {
                _loadingProgress.value = i
                delay(200)
            }

            // Naviguer vers l'écran de bienvenue après 2 secondes
            _navigationEvent.value = NavigationEvent.NavigateToWelcome
        }
    }

    fun onNavigationComplete() {
        _navigationEvent.value = null
    }

    sealed class NavigationEvent {
        object NavigateToWelcome : NavigationEvent()
    }
}

