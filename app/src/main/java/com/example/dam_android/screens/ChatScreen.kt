package com.example.dam_android.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.R
import com.example.dam_android.models.ParentChatRoom
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.network.local.SessionManager
import com.example.dam_android.ui.theme.Black
import com.example.dam_android.ui.theme.White
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onOpenRoom: (ParentChatRoom) -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val parent = remember { sessionManager.getUser() }

    var rooms by remember { mutableStateOf<List<ParentChatRoom>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showUnreadOnly by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val filteredRooms by remember(rooms, searchQuery, showUnreadOnly) {
        derivedStateOf {
            rooms.filter { room ->
                val matchesSearch = searchQuery.isBlank() ||
                        room.childName.contains(searchQuery, ignoreCase = true) ||
                        room.lastMessagePreview.contains(searchQuery, ignoreCase = true)
                val matchesUnread = !showUnreadOnly || room.unreadCount > 0
                matchesSearch && matchesUnread
            }
        }
    }

    val totalUnread by remember(rooms) {
        derivedStateOf { rooms.sumOf { it.unreadCount.coerceAtLeast(0) } }
    }

    val outlineBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFFFFAE6D), Color(0xFFFF7C55))
        )
    }

    suspend fun loadRooms() {
        val parentId = parent?.id
        if (parentId.isNullOrBlank()) {
            errorMessage = "Votre session parent est introuvable."
            rooms = emptyList()
            isLoading = false
            return
        }
        isLoading = true
        errorMessage = null
        val result = ApiService.getParentChatRooms(parentId)
        result.onSuccess {
            rooms = it.sortedByDescending { room -> room.lastMessageTimestamp.orEmpty() }
            errorMessage = null
        }.onFailure { throwable ->
            errorMessage = throwable.message ?: "Impossible de récupérer les conversations."
            rooms = emptyList()
        }
        isLoading = false
    }

    LaunchedEffect(parent?.id) {
        loadRooms()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF6ED))
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            HeaderSection(
                parentName = parent?.let { "${it.name} ${it.lastName}".trim() } ?: "",
                totalUnread = totalUnread,
                onBack = onNavigateBack
            )

            SearchAndFilterRow(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it },
                showUnreadOnly = showUnreadOnly,
                onToggleUnread = { showUnreadOnly = !showUnreadOnly },
                onClearSearch = { searchQuery = "" },
                outlineBrush = outlineBrush
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                when {
                    isLoading -> LoadingState()
                    errorMessage != null -> ErrorState(
                        message = errorMessage ?: "",
                        onRetry = { scope.launch { loadRooms() } }
                    )

                    rooms.isEmpty() -> EmptyState(
                        title = "Aucune conversation pour le moment.",
                        subtitle = "Dès qu'un échange commence avec un enfant, il apparaîtra ici."
                    )

                    filteredRooms.isEmpty() -> EmptyState(
                        title = "Aucun résultat",
                        subtitle = "Aucune conversation ne correspond à \"$searchQuery\""
                    )

                    else -> ConversationsList(
                        rooms = filteredRooms,
                        onOpenRoom = onOpenRoom
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    parentName: String,
    totalUnread: Int,
    onBack: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color(0xFFFFC9A2)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.7f),
                contentColor = Color(0xFFEA784D)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(22.dp)
            )
        }

        AnimatedVisibility(visible = parentName.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.85f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFFF7C55),
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = parentName,
                            color = Color(0xFF955B36),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (totalUnread > 0) {
                            Text(
                                text = "$totalUnread messages non lus",
                                color = Color(0xFFB07253),
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = "Toutes vos conversations sont à jour",
                                color = Color(0xFFB07253),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterRow(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    showUnreadOnly: Boolean,
    onToggleUnread: () -> Unit,
    onClearSearch: () -> Unit,
    outlineBrush: Brush
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            placeholder = { Text("Rechercher une conversation") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFFB99C8A)
                )
            },
            trailingIcon = {
                AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                    Text(
                        text = "Effacer",
                        color = Color(0xFFFF7C55),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable(onClick = onClearSearch),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.8f),
                focusedIndicatorColor = Color(0xFFFF9553),
                unfocusedIndicatorColor = Color(0xFFFFD6B2),
                cursorColor = Color(0xFFFF7C55),
                focusedTextColor = Black,
                unfocusedTextColor = Black
            ),
            shape = RoundedCornerShape(20.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !showUnreadOnly,
                onClick = {
                    if (showUnreadOnly) onToggleUnread()
                },
                label = { Text("Tous") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.85f),
                    selectedContainerColor = Color.White,
                    labelColor = Color(0xFF8C6F5B),
                    selectedLabelColor = Black
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (!showUnreadOnly) Color.Transparent else Color(0xFFFFD6B2)
                )
            )

            FilterChip(
                selected = showUnreadOnly,
                onClick = onToggleUnread,
                label = { Text("Non lus") },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.85f),
                    selectedContainerColor = Color.Transparent,
                    labelColor = Color(0xFF8C6F5B),
                    selectedLabelColor = Color.White
                ),
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(outlineBrush)
                    )
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (showUnreadOnly) Color.Transparent else Color(0xFFFFD6B2)
                )
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFFF7C55))
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0xFFEA784D),
            modifier = Modifier.size(42.dp)
        )
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = Color(0xFFB25F39),
            fontSize = 15.sp
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF8C61),
                contentColor = White
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Réessayer")
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = Color(0xFFEA784D),
            modifier = Modifier.size(42.dp)
        )
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            textAlign = TextAlign.Center,
            color = Color(0xFF8C6F5B)
        )
    }
}

@Composable
private fun ConversationsList(
    rooms: List<ParentChatRoom>,
    onOpenRoom: (ParentChatRoom) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(rooms, key = { it.roomId }) { room ->
            ConversationCard(
                room = room,
                onClick = { onOpenRoom(room) }
            )
        }
    }
}

@Composable
private fun ConversationCard(
    room: ParentChatRoom,
    onClick: () -> Unit
) {
    val unreadBrush = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFFAE6D), Color(0xFFFF7C55))
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF0E3)),
                contentAlignment = Alignment.Center
            ) {
                val initials = room.childName
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifBlank { "?" }

                Text(
                    text = initials,
                    color = Color(0xFFEA784D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = room.childName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Black
                    )
                    val formatted = formatTimestamp(room.lastMessageTimestamp)
                    if (formatted.isNotEmpty()) {
                        Text(
                            text = formatted,
                            fontSize = 12.sp,
                            color = Color(0xFFB19B8D)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = room.lastMessagePreview,
                    fontSize = 14.sp,
                    color = Color(0xFF8C6F5B),
                    maxLines = 2
                )
            }

            if (room.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(unreadBrush)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = room.unreadCount.toString(),
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
        return isoString.substringBefore("T").ifBlank { "" }
    }
    return try {
        val instant = Instant.parse(isoString)
        val zone = instant.atZone(ZoneId.systemDefault())
        val today = LocalDate.now()
        val date = zone.toLocalDate()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
        val dateFormatterSameYear = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        val dateFormatterOtherYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

        when {
            date == today -> zone.format(timeFormatter)
            date == today.minusDays(1) -> "Hier"
            date.year == today.year -> zone.format(dateFormatterSameYear)
            else -> zone.format(dateFormatterOtherYear)
        }
    } catch (e: Exception) {
        isoString.substringBefore("T").ifBlank { "" }
    }
}

