package com.example.dam_android.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.models.ChildModel
import com.example.dam_android.network.api.RetrofitClient
import com.example.dam_android.ui.theme.*
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddChild: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToQRCode: (String, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(1) } // Child tab is selected
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // State for children list
    var children by remember { mutableStateOf<List<ChildModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Fetch children on screen load
    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            Log.d("ChildManagementScreen", "🔄 Fetching children from API...")
            val response = RetrofitClient.childApi.getChildren()
            Log.d("ChildManagementScreen", "📥 Response received: code=${response.code()}, success=${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    children = responseBody
                    Log.d("ChildManagementScreen", "✅ Successfully loaded ${children.size} children")
                    children.forEachIndexed { index, child ->
                        Log.d("ChildManagementScreen", "   Child $index: ${child.firstName} ${child.lastName} (ID: ${child._id}, Parent: ${child.parent})")
                    }
                } else {
                    errorMessage = "No children data received"
                    Log.e("ChildManagementScreen", "❌ Response body is null")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                errorMessage = "Failed to load children (${response.code()})${if (errorBody != null) ": $errorBody" else ""}"
                Log.e("ChildManagementScreen", "❌ Error response: code=${response.code()}, body=$errorBody")
            }
        } catch (e: JsonSyntaxException) {
            errorMessage = "Data parsing error: ${e.message}"
            Log.e("ChildManagementScreen", "❌ JSON parsing error", e)
            e.printStackTrace()
        } catch (e: Exception) {
            errorMessage = "Error loading children: ${e.message ?: "Unknown error"}"
            Log.e("ChildManagementScreen", "❌ Exception loading children", e)
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }
    
    // Refresh function to reload children
    fun refreshChildren() {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                Log.d("ChildManagementScreen", "🔄 Refreshing children list...")
                val response = RetrofitClient.childApi.getChildren()
                Log.d("ChildManagementScreen", "📥 Refresh response: code=${response.code()}, success=${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        children = responseBody
                        Log.d("ChildManagementScreen", "✅ Successfully refreshed: ${children.size} children")
                    } else {
                        errorMessage = "No children data received"
                        Log.e("ChildManagementScreen", "❌ Refresh: Response body is null")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    errorMessage = "Failed to load children (${response.code()})${if (errorBody != null) ": $errorBody" else ""}"
                    Log.e("ChildManagementScreen", "❌ Refresh error: code=${response.code()}, body=$errorBody")
                }
            } catch (e: JsonSyntaxException) {
                errorMessage = "Data parsing error: ${e.message}"
                Log.e("ChildManagementScreen", "❌ Refresh: JSON parsing error", e)
                e.printStackTrace()
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message ?: "Unknown error"}"
                Log.e("ChildManagementScreen", "❌ Refresh: Exception", e)
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

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
                actions = {
                    IconButton(
                        onClick = { refreshChildren() },
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = if (isLoading) Black.copy(alpha = 0.3f) else Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OrangeButton,
                    titleContentColor = Black,
                    navigationIconContentColor = Black,
                    actionIconContentColor = Black
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
                        3 -> onNavigateToChat()
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
            when {
                isLoading -> {
                    // Loading state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = OrangeButton,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                errorMessage != null -> {
                    // Error state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "An error occurred",
                            fontSize = 16.sp,
                            color = Black.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { refreshChildren() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OrangeButton,
                                contentColor = White
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Retry", fontSize = 16.sp)
                        }
                    }
                }
                children.isEmpty() -> {
                    // Empty state
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
                else -> {
                    // Children list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(children) { child ->
                            ChildCard(
                                child = child,
                                onViewQRCode = {
                                    if (child.qrCode != null && child.qrCode.isNotBlank()) {
                                        val childName = "${child.firstName} ${child.lastName}"
                                        onNavigateToQRCode(child.qrCode, childName)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "QR code not available for this child",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChildCard(
    child: ChildModel,
    onViewQRCode: () -> Unit
) {
    val childName = "${child.firstName} ${child.lastName}"
    val deviceType = child.deviceType
    val isOnline = child.isOnline
    val status = child.status
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side: Child info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar/Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(OrangeButton.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChildCare,
                        contentDescription = "Child",
                        tint = OrangeButton,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Child details
                Column {
                    Text(
                        text = childName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Device type badge
                        Surface(
                            color = if (deviceType == "PHONE") 
                                OrangeButton.copy(alpha = 0.1f) 
                            else 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = deviceType,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (deviceType == "PHONE") OrangeButton else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // Online status
                        if (isOnline) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                )
                                Text(
                                    text = "Online",
                                    fontSize = 12.sp,
                                    color = Black.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Right side: QR Code button
            IconButton(
                onClick = onViewQRCode,
                modifier = Modifier
                    .size(48.dp)
                    .background(OrangeButton.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.QrCode2,
                    contentDescription = "View QR Code",
                    tint = OrangeButton,
                    modifier = Modifier.size(24.dp)
                )
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
                    Icons.Default.Chat,
                    contentDescription = "Chat",
                    modifier = Modifier.size(24.dp)
                )
            },
            label = { Text("Chat", fontSize = 12.sp) },
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

