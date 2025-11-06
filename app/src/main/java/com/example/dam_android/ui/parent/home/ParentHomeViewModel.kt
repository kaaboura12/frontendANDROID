package com.example.dam_android.ui.parent.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.model.User
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran d'accueil parent
 */
class ParentHomeViewModel : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _children = MutableLiveData<List<ChildInfo>>(emptyList())
    val children: LiveData<List<ChildInfo>> = _children

    private val _selectedNavItem = MutableLiveData<NavItem>(NavItem.HOME)
    val selectedNavItem: LiveData<NavItem> = _selectedNavItem

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    fun setUser(user: User?) {
        _user.value = user
    }

    fun loadChildren() {
        viewModelScope.launch {
            // TODO: Charger les enfants depuis le repository
            // Pour l'instant, données de test
            _children.value = listOf(
                ChildInfo("1", "Pierre", "En ligne", true),
                ChildInfo("2", "Marie", "Hors ligne", false)
            )
        }
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

    fun onChildCardClicked(childId: String) {
        _toastMessage.value = "Détails de l'enfant: $childId"
        // TODO: Navigate to child details
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

    data class ChildInfo(
        val id: String,
        val name: String,
        val status: String,
        val isOnline: Boolean
    )

    sealed class NavigationEvent {
        object NavigateToProfile : NavigationEvent()
        data class NavigateToChildDetails(val childId: String) : NavigationEvent()
    }
}

