package com.example.dam_android.ui.child.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.model.User
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran d'accueil enfant
 */
class ChildHomeViewModel : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _selectedNavItem = MutableLiveData<NavItem>(NavItem.HOME)
    val selectedNavItem: LiveData<NavItem> = _selectedNavItem

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _dangerAlertSent = MutableLiveData<Boolean>(false)
    val dangerAlertSent: LiveData<Boolean> = _dangerAlertSent

    fun setUser(user: User?) {
        _user.value = user
    }

    fun onNavItemSelected(navItem: NavItem) {
        _selectedNavItem.value = navItem

        when (navItem) {
            NavItem.HOME -> {
                // Déjà sur l'accueil
            }
            NavItem.CHILD -> {
                _toastMessage.value = "Child section"
            }
            NavItem.LOCATION -> {
                _toastMessage.value = "Location section"
            }
            NavItem.ACTIVITY -> {
                _toastMessage.value = "Activity section"
            }
            NavItem.PROFILE -> {
                _navigationEvent.value = NavigationEvent.NavigateToProfile
            }
        }
    }

    fun onDangerButtonClicked() {
        viewModelScope.launch {
            // TODO: Envoyer l'alerte de danger aux parents via API
            _dangerAlertSent.value = true
            _toastMessage.value = "Alert envoyée aux parents!"

            // Réinitialiser après quelques secondes
            kotlinx.coroutines.delay(3000)
            _dangerAlertSent.value = false
        }
    }

    fun onToastShown() {
        _toastMessage.value = null
    }

    fun onNavigationComplete() {
        _navigationEvent.value = null
    }

    enum class NavItem {
        HOME, CHILD, LOCATION, ACTIVITY, PROFILE
    }

    sealed class NavigationEvent {
        object NavigateToProfile : NavigationEvent()
    }
}

