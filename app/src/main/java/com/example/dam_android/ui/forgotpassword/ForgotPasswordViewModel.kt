package com.example.dam_android.ui.forgotpassword

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
            try {
                authRepository.resetPassword(emailValue).collect { result ->
                    if (result.isSuccess) {
                        val message = result.getOrNull() ?: "Code de réinitialisation envoyé"
                        _resetResult.value = ResetPasswordResult.Success(message)
                    } else {
                        val error = result.exceptionOrNull()?.message
                            ?: "Impossible d'envoyer l'email. Vérifiez que le compte existe et est vérifié."
                        _resetResult.value = ResetPasswordResult.Error(error)
                    }
                }
            } catch (e: Exception) {
                _resetResult.value = ResetPasswordResult.Error(
                    e.message ?: "Erreur lors de l'envoi"
                )
            }
        }
    }

    sealed class ResetPasswordResult {
        data class Success(val message: String) : ResetPasswordResult()
        data class Error(val message: String) : ResetPasswordResult()
        object Loading : ResetPasswordResult()
    }
}

