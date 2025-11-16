package com.example.dam_android.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_android.R
import com.example.dam_android.models.ChatRoomDetail
import com.example.dam_android.models.UserRole
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.network.local.SessionManager
import kotlinx.coroutines.launch

@Composable
fun ChildChatScreen(
    onNavigateBack: () -> Unit,
    onOpenRoom: (roomId: String, childName: String?) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager.getInstance(context) }
    val childUser = remember { sessionManager.getUser() }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var room by remember { mutableStateOf<ChatRoomDetail?>(null) }
    var hasNavigated by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF6ED), Color(0xFFFFE2C6))
        )
    }

    fun loadRoom() {
        val childId = childUser?.id
        if (childId.isNullOrBlank()) {
            errorMessage = "Session enfant introuvable."
            isLoading = false
            return
        }
        if (childUser.role != UserRole.CHILD) {
            errorMessage = "Cet espace est réservé aux enfants."
            isLoading = false
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null

            val result = ApiService.getChildChatRoom(childId)
            result.onSuccess {
                room = it
                errorMessage = null
            }.onFailure { throwable ->
                errorMessage = throwable.message ?: "Impossible de récupérer la conversation."
                room = null
            }

            isLoading = false
        }
    }

    LaunchedEffect(childUser?.id) {
        loadRoom()
    }

    LaunchedEffect(room?.id, hasNavigated) {
        val roomId = room?.id
        if (!roomId.isNullOrBlank() && !hasNavigated) {
            hasNavigated = true
            onOpenRoom(roomId, room?.childName)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.25f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(
                onNavigateBack = onNavigateBack
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = Color.White.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    when {
                        isLoading -> LoadingState()
                        errorMessage != null -> ErrorState(
                            message = errorMessage ?: "",
                            onRetry = { loadRoom() }
                        )

                        room != null -> SuccessState(
                            room = room!!,
                            onOpenConversation = {
                                room?.id?.let { id ->
                                    onOpenRoom(id, room?.childName)
                                }
                            }
                        )
                        else -> {
                            ErrorState(
                                message = "Conversation introuvable.",
                                onRetry = { loadRoom() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = Color(0xFFEA784D)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Messages",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF442B1B)
            )
            Text(
                text = "Discute avec tes proches",
                fontSize = 12.sp,
                color = Color(0xFF8C6F5B)
            )
        }

        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = Color(0xFFFF7C55))
        Text(
            text = "Chargement de ta conversation...",
            color = Color(0xFF8C6F5B),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFB33A32),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = message,
            color = Color(0xFFB33A32),
            fontSize = 14.sp
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7C55),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(text = "Réessayer")
        }
    }
}

@Composable
private fun SuccessState(
    room: ChatRoomDetail,
    onOpenConversation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Chat,
            contentDescription = null,
            tint = Color(0xFFFF7C55),
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Ta conversation est prête !",
            color = Color(0xFF442B1B),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Appuie sur le bouton pour discuter avec ta famille.",
            color = Color(0xFF8C6F5B),
            fontSize = 14.sp
        )

        Button(
            onClick = onOpenConversation,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7C55),
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text(text = "Ouvrir la conversation")
        }
    }
}


