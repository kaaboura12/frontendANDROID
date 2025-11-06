package com.example.dam_android.ui.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel pour l'écran de bienvenue
 */
class WelcomeViewModel : ViewModel() {

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    fun onGetStartedClicked() {
        _navigationEvent.value = NavigationEvent.NavigateToSignIn
    }

    fun onNavigationComplete() {
        _navigationEvent.value = null
    }

    sealed class NavigationEvent {
        object NavigateToSignIn : NavigationEvent()
    }
}

