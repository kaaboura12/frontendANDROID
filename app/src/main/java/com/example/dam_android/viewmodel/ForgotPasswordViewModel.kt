package com.example.dam_android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _resetResult = MutableLiveData<ResetPasswordResult>()
    val resetResult: LiveData<ResetPasswordResult> = _resetResult

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _isEmailValid = MutableLiveData<Boolean>(false)
    val isEmailValid: LiveData<Boolean> = _isEmailValid

    fun onEmailChanged(email: String) {
        _email.value = email
        _isEmailValid.value = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun resetPassword() {
        val emailValue = _email.value ?: return

        _resetResult.value = ResetPasswordResult.Loading

        viewModelScope.launch {
            authRepository.resetPassword(emailValue).collect { success ->
                _resetResult.value = if (success) {
                    ResetPasswordResult.Success("Lien de réinitialisation envoyé à $emailValue")
                } else {
                    ResetPasswordResult.Error("Aucun compte trouvé avec cet email")
                }
            }
        }
    }

    sealed class ResetPasswordResult {
        data class Success(val message: String) : ResetPasswordResult()
        data class Error(val message: String) : ResetPasswordResult()
        object Loading : ResetPasswordResult()
    }
}

