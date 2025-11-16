package com.example.dam_android.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.R
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.ui.theme.Black
import com.example.dam_android.ui.theme.Orange700
import com.example.dam_android.ui.theme.White
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToVerification: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
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

    val isFormValid = name.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            password == confirmPassword

    suspend fun handleSignUp() {
        if (!isFormValid) {
            errorMessage = "Please complete all fields correctly"
            return
        }

        isLoading = true
        errorMessage = null

        try {
            val result = ApiService.registerUser(
                firstName = name,
                lastName = lastName,
                email = email,
                password = password,
                role = "PARENT"
            )

            result.onSuccess { user ->
                isLoading = false
                onNavigateToVerification(email)
            }

            result.onFailure { exception ->
                isLoading = false
                errorMessage = exception.message ?: "Registration failed"
            }
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Error: ${e.message}"
        }
    }

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
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

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
                    text = "Create Account",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                color = Black
            )

            Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Already have an account ?",
                        fontSize = 15.sp,
                        color = Black.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
            TextButton(
                onClick = onNavigateToSignIn,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                            text = "Sign In",
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
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "First Name :",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            placeholder = {
                                Text(
                                    text = "Enter your first name",
                                    color = Black.copy(alpha = 0.35f)
                                )
                            },
                singleLine = true,
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

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Last Name :",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            placeholder = {
                                Text(
                                    text = "Enter your last name",
                                    color = Black.copy(alpha = 0.35f)
                                )
                            },
                singleLine = true,
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

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Confirm Password :",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Black
                        )
                        OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            placeholder = {
                                Text(
                                    text = "Confirm your password",
                                    color = Black.copy(alpha = 0.35f)
                                )
                            },
                singleLine = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
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

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
                    }

            Button(
                onClick = {
                    coroutineScope.launch {
                        handleSignUp()
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
                enabled = isFormValid && !isLoading
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
                                    text = "Sign Up",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onNavigateToSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFFF7C55)
                ),
                shape = buttonShape,
                border = BorderStroke(2.dp, outlineBrush),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                Text(
                    text = "Back to Sign In",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "By signing up you agree to our Terms & Privacy Policy",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}