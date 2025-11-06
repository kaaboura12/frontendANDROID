package com.example.dam_android.ui.signin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SignInViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    private val _isFormValid = MutableLiveData<Boolean>(false)
    val isFormValid: LiveData<Boolean> = _isFormValid

    fun onEmailChanged(email: String) {
        _email.value = email
        validateForm()
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
        validateForm()
    }

    private fun validateForm() {
        val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value ?: "").matches()
        val passwordValid = (_password.value?.length ?: 0) >= 6
        _isFormValid.value = emailValid && passwordValid
    }

    fun signIn() {
        val emailValue = _email.value ?: return
        val passwordValue = _password.value ?: return

        viewModelScope.launch {
            authRepository.signIn(emailValue, passwordValue).collect { result ->
                _authResult.value = result
            }
        }
    }
}

