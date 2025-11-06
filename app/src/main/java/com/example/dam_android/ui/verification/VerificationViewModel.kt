package com.example.dam_android.ui.verification

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_android.data.api.ApiService
import com.example.dam_android.data.model.AuthResult
import kotlinx.coroutines.launch
import android.util.Log

/**
 * ViewModel pour la vérification du code à 6 chiffres
 */
class VerificationViewModel : ViewModel() {

    private val TAG = "VerificationViewModel"

    private val _authResult = MutableLiveData<AuthResult>()
    val authResult: LiveData<AuthResult> = _authResult

    private val _verificationCode = MutableLiveData("")
    val verificationCode: LiveData<String> = _verificationCode

    private val _isCodeValid = MutableLiveData(false)
    val isCodeValid: LiveData<Boolean> = _isCodeValid

    private val _remainingTime = MutableLiveData(60)
    val remainingTime: LiveData<Int> = _remainingTime

    fun onCodeChanged(code: String) {
        // Limiter à 6 chiffres
        val filteredCode = code.filter { it.isDigit() }.take(6)
        _verificationCode.value = filteredCode
        _isCodeValid.value = filteredCode.length == 6
        Log.d(TAG, "Code changé: $filteredCode (valide: ${filteredCode.length == 6})")
    }

    fun verifyCode(email: String, code: String) {
        if (code.length != 6) {
            _authResult.value = AuthResult.Error("Le code doit contenir 6 chiffres")
            return
        }

        Log.d(TAG, "Vérification du code pour: $email")
        _authResult.value = AuthResult.Loading

        viewModelScope.launch {
            val result = ApiService.verifyCode(email, code)

            _authResult.value = if (result.isSuccess) {
                Log.d(TAG, "✅ Vérification réussie")
                AuthResult.Success(result.getOrNull()!!)
            } else {
                val error = result.exceptionOrNull()?.message ?: "Erreur de vérification"
                Log.e(TAG, "❌ Vérification échouée: $error")
                AuthResult.Error(error)
            }
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            for (i in 60 downTo 0) {
                _remainingTime.value = i
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun resetTimer() {
        _remainingTime.value = 60
        startTimer()
    }
}

