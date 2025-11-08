package com.example.dam_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.ui.theme.*
import com.example.dam_android.util.LocalStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit
) {
    val storage = LocalStorage.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val currentUser = storage.getUser()

    var firstName by remember { mutableStateOf(currentUser?.name ?: "") }
    var lastName by remember { mutableStateOf(currentUser?.lastName ?: "") }
    var phoneNumber by remember { mutableStateOf(currentUser?.phoneNumber ?: "") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifier le profil", color = White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangeButton
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    )
                )
                .padding(paddingValues)
        ) {
            // Decorative element
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
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(OrangeButton, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentUser?.email ?: "",
                    fontSize = 14.sp,
                    color = Black.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Messages d'erreur ou succès
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                successMessage?.let { success ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = OrangeButton.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = success,
                            color = OrangeButton,
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Prénom
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Prénom") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton,
                        focusedLeadingIconColor = OrangeButton,
                        cursorColor = OrangeButton
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Nom
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Nom") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton,
                        focusedLeadingIconColor = OrangeButton,
                        cursorColor = OrangeButton
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Numéro de téléphone
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Numéro de téléphone") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton,
                        focusedLeadingIconColor = OrangeButton,
                        cursorColor = OrangeButton
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Changer le mot de passe (optionnel)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Nouveau mot de passe
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Nouveau mot de passe") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "Masquer" else "Afficher"
                            )
                        }
                    },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton,
                        focusedLeadingIconColor = OrangeButton,
                        cursorColor = OrangeButton
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirmer mot de passe
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmer le mot de passe") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                            Icon(
                                if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showConfirmPassword) "Masquer" else "Afficher"
                            )
                        }
                    },
                    visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OrangeButton,
                        focusedLabelColor = OrangeButton,
                        focusedLeadingIconColor = OrangeButton,
                        cursorColor = OrangeButton
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Bouton Sauvegarder
                Button(
                    onClick = {
                        // Validation
                        errorMessage = null
                        successMessage = null

                        if (firstName.isBlank() || lastName.isBlank()) {
                            errorMessage = "Le prénom et le nom sont requis"
                            return@Button
                        }

                        if (password.isNotEmpty() && password != confirmPassword) {
                            errorMessage = "Les mots de passe ne correspondent pas"
                            return@Button
                        }

                        if (password.isNotEmpty() && password.length < 6) {
                            errorMessage = "Le mot de passe doit contenir au moins 6 caractères"
                            return@Button
                        }

                        // Appel API
                        isLoading = true
                        scope.launch {
                            try {
                                val userId = currentUser?.id ?: throw Exception("Utilisateur non connecté")

                                val result = ApiService.updateUser(
                                    userId = userId,
                                    firstName = firstName,
                                    lastName = lastName,
                                    phoneNumber = if (phoneNumber.isNotBlank()) phoneNumber else null,
                                    password = if (password.isNotEmpty()) password else null
                                )

                                result.fold(
                                    onSuccess = { updatedUser ->
                                        // Mettre à jour le storage local
                                        storage.saveUser(updatedUser)
                                        successMessage = "Profil mis à jour avec succès!"

                                        // Réinitialiser les champs de mot de passe
                                        password = ""
                                        confirmPassword = ""

                                        // Retour à la page de profil après 1.5 secondes
                                        kotlinx.coroutines.delay(1500)
                                        onProfileUpdated()
                                    },
                                    onFailure = { exception ->
                                        errorMessage = exception.message ?: "Erreur lors de la mise à jour"
                                    }
                                )
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Erreur inattendue"
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeButton,
                        contentColor = White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = "Sauvegarder",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton Annuler
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OrangeButton
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Annuler",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

