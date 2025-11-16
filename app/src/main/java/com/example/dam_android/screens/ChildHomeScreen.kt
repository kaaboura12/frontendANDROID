package com.example.dam_android.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.ui.theme.*
import com.example.dam_android.util.LocalStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildHomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val storage = LocalStorage.getInstance(context)
    val currentUser = storage.getUser()
    
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            // TopBar personnalisée avec fond orange (matching parent screen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OrangeButton)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Homepage",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )

                    // Logout button
                    IconButton(onClick = {
                        storage.logout()
                        onLogout()
                    }) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            ChildBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        0 -> { /* Already on Home */ }
                        1 -> onNavigateToChat()
                        2 -> onNavigateToLocation()
                        3 -> onNavigateToProfile()
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPeach)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Carte de bienvenue (matching parent screen)
                item {
                    WelcomeCard(userName = currentUser?.name ?: "Enfant")
                }

                // SOS Button - Prominent Emergency Button
                item {
                    SOSButton(onClick = {
                        // Static for now - just show a toast
                        Toast.makeText(
                            context,
                            "🚨 SOS Activated! (Static for now)",
                            Toast.LENGTH_SHORT
                        ).show()
                    })
                }

                // Mes Jeux
                item {
                    ChildFeatureCard(
                        icon = Icons.Default.Games,
                        title = "🎮 Mes Jeux",
                        description = "Joue et apprends!",
                        onClick = { /* TODO: Navigation vers jeux */ }
                    )
                }

                // Mes Devoirs
                item {
                    ChildFeatureCard(
                        icon = Icons.Default.MenuBook,
                        title = "📚 Mes Devoirs",
                        description = "Vérifie tes devoirs",
                        onClick = { /* TODO: Navigation vers devoirs */ }
                    )
                }

                // Mes Récompenses
                item {
                    ChildFeatureCard(
                        icon = Icons.Default.Star,
                        title = "⭐ Mes Récompenses",
                        description = "Vois tes réalisations",
                        onClick = { /* TODO: Navigation vers récompenses */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SOSButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE53935)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Large SOS icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(White.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "SOS",
                    tint = White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // SOS Text
            Text(
                text = "SOS",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = White,
                letterSpacing = 4.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Emergency message
            Text(
                text = "Emergency Button",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Press in case of emergency",
                fontSize = 12.sp,
                color = White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WelcomeCard(userName: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(OrangeButton, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Texte de bienvenue
            Text(
                text = "Salut, $userName! 👋",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
        }
    }
}

@Composable
private fun ChildFeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(OrangeButton.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = OrangeButton,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ChildBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = White,
        contentColor = Black,
        modifier = Modifier.height(70.dp)
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Home", fontSize = 12.sp) },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeButton,
                selectedTextColor = OrangeButton,
                unselectedIconColor = Black.copy(alpha = 0.6f),
                unselectedTextColor = Black.copy(alpha = 0.6f),
                indicatorColor = OrangeButton.copy(alpha = 0.1f)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "Chat",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Chat", fontSize = 12.sp) },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeButton,
                selectedTextColor = OrangeButton,
                unselectedIconColor = Black.copy(alpha = 0.6f),
                unselectedTextColor = Black.copy(alpha = 0.6f),
                indicatorColor = OrangeButton.copy(alpha = 0.1f)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Location",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Location", fontSize = 12.sp) },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeButton,
                selectedTextColor = OrangeButton,
                unselectedIconColor = Black.copy(alpha = 0.6f),
                unselectedTextColor = Black.copy(alpha = 0.6f),
                indicatorColor = OrangeButton.copy(alpha = 0.1f)
            )
        )

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("profile", fontSize = 12.sp) },
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = OrangeButton,
                selectedTextColor = OrangeButton,
                unselectedIconColor = Black.copy(alpha = 0.6f),
                unselectedTextColor = Black.copy(alpha = 0.6f),
                indicatorColor = OrangeButton.copy(alpha = 0.1f)
            )
        )
    }
}
