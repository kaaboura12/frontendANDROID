package com.example.dam_android.ui.signup

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.model.AuthResult
import com.example.dam_android.data.model.UserRole
import com.example.dam_android.data.repository.AuthRepository
import kotlinx.coroutines.launch

class SignUpViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val TAG = "SignUpViewModel"

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _name = MutableLiveData<String>()
    val name: LiveData<String> = _name

    private val _lastName = MutableLiveData<String>()
    val lastName: LiveData<String> = _lastName

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

    fun onLastNameChanged(lastName: String) {
        _lastName.value = lastName
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
        val nameValue = _name.value ?: ""
        val lastNameValue = _lastName.value ?: ""
        val emailValue = _email.value ?: ""
        val passwordValue = _password.value ?: ""
        val confirmPasswordValue = _confirmPassword.value ?: ""

        val nameValid = nameValue.length >= 2
        val lastNameValid = lastNameValue.length >= 2
        val emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()
        val passwordValid = passwordValue.length >= 6
        val passwordsMatch = passwordValue == confirmPasswordValue && passwordValue.isNotEmpty()

        Log.d(TAG, "Validation - Name: $nameValid (${nameValue.length}), LastName: $lastNameValid (${lastNameValue.length}), Email: $emailValid, Password: $passwordValid (${passwordValue.length}), Match: $passwordsMatch")

        _isFormValid.value = nameValid && lastNameValid && emailValid && passwordValid && passwordsMatch
    }

    fun signUp() {
        val nameValue = _name.value ?: return
        val lastNameValue = _lastName.value ?: return
        val emailValue = _email.value ?: return
        val passwordValue = _password.value ?: return
        val roleValue = _role.value ?: UserRole.CHILD

        Log.d(TAG, "SignUp called - Name: $nameValue, LastName: $lastNameValue, Email: $emailValue, Role: $roleValue")

        viewModelScope.launch {
            authRepository.signUp(nameValue, lastNameValue, emailValue, passwordValue, roleValue).collect { result ->
                Log.d(TAG, "SignUp result: $result")
                _authResult.value = result
            }
        }
    }
}

