package com.example.dam_android.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dam_android.util.LocalStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildHomeScreen(
    onNavigateToProfile: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val storage = LocalStorage.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val currentUser = storage.getUser()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accueil Enfant") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Text(
                text = "Salut, ${currentUser?.name ?: "Enfant"}! 👋",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigation vers jeux */ }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎮 Mes Jeux",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Joue et apprends!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigation vers devoirs */ }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📚 Mes Devoirs",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Vérifie tes devoirs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO: Navigation vers récompenses */ }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⭐ Mes Récompenses",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Vois tes réalisations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

