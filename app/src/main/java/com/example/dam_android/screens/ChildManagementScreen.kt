package com.example.dam_android.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToActivity: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(1) } // Child tab is selected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Child Management",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangeButton,
                    titleContentColor = Black,
                    navigationIconContentColor = Black
                )
            )
        },
        bottomBar = {
            ChildManagementBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    selectedTab = tab
                    when (tab) {
                        0 -> onNavigateToHome()
                        1 -> { /* Already on Child */ }
                        2 -> onNavigateToLocation()
                        3 -> onNavigateToActivity()
                        4 -> onNavigateToProfile()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddChild,
                containerColor = OrangeButton,
                contentColor = White,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Child",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPeach)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ChildCare,
                    contentDescription = "Child",
                    modifier = Modifier.size(100.dp),
                    tint = OrangeButton.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "No children added yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToAddChild,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OrangeButton,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .height(56.dp)
                        .fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Child",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChildManagementBottomNavigationBar(
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

