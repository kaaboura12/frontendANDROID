package com.example.dam_android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.UserRole
import com.example.dam_android.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _name = MutableLiveData<String>()
    val name: LiveData<String> = _name

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String> = _password

    private val _confirmPassword = MutableLiveData<String>()
    val confirmPassword: LiveData<String> = _confirmPassword

    private val _role = MutableLiveData<UserRole>(UserRole.CHILD)
    val role: LiveData<UserRole> = _role

    private val _isFormValid = MutableLiveData<Boolean>(false)
    val isFormValid: LiveData<Boolean> = _isFormValid

    fun onNameChanged(name: String) {
        _name.value = name
        validateForm()
    }

    fun onEmailChanged(email: String) {
        _email.value = email
        validateForm()
    }

    fun onPasswordChanged(password: String) {
        _password.value = password
        validateForm()
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
        validateForm()
    }

    fun onRoleChanged(role: UserRole) {
        _role.value = role
    }

    private fun validateForm() {
        val nameValid = (_name.value?.length ?: 0) >= 2
        val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(_email.value ?: "").matches()
        val passwordValid = (_password.value?.length ?: 0) >= 6
        val passwordsMatch = _password.value == _confirmPassword.value

        _isFormValid.value = nameValid && emailValid && passwordValid && passwordsMatch
    }

    fun signUp() {
        val nameValue = _name.value ?: return
        val emailValue = _email.value ?: return
        val passwordValue = _password.value ?: return
        val roleValue = _role.value ?: UserRole.CHILD

        viewModelScope.launch {
            authRepository.signUp(nameValue, emailValue, passwordValue, roleValue).collect { result ->
                _authResult.value = result
            }
        }
    }
}
