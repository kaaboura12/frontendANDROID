
package com.example.dam_android.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.R
import com.example.dam_android.ui.theme.*
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.network.local.SessionManager
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.example.dam_android.util.GoogleSignInHelper
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateToParentHome: () -> Unit,
    onNavigateToChildHome: () -> Unit,
    onNavigateToChildQrLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accentBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFFF9553), Color(0xFFFF6A4F))
        )
    }
    val outlineBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFFFAE6D), Color(0xFFFF7C55))
        )
    }
    val buttonShape = RoundedCornerShape(28.dp)
    
    // Define functions first (before they are used) - this helps IDE find usages
    suspend fun handleSignIn() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Please fill in all fields"
            return
        }

        isLoading = true
        errorMessage = null

        try {
            // ✅ VRAI APPEL API
            val result = ApiService.loginUser(email, password)

            result.onSuccess { (user, token) ->
                // Sauvegarder le token et l'utilisateur
                val sessionManager = SessionManager.getInstance(context)
                sessionManager.saveUser(user, token)

                isLoading = false

                // Navigation selon le rôle
                when (user.roleString.lowercase()) {
                    "parent" -> onNavigateToParentHome()
                    "child", "enfant" -> onNavigateToChildHome()
                    else -> onNavigateToParentHome()
                }
            }

            result.onFailure { exception ->
                isLoading = false
                errorMessage = exception.message ?: "Sign in failed"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Sign in error: ${e.message}"
        }
    }

    suspend fun handleGoogleSignIn(idToken: String) {
        isLoading = true
        errorMessage = null

        try {
            val result = ApiService.loginWithGoogle(idToken)

            result.onSuccess { (user, token) ->
                val sessionManager = SessionManager.getInstance(context)
                sessionManager.saveUser(user, token)

                isLoading = false

                when (user.roleString.lowercase()) {
                    "parent" -> onNavigateToParentHome()
                    "child", "enfant" -> onNavigateToChildHome()
                    else -> onNavigateToParentHome()
                }
            }

            result.onFailure { exception ->
                isLoading = false
                errorMessage = exception.message ?: "Google sign-in failed"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Google sign-in error: ${e.message}"
        }
    }
    
    // Google Sign-In client
    // Web Client ID is now hardcoded in GoogleSignInHelper - will always work
    val googleSignInClient = remember {
        val appContext = context.applicationContext
        android.util.Log.d("SignInScreen", "🔧 Creating GoogleSignInClient...")
        GoogleSignInHelper.getGoogleSignInClient(appContext)
    }
    
    // Google Sign-In launcher (uses handleGoogleSignIn function defined above)
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        coroutineScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                
                account?.let { acc ->
                    val idToken = GoogleSignInHelper.getIdToken(acc)
                    
                    if (idToken != null) {
                        // We have idToken, proceed with backend authentication
                        handleGoogleSignIn(idToken)
                    } else {
                        // idToken is null - Web Client ID might not be configured
                        isLoading = false
                        errorMessage = "Google Sign-In successful but ID token is missing. Please configure Web Client ID in GoogleSignInHelper.kt"
                    }
                } ?: run {
                    errorMessage = "Failed to get Google account"
                    isLoading = false
                }
            } catch (e: ApiException) {
                isLoading = false
                when (e.statusCode) {
                    12501 -> errorMessage = "Google Sign-In was cancelled"
                    7 -> errorMessage = "Network error. Please check your internet connection"
                    10 -> {
                        // Status code 10 = DEVELOPER_ERROR
                        // This means the Web Client ID is missing, incorrect, or not properly configured
                        errorMessage = "Developer error. Please check Web Client ID configuration.\n\n" +
                                "To fix this:\n" +
                                "1. Go to Google Cloud Console\n" +
                                "2. Create a Web application OAuth 2.0 Client ID\n" +
                                "3. Add it to res/values/strings.xml as 'GOOGLE_CLIENT_ID'\n" +
                                "4. Rebuild the app"
                    }
                    else -> errorMessage = "Google Sign-In failed: ${e.message ?: "Unknown error (Code: ${e.statusCode})"}"
                }
                android.util.Log.e("SignInScreen", "Google Sign-In error: Status ${e.statusCode}, Message: ${e.message}", e)
            } catch (e: Exception) {
                isLoading = false
                errorMessage = "Error: ${e.message}"
                android.util.Log.e("SignInScreen", "Google Sign-In exception", e)
            }
        }
    }

    // Design identique au fragment_sign_in.xml
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Logo (80dp x 80dp comme dans XML)
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(84.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Sign In",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account ?",
                        fontSize = 15.sp,
                        color = Black.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(
                        onClick = onNavigateToSignUp,
                        modifier = Modifier.padding(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Sign Up",
                            fontSize = 15.sp,
                            color = Orange700,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = Color.White.copy(alpha = 0.94f),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, Color(0xFFFFE6CD))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Email :",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            placeholder = {
                                Text(
                                    text = "Enter your email",
                                    color = Black.copy(alpha = 0.35f)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                disabledContainerColor = White,
                                focusedIndicatorColor = Color(0xFFFF9553),
                                unfocusedIndicatorColor = Color(0xFFFFD6B2),
                                cursorColor = Color(0xFFFF7C55),
                                focusedTextColor = Black,
                                unfocusedTextColor = Black
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Password :",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            placeholder = {
                                Text(
                                    text = "Enter your password",
                                    color = Black.copy(alpha = 0.35f)
                                )
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = White,
                                unfocusedContainerColor = White,
                                disabledContainerColor = White,
                                focusedIndicatorColor = Color(0xFFFF9553),
                                unfocusedIndicatorColor = Color(0xFFFFD6B2),
                                cursorColor = Color(0xFFFF7C55),
                                focusedTextColor = Black,
                                unfocusedTextColor = Black
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateToForgotPassword,
                            modifier = Modifier.padding(0.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Forgot Password ?",
                                fontSize = 14.sp,
                                color = Orange700,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                handleSignIn()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = White
                        ),
                        shape = buttonShape,
                        contentPadding = PaddingValues(0.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        enabled = !isLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(buttonShape)
                                .background(accentBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Sign In",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Black.copy(alpha = 0.2f)
                        )
                        Text(
                            text = "OR",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            fontSize = 14.sp,
                            color = Black.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Black.copy(alpha = 0.2f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val signInIntent = googleSignInClient.signInIntent
                                googleSignInLauncher.launch(signInIntent)
                                isLoading = true
                                errorMessage = null
                            } catch (e: Exception) {
                                errorMessage = "Error starting Google Sign-In: ${e.message}"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = White,
                            contentColor = Color(0xFF4285F4)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(2.dp, Color(0xFF4285F4)),
                        enabled = !isLoading
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(Color(0xFF4285F4), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4285F4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = onNavigateToChildQrLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFFF7C55)
                ),
                shape = buttonShape,
                border = BorderStroke(2.dp, outlineBrush),
                enabled = !isLoading,
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan child QR code",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign in as child",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
