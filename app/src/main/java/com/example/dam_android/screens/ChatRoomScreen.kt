package com.example.dam_android.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.core.content.ContextCompat
import com.example.dam_android.R
import com.example.dam_android.models.CallState
import com.example.dam_android.models.ChatMessage
import com.example.dam_android.models.ChatRoomDetail
import com.example.dam_android.models.UserRole
import com.example.dam_android.network.api.ApiService
import com.example.dam_android.network.local.SessionManager
import com.example.dam_android.network.socket.ChatSocketManager
import com.example.dam_android.network.socket.WebRTCSignalingManager
import com.example.dam_android.ui.theme.Black
import com.example.dam_android.ui.theme.White
import com.example.dam_android.webrtc.WebRTCAudioCallManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val CHAT_ROOM_TAG = "ChatRoomScreen"
@Composable
fun ChatRoomScreen(
    roomId: String,
    childNameHint: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val audioContext = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.createAttributionContext("audio_recording")
        } else {
            context
        }
    }
    val sessionManager = remember { SessionManager.getInstance(context) }
    ChatSocketManager.initialize(sessionManager)
    val currentUser = sessionManager.getUser()
    val resolvedSenderModel = remember(currentUser?.role) {
        if (currentUser?.role == UserRole.CHILD) "Child" else "User"
    }

    // WebRTC setup
    val webrtcManager = remember(context) { WebRTCAudioCallManager(context) }
    val callState by webrtcManager.callState.collectAsState()
    val isMutedCall by webrtcManager.isMuted.collectAsState()
    val isSpeakerOn by webrtcManager.isSpeakerOn.collectAsState()

    // Initialize WebRTC signaling (uses existing chat socket)
    LaunchedEffect(currentUser) {
        currentUser?.id?.let { userId ->
            WebRTCSignalingManager.initialize(userId)
        }
    }

    var roomDetail by remember { mutableStateOf<ChatRoomDetail?>(null) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var messageInput by rememberSaveable { mutableStateOf("") }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val backgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFFFF6ED), Color(0xFFFFE2C6))
        )
    }

    val connectionState by ChatSocketManager.connectionState.collectAsState(initial = false)

    var isRecording by remember { mutableStateOf(false) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingStartTimestamp by remember { mutableStateOf<Long?>(null) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(audioContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasAudioPermission = granted
    }

    // Audio call permissions (RECORD_AUDIO + BLUETOOTH_CONNECT for Android 12+)
    val callPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(CHAT_ROOM_TAG, "✅ All call permissions granted")
        } else {
            Log.w(CHAT_ROOM_TAG, "⚠️ Some call permissions denied: $permissions")
        }
    }

    var currentlyPlayingMessageId by remember { mutableStateOf<String?>(null) }
    var isAudioLoading by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeTempAudioFile by remember { mutableStateOf<File?>(null) }

    suspend fun refreshRoom() {
        isLoading = true
        errorMessage = null
        val roomResult = ApiService.getChatRoom(roomId)
        val messagesResult = ApiService.getChatMessages(roomId)
        roomResult.onSuccess { roomDetail = it }
            .onFailure { errorMessage = it.message }
        messagesResult.onSuccess { fetched ->
            messages = fetched.sortedBy { it.createdAt }
        }.onFailure {
            errorMessage = it.message
        }
        isLoading = false
    }

    suspend fun sendMessage() {
        val userId = currentUser?.id
        if (messageInput.isBlank() || userId.isNullOrBlank()) return
        val senderModel = resolvedSenderModel
        isSending = true
        val trimmed = messageInput.trim()
        val socketResult = ChatSocketManager.sendTextMessage(
            roomId = roomId,
            text = trimmed,
            senderModel = senderModel,
            senderId = userId
        )
        if (socketResult.isSuccess) {
            messageInput = ""
            isSending = false
            return
        }
        val restResult = ApiService.sendChatTextMessage(
            roomId = roomId,
            text = trimmed,
            senderModel = senderModel,
            senderId = userId
        )
        restResult.onFailure {
            errorMessage = it.message
        }.onSuccess { sent ->
            messages = (messages + sent).sortedBy { it.createdAt }
            messageInput = ""
        }
        isSending = false
    }

    fun stopAudioPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.e(CHAT_ROOM_TAG, "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
            activeTempAudioFile?.let { file ->
                try {
                    if (file.exists()) {
                        file.delete()
                    }
                    Unit
                } catch (e: Exception) {
                    Log.e(CHAT_ROOM_TAG, "Error deleting temp file", e)
                }
            }
            activeTempAudioFile = null
            currentlyPlayingMessageId = null
            isAudioLoading = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    suspend fun startAudioPlayback(message: ChatMessage) {
        val audio = message.audio ?: return
        val source = audio.url ?: return
        stopAudioPlayback()
        isAudioLoading = true
        try {
            val playerInstance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaPlayer(audioContext)
            } else {
                MediaPlayer()
            }
            val player = playerInstance.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(1f, 1f)
            }
            player.setOnCompletionListener {
                stopAudioPlayback()
            }
            player.setOnErrorListener { _, what, extra ->
                val errorMsg = when (what) {
                    MediaPlayer.MEDIA_ERROR_UNKNOWN -> "MEDIA_ERROR_UNKNOWN"
                    MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "MEDIA_ERROR_SERVER_DIED"
                    else -> "Error code: $what"
                }
                val extraMsg = when (extra) {
                    MediaPlayer.MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"
                    MediaPlayer.MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"
                    MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"
                    MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"
                    else -> "Extra: $extra"
                }
                Log.e(CHAT_ROOM_TAG, "MediaPlayer error - $errorMsg / $extraMsg")
                scope.launch {
                    stopAudioPlayback()
                    errorMessage = "Erreur audio: $errorMsg"
                }
                true
            }
            mediaPlayer = player
            if (source.startsWith("data:")) {
                val mimeTypePart = source.substringAfter("data:", "").substringBefore(";", "")
                val base64Data = source.substringAfter(",", "")
                if (base64Data.isEmpty()) {
                    stopAudioPlayback()
                    errorMessage = "Audio non disponible"
                    return
                }
                val tempFile = withContext(Dispatchers.IO) {
                    val bytes = Base64.decode(base64Data, Base64.DEFAULT or Base64.NO_WRAP)
                        val extension = when {
                        mimeTypePart.contains("mp4") || mimeTypePart.contains("m4a") -> ".m4a"
                        mimeTypePart.contains("mpeg") || mimeTypePart.contains("mp3") -> ".mp3"
                        mimeTypePart.contains("ogg") -> ".ogg"
                        mimeTypePart.contains("wav") -> ".wav"
                        else -> ".m4a"
                    }
                        File.createTempFile("voice_${message.id}_", extension, audioContext.cacheDir).also { file ->
                        FileOutputStream(file).use { out ->
                            out.write(bytes)
                            out.flush()
                        }
                    }
                }
                activeTempAudioFile = tempFile
                player.setDataSource(tempFile.absolutePath)
                player.setOnPreparedListener {
                    currentlyPlayingMessageId = message.id
                    isAudioLoading = false
                    it.start()
                }
                player.prepareAsync()
            } else {
                if (!source.startsWith("http://") && !source.startsWith("https://")) {
                    stopAudioPlayback()
                    errorMessage = "URL audio invalide"
                    return
                }
                withContext(Dispatchers.IO) {
                    try {
                        val connection = java.net.URL(source).openConnection() as java.net.HttpURLConnection
                        connection.requestMethod = "HEAD"
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        val responseCode = connection.responseCode
                        connection.disconnect()
                        if (responseCode != 200) {
                            throw IllegalStateException("Audio non disponible (HTTP $responseCode)")
                        }
                        player.setDataSource(source)
                    } catch (e: Exception) {
                        Log.e(CHAT_ROOM_TAG, "Error checking audio URL", e)
                        withContext(Dispatchers.Main) {
                            stopAudioPlayback()
                            errorMessage = e.message
                        }
                        return@withContext
                    }
                }
                player.setOnPreparedListener {
                    currentlyPlayingMessageId = message.id
                    isAudioLoading = false
                    it.start()
                }
                player.prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(CHAT_ROOM_TAG, "Audio playback failed", e)
            stopAudioPlayback()
            errorMessage = "Erreur: ${e.message}"
        }
    }

    fun toggleAudioPlayback(message: ChatMessage) {
        scope.launch {
            if (currentlyPlayingMessageId == message.id && !isAudioLoading) {
                stopAudioPlayback()
            } else {
                startAudioPlayback(message)
            }
        }
    }

    fun startRecording() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        try {
            val file = File.createTempFile("voice_${System.currentTimeMillis()}", ".m4a", audioContext.cacheDir)
            val recorderInstance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(audioContext)
            } else {
                MediaRecorder()
            }
            val mediaRecorder = recorderInstance.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            recordingFile = file
            recordingStartTimestamp = System.currentTimeMillis()
            recordingElapsedMs = 0L
            isRecording = true
        } catch (e: Exception) {
            Log.e(CHAT_ROOM_TAG, "Failed to start recording", e)
            errorMessage = e.message
            recorder?.release()
            recorder = null
            recordingFile = null
            isRecording = false
        }
    }

    fun stopRecording(onFinished: (File, Double?) -> Unit) {
        val rec = recorder ?: return
        try {
            rec.stop()
        } catch (e: Exception) {
            Log.e(CHAT_ROOM_TAG, "Error stopping recording", e)
        } finally {
            rec.release()
        }
        recorder = null
        isRecording = false
        val file = recordingFile
        val durationSec = recordingStartTimestamp?.let { start ->
            val elapsed = System.currentTimeMillis() - start
            elapsed / 1000.0
        }
        recordingFile = null
        recordingStartTimestamp = null
        if (file != null && file.exists()) {
            onFinished(file, durationSec)
        } else {
            Log.e(CHAT_ROOM_TAG, "Recording file missing")
        }
    }

    suspend fun sendAudioMessage(file: File, durationSec: Double?) {
        val userId = currentUser?.id ?: return
        isSending = true
        val senderModel = resolvedSenderModel
        val result = ApiService.sendChatAudioMessage(
            roomId = roomId,
            file = file,
            durationSec = durationSec,
            senderModel = senderModel,
            senderId = userId
        )
        result.onFailure {
            Log.e(CHAT_ROOM_TAG, "Failed to send audio message", it)
            errorMessage = it.message
        }.onSuccess { message ->
            messages = (messages + message).sortedBy { it.createdAt }
        }
        isSending = false
        if (file.exists()) {
            file.delete()
        }
    }


    LaunchedEffect(roomId) {
        ChatSocketManager.connect()
        ChatSocketManager.joinRoom(roomId)
        WebRTCSignalingManager.joinRoom(roomId)
        refreshRoom()
    }

    LaunchedEffect(roomId) {
        Log.d(CHAT_ROOM_TAG, "🎧 Started listening for incoming messages in room: $roomId")
        ChatSocketManager.incomingMessages.collect { incoming ->
            Log.d(CHAT_ROOM_TAG, "📨 Received message: room=${incoming.roomId}, text=${incoming.text?.take(30)}")
            if (incoming.roomId == roomId) {
                Log.d(CHAT_ROOM_TAG, "✅ Adding message to list (current count: ${messages.size})")
                messages = (messages + incoming).sortedBy { it.createdAt }
                Log.d(CHAT_ROOM_TAG, "✅ Messages list updated (new count: ${messages.size})")
            } else {
                Log.w(CHAT_ROOM_TAG, "⚠️ Message for different room: ${incoming.roomId} (expected: $roomId)")
            }
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingStartTimestamp = recordingStartTimestamp ?: System.currentTimeMillis()
            while (isActive && isRecording) {
                delay(500)
                val start = recordingStartTimestamp ?: continue
                recordingElapsedMs = System.currentTimeMillis() - start
            }
        } else {
            recordingElapsedMs = 0L
        }
    }

    DisposableEffect(roomId) {
        onDispose {
            ChatSocketManager.leaveRoom(roomId)
            WebRTCSignalingManager.leaveRoom(roomId)
            webrtcManager.endCall()
            recorder?.release()
            recorder = null
            recordingFile?.delete()
            recordingFile = null
            stopAudioPlayback()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    val formattedRecordingTime = remember(recordingElapsedMs) {
        val totalSeconds = (recordingElapsedMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            RoomHeader(
                title = roomDetail?.childName ?: childNameHint ?: "Conversation",
                subtitle = roomDetail?.participants?.size?.takeIf { it > 0 }?.let { "$it participants" },
                onBack = onNavigateBack,
                onRefresh = { scope.launch { refreshRoom() } },
                isRefreshing = isLoading,
                onCallClick = {
                    // Get the other participant to call
                    Log.d(CHAT_ROOM_TAG, "Call button clicked")
                    Log.d(CHAT_ROOM_TAG, "Current user ID: ${currentUser?.id}")
                    Log.d(CHAT_ROOM_TAG, "Room participants: ${roomDetail?.participants}")
                    
                    val otherParticipantId = roomDetail?.participants?.firstOrNull { it != currentUser?.id }
                    
                    if (otherParticipantId != null) {
                        Log.d(CHAT_ROOM_TAG, "Starting call to: $otherParticipantId")
                        webrtcManager.startCall(roomId, otherParticipantId)
                    } else {
                        // Fallback: Try to find participant from messages
                        val otherSenderId = messages.firstOrNull { 
                            it.senderId != null && it.senderId != currentUser?.id 
                        }?.senderId
                        
                        if (otherSenderId != null) {
                            Log.d(CHAT_ROOM_TAG, "Found participant from messages: $otherSenderId")
                            webrtcManager.startCall(roomId, otherSenderId)
                        } else {
                            Log.w(CHAT_ROOM_TAG, "No other participant found to call")
                            errorMessage = "Aucun participant disponible pour appeler"
                        }
                    }
                },
                callState = callState
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Call status messages
            AnimatedVisibility(visible = callState == CallState.CALLING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFE1D3)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFFFF7C55),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "Appel en cours...",
                            color = Color(0xFFB25F39),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Incoming call dialog
            if (callState == CallState.INCOMING) {
                IncomingCallDialog(
                    callerName = roomDetail?.childName ?: "Inconnu",
                    onAccept = { webrtcManager.acceptCall() },
                    onReject = { webrtcManager.rejectCall() }
                )
            }

            // Active call controls
            AnimatedVisibility(visible = callState == CallState.CONNECTED || callState == CallState.CONNECTING) {
                ActiveCallControls(
                    isMuted = isMutedCall,
                    isSpeakerOn = isSpeakerOn,
                    callState = callState,
                    onToggleMute = { webrtcManager.toggleMute() },
                    onToggleSpeaker = { webrtcManager.toggleSpeaker() },
                    onEndCall = { webrtcManager.endCall() }
                )
            }

            AnimatedVisibility(visible = !isLoading && errorMessage != null) {
                ErrorBanner(message = errorMessage ?: "Erreur inconnue")
            }

            AnimatedVisibility(visible = isRecording) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFE1D3)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Enregistrement en cours...",
                            color = Color(0xFFB25F39),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formattedRecordingTime,
                            color = Color(0xFFB25F39),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF7C55))
                    }
                }

                messages.isEmpty() -> {
                    EmptyMessagesState(modifier = Modifier.weight(1f))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        state = listState
                    ) {
                        items(messages.size) { index ->
                            val message = messages[index]
                            val isMine = message.senderId == currentUser?.id
                            val isPlaying = currentlyPlayingMessageId == message.id && !isAudioLoading
                            val loadingAudio = currentlyPlayingMessageId == message.id && isAudioLoading
                            MessageBubble(
                                message = message,
                                isMine = isMine,
                                isAudioPlaying = isPlaying,
                                isAudioLoading = loadingAudio,
                                onAudioAction = { toggleAudioPlayback(it) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            MessageInputRow(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    scope.launch {
                        sendMessage()
                    }
                },
                onToggleRecord = {
                    if (isRecording) {
                        stopRecording { file, durationSec ->
                            scope.launch {
                                sendAudioMessage(file, durationSec)
                            }
                        }
                    } else {
                        startRecording()
                    }
                },
                isRecording = isRecording,
                enabled = !isSending && !isLoading,
                isSending = isSending,
                isSocketConnected = connectionState
            )
        }
    }
}

@Composable
private fun RoomHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onCallClick: () -> Unit,
    callState: CallState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFFEA784D)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Black
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF8C6F5B)
                )
            }
        }

        // Call button
        IconButton(
            onClick = onCallClick,
            enabled = callState == CallState.IDLE,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    when (callState) {
                        CallState.IDLE -> Color(0xFF4CAF50).copy(alpha = 0.8f)
                        CallState.CALLING, CallState.CONNECTING -> Color(0xFFFF9800).copy(alpha = 0.8f)
                        CallState.CONNECTED -> Color(0xFF4CAF50)
                        else -> Color.White.copy(alpha = 0.5f)
                    }
                )
        ) {
            if (callState == CallState.CALLING || callState == CallState.CONNECTING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = White
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.8f))
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFFFF7C55),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFFEA784D)
                )
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFFFE3DD)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFB33A32)
            )
            Text(
                text = message,
                color = Color(0xFFB33A32),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyMessagesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Commencez la conversation",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Black
        )
        Text(
            text = "Envoyez un premier message pour rester connectés.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF8C6F5B)
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    isAudioPlaying: Boolean,
    isAudioLoading: Boolean,
    onAudioAction: (ChatMessage) -> Unit
) {
    val bubbleColor = if (isMine) Color(0xFFFF9553) else Color.White
    val textColor = if (isMine) White else Black

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
            tonalElevation = if (isMine) 0.dp else 4.dp
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!message.text.isNullOrBlank()) {
                    Text(
                        text = message.text,
                        color = textColor,
                        fontSize = 15.sp
                    )
                }
                if (message.audio != null && !message.audio.url.isNullOrBlank()) {
                    AudioMessageRow(
                        isPlaying = isAudioPlaying,
                        isLoading = isAudioLoading,
                        duration = message.audio.durationSec,
                        onClick = { onAudioAction(message) },
                        textColor = textColor
                    )
                }
            }
        }

        val timestamp = message.createdAt?.let { formatTimestampFriendly(it) }
        if (!timestamp.isNullOrBlank()) {
            Text(
                text = timestamp,
                fontSize = 11.sp,
                color = Color(0xFF9F8674),
                modifier = Modifier.padding(top = 4.dp, end = if (isMine) 8.dp else 0.dp)
            )
        }
    }
}

@Composable
private fun AudioMessageRow(
    isPlaying: Boolean,
    isLoading: Boolean,
    duration: Double?,
    onClick: () -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = textColor,
                disabledContainerColor = Color.White.copy(alpha = 0.2f),
                disabledContentColor = textColor.copy(alpha = 0.6f)
            )
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = textColor,
                        strokeWidth = 2.dp
                    )
                }

                isPlaying -> {
                    Icon(
                        imageVector = Icons.Filled.Pause,
                        contentDescription = "Pause"
                    )
                }

                else -> {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Lire"
                    )
                }
            }
        }

        val durationLabel = duration?.let {
            val totalSeconds = it.toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        } ?: "Audio"

        Text(
            text = durationLabel,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MessageInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleRecord: () -> Unit,
    isRecording: Boolean,
    enabled: Boolean,
    isSending: Boolean,
    isSocketConnected: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onToggleRecord,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isRecording) Color(0xFFFF7C55) else Color.White.copy(alpha = 0.8f))
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = if (isRecording) "Arrêter" else "Enregistrer",
                tint = if (isRecording) White else Color(0xFFEA784D)
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            placeholder = { Text("Écrire un message") },
            singleLine = true,
            enabled = enabled && !isRecording,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White.copy(alpha = 0.7f),
                focusedIndicatorColor = Color(0xFFFF9553),
                unfocusedIndicatorColor = Color(0xFFFFD6B2),
                cursorColor = Color(0xFFFF7C55)
            ),
            shape = RoundedCornerShape(20.dp)
        )

        Button(
            onClick = onSend,
            enabled = enabled && value.isNotBlank() && !isRecording,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF7C55),
                contentColor = White,
                disabledContainerColor = Color(0xFFFFC7AD),
                disabledContentColor = White.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.height(48.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Envoyer",
                    tint = if (isSocketConnected) White else White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun IncomingCallDialog(
    callerName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onReject,
        icon = {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Incoming Call",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Appel entrant",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "$callerName vous appelle",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Accepter")
            }
        },
        dismissButton = {
            Button(
                onClick = onReject,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Refuser")
            }
        }
    )
}

@Composable
private fun ActiveCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    callState: CallState,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Call status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (callState == CallState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Connexion en cours...",
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Appel en cours",
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFF44336) else White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // End call button
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF44336))
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Speaker button
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) White.copy(alpha = 0.9f) else White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = if (isSpeakerOn) "Speaker On" else "Speaker Off",
                        tint = if (isSpeakerOn) Color(0xFF4CAF50) else White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestampFriendly(iso: String): String {
    if (iso.isBlank()) return ""
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val instant = Instant.parse(iso)
            val local = instant.atZone(ZoneId.systemDefault())
            val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
            formatter.format(local)
        } catch (e: Exception) {
            iso.substringBefore("T")
        }
    } else {
        iso.substringBefore("T")
    }
}
