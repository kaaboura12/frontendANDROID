package com.example.dam_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.ui.theme.*
import com.example.dam_android.util.LocalStorage

data class Child(
    val id: String,
    val name: String,
    val location: String,
    val status: String,
    val isActive: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToChild: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToActivity: () -> Unit
) {
    val storage = LocalStorage.getInstance(androidx.compose.ui.platform.LocalContext.current)
    val currentUser = storage.getUser()

    // Données de démonstration
    val children = remember {
        listOf(
            Child("1", "Chaima benty", "tunis,centre", "Just now", true),
            Child("2", "Chaima benty", "tunis,centre", "Just now", true),
            Child("3", "Chaima benty", "tunis,centre", "Just now", true)
        )
    }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            // TopBar personnalisée avec fond orange
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

                    // Icône Shield/Sécurité
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Security",
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        0 -> { /* Already on Home */ }
                        1 -> onNavigateToChild()
                        2 -> onNavigateToLocation()
                        3 -> onNavigateToActivity()
                        4 -> onNavigateToProfile()
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
                // Carte de bienvenue
                item {
                    WelcomeCard(userName = currentUser?.name ?: "mohamed amin")
                }

                // Titre "Ur childs :"
                item {
                    Text(
                        text = "Ur childs :",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Liste des enfants
                items(children) { child ->
                    ChildCard(child = child)
                }
            }
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
                text = "\"bonjour mr $userName\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
        }
    }
}

@Composable
private fun ChildCard(child: Child) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar de l'enfant
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Child",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Informations de l'enfant
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = child.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )

                    // Indicateur de statut (point vert)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = child.location,
                        fontSize = 12.sp,
                        color = Black.copy(alpha = 0.6f)
                    )

                    Text(
                        text = child.status,
                        fontSize = 12.sp,
                        color = Black.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { /* TODO: Check profile */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeButton,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "check profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { /* TODO: Check in map */ },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OrangeButton,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "check in map",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
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
                    Icons.Default.ChildCare,
                    contentDescription = "Child",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Child", fontSize = 12.sp) },
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
                    Icons.Default.Timeline,
                    contentDescription = "Activity",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Activity", fontSize = 12.sp) },
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

        NavigationBarItem(
            icon = {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("profile", fontSize = 12.sp) },
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
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