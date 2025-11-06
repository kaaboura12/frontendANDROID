package com.example.dam_android.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.api.ApiService
import com.example.dam_android.data.model.User
import kotlinx.coroutines.launch

/**
 * ViewModel pour l'écran de profil
 */
class ProfileViewModel : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _navigationEvent = MutableLiveData<NavigationEvent?>()
    val navigationEvent: LiveData<NavigationEvent?> = _navigationEvent

    private val _showLogoutDialog = MutableLiveData<Boolean>(false)
    val showLogoutDialog: LiveData<Boolean> = _showLogoutDialog

    private val _updateResult = MutableLiveData<UpdateResult?>()
    val updateResult: LiveData<UpdateResult?> = _updateResult

    fun setUser(user: User?) {
        _user.value = user
    }

    fun onEditProfileClicked() {
        _navigationEvent.value = NavigationEvent.NavigateToEditProfile
    }

    fun onLogoutClicked() {
        _showLogoutDialog.value = true
    }

    fun onLogoutConfirmed() {
        _showLogoutDialog.value = false
        _navigationEvent.value = NavigationEvent.NavigateToSignIn
    }

    fun onLogoutCancelled() {
        _showLogoutDialog.value = false
    }

    fun onBackPressed() {
        _navigationEvent.value = NavigationEvent.NavigateBack
    }

    fun updateProfile(userId: String, firstName: String, lastName: String, phoneNumber: String, password: String?) {
        viewModelScope.launch {
            _updateResult.value = UpdateResult.Loading

            val result = ApiService.updateUser(
                userId = userId,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                password = if (password.isNullOrBlank()) null else password
            )

            _updateResult.value = if (result.isSuccess) {
                val updatedUser = result.getOrNull()
                _user.value = updatedUser
                UpdateResult.Success(updatedUser!!)
            } else {
                UpdateResult.Error(result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour")
            }
        }
    }

    fun onNavigationComplete() {
        _navigationEvent.value = null
    }

    fun onUpdateResultHandled() {
        _updateResult.value = null
    }

    sealed class NavigationEvent {
        object NavigateToSignIn : NavigationEvent()
        object NavigateBack : NavigationEvent()
        object NavigateToEditProfile : NavigationEvent()
    }

    sealed class UpdateResult {
        object Loading : UpdateResult()
        data class Success(val user: User) : UpdateResult()
        data class Error(val message: String) : UpdateResult()
    }
}
