package com.example.dam_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.network.local.SessionManager
import com.example.dam_android.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VerificationScreen(
    email: String,
    onNavigateToParentHome: () -> Unit,
    onNavigateToChildHome: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var otpValues by remember { mutableStateOf(List(6) { "" }) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableStateOf(55) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Countdown timer
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    suspend fun handleVerification() {
        val code = otpValues.joinToString("")

        if (code.length != 6) {
            errorMessage = "Le code doit contenir 6 chiffres"
            return
        }

        isLoading = true
        errorMessage = null

        try {
            val result = ApiService.verifyCode(email, code)

            result.onSuccess { user ->
                isLoading = false

                // Sauvegarder l'utilisateur vérifié
                val sessionManager = SessionManager.getInstance(context)
                sessionManager.saveUser(user)

                // Navigation selon le rôle
                when (user.roleString.lowercase()) {
                    "parent" -> onNavigateToParentHome()
                    "child", "enfant" -> onNavigateToChildHome()
                    else -> onNavigateToParentHome()
                }
            }

            result.onFailure { exception ->
                isLoading = false
                errorMessage = exception.message ?: "Code invalide ou expiré"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Erreur de vérification: ${e.message}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientEnd)
                )
            )
    ) {
        // Top decorative wave (280dp x 280dp, position top-end)
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 60.dp, y = (-100).dp)
                .align(Alignment.TopEnd)
                .background(BgPeach, CircleShape)
                .alpha(0.5f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Bouton retour
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Black
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Icône Lock dans un cercle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally)
                    .background(Orange700.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = Orange700,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Titre
            Text(
                text = "Verification Code",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sous-titre avec email
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "We've sent a 6-digit code to",
                    fontSize = 14.sp,
                    color = Black.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = email,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Black,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Cases OTP (6 cases)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                otpValues.forEachIndexed { index, value ->
                    OtpInputBox(
                        value = value,
                        onValueChange = { newValue ->
                            if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
                                otpValues = otpValues.toMutableList().apply {
                                    this[index] = newValue
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Texte "Didn't receive the code?"
            Text(
                text = "Didn't receive the code? (${countdown}s)",
                fontSize = 14.sp,
                color = Black.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Message d'erreur
            errorMessage?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bouton "Verify"
            Button(
                onClick = {
                    coroutineScope.launch {
                        handleVerification()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeButton,
                    contentColor = White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Verify",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OtpInputBox(
    value: String,
    onValueChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(
                width = 1.5.dp,
                color = if (value.isNotEmpty()) OrangeButton else Color(0xFFE0E0E0),
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Black,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}