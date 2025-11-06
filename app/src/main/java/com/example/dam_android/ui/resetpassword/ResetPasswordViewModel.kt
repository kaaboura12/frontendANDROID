package com.example.dam_android.ui.resetpassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ResetPasswordViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _resetResult = MutableLiveData<ResetPasswordResult>()
    val resetResult: LiveData<ResetPasswordResult> = _resetResult

    private val _resendResult = MutableLiveData<ResendResult?>()
    val resendResult: LiveData<ResendResult?> = _resendResult

    private val _code = MutableLiveData<String>()
    private val _newPassword = MutableLiveData<String>()
    private val _confirmPassword = MutableLiveData<String>()

    private val _isFormValid = MutableLiveData<Boolean>(false)
    val isFormValid: LiveData<Boolean> = _isFormValid

    fun onCodeChanged(code: String) {
        _code.value = code
        validateForm()
    }

    fun onNewPasswordChanged(password: String) {
        _newPassword.value = password
        validateForm()
    }

    fun onConfirmPasswordChanged(password: String) {
        _confirmPassword.value = password
        validateForm()
    }

    private fun validateForm() {
        val code = _code.value ?: ""
        val newPassword = _newPassword.value ?: ""
        val confirmPassword = _confirmPassword.value ?: ""

        val isValid = code.length == 6 &&
                newPassword.length >= 6 &&
                newPassword == confirmPassword

        _isFormValid.value = isValid
    }

    fun resetPassword(email: String) {
        val code = _code.value ?: return
        val newPassword = _newPassword.value ?: return
        val confirmPassword = _confirmPassword.value ?: return

        if (newPassword != confirmPassword) {
            _resetResult.value = ResetPasswordResult.Error("Les mots de passe ne correspondent pas")
            return
        }

        if (newPassword.length < 6) {
            _resetResult.value = ResetPasswordResult.Error("Le mot de passe doit contenir au moins 6 caractères")
            return
        }

        _resetResult.value = ResetPasswordResult.Loading

        viewModelScope.launch {
            try {
                authRepository.resetPasswordWithCode(email, code, newPassword).collect { result ->
                    if (result.isSuccess) {
                        _resetResult.value = ResetPasswordResult.Success(
                            "Mot de passe réinitialisé avec succès !"
                        )
                    } else {
                        val error = result.exceptionOrNull()?.message
                            ?: "Code invalide ou expiré"
                        _resetResult.value = ResetPasswordResult.Error(error)
                    }
                }
            } catch (e: Exception) {
                _resetResult.value = ResetPasswordResult.Error(
                    e.message ?: "Erreur lors de la réinitialisation"
                )
            }
        }
    }

    fun resendCode(email: String) {
        viewModelScope.launch {
            try {
                authRepository.resetPassword(email).collect { result ->
                    if (result.isSuccess) {
                        _resendResult.value = ResendResult.Success("Code renvoyé avec succès")
                    } else {
                        _resendResult.value = ResendResult.Error("Erreur lors du renvoi du code")
                    }
                }
            } catch (e: Exception) {
                _resendResult.value = ResendResult.Error(e.message ?: "Erreur réseau")
            }
        }
    }

    sealed class ResetPasswordResult {
        data class Success(val message: String) : ResetPasswordResult()
        data class Error(val message: String) : ResetPasswordResult()
        object Loading : ResetPasswordResult()
    }

    sealed class ResendResult {
        data class Success(val message: String) : ResendResult()
        data class Error(val message: String) : ResendResult()
    }
}

