package com.example.dam_android.webrtc

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.dam_android.models.CallState
import com.example.dam_android.models.SignalType
import com.example.dam_android.models.SignalingMessage
import com.example.dam_android.network.socket.WebRTCSignalingManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.webrtc.*

class WebRTCAudioCallManager(private val context: Context) {

    private val TAG = "WebRTCAudioCall"
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var audioTrack: AudioTrack? = null

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private var currentRoomId: String? = null
    private var currentPeerId: String? = null
    private var isInitiator = false

    // queue remote ICE candidates until remote description is set
    private val remoteIceQueue = mutableListOf<com.example.dam_android.models.IceCandidate>()
    private var remoteDescriptionSet: Boolean = false

    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
    )

    init {
        initializePeerConnectionFactory()
        startListeningToSignals()
    }

    private fun initializePeerConnectionFactory() {
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(null)
            .setVideoDecoderFactory(null)
            .setOptions(PeerConnectionFactory.Options())
            .createPeerConnectionFactory()
        Log.d(TAG, "PeerConnectionFactory initialized")
    }

    private fun startListeningToSignals() {
        scope.launch {
            WebRTCSignalingManager.incomingSignals.collectLatest { signal ->
                handleSignalingMessage(signal)
            }
        }
    }

    private fun handleSignalingMessage(signal: SignalingMessage) {
        Log.d(TAG, "Signal received: ${signal.type}")
        when (signal.type) {
            SignalType.CALL_REQUEST -> {
                currentRoomId = signal.roomId
                currentPeerId = signal.senderId
                isInitiator = false
                _callState.value = CallState.INCOMING
            }

            SignalType.CALL_ACCEPTED -> {
                _callState.value = CallState.CONNECTING
                if (isInitiator) scope.launch { createOffer() }
            }

            SignalType.CALL_REJECTED -> {
                _callState.value = CallState.REJECTED
                cleanup()
            }

            SignalType.CALL_ENDED -> {
                _callState.value = CallState.ENDED
                cleanup()
            }

            SignalType.OFFER -> {
                val model = gson.fromJson(
                    signal.data,
                    com.example.dam_android.models.SessionDescription::class.java
                )
                handleRemoteOffer(model.sdp)
            }

            SignalType.ANSWER -> {
                val model = gson.fromJson(
                    signal.data,
                    com.example.dam_android.models.SessionDescription::class.java
                )
                handleRemoteAnswer(model.sdp)
            }

            SignalType.ICE_CANDIDATE -> {
                val model = gson.fromJson(
                    signal.data,
                    com.example.dam_android.models.IceCandidate::class.java
                )
                addRemoteIceCandidate(model)
            }
        }
    }

    // ---------------------------
    // Public call control
    // ---------------------------

    fun startCall(roomId: String, targetId: String) {
        currentRoomId = roomId
        currentPeerId = targetId
        isInitiator = true
        _callState.value = CallState.CALLING
        WebRTCSignalingManager.sendCallRequest(roomId, targetId)
        Log.d(TAG, "Call requested to $targetId in room $roomId")
    }

    fun acceptCall() {
        val room = currentRoomId ?: return
        val peer = currentPeerId ?: return
        isInitiator = false
        _callState.value = CallState.CONNECTING
        WebRTCSignalingManager.acceptCall(room, peer)
        ensurePeerConnection()
        Log.d(TAG, "Accepted call from $peer")
    }

    fun rejectCall() {
        val room = currentRoomId ?: return
        val peer = currentPeerId ?: return
        WebRTCSignalingManager.rejectCall(room, peer)
        _callState.value = CallState.REJECTED
        cleanup()
        Log.d(TAG, "Rejected call from $peer")
    }

    fun endCall() {
        val room = currentRoomId ?: return
        WebRTCSignalingManager.endCall(room, currentPeerId)
        _callState.value = CallState.ENDED
        cleanup()
        Log.d(TAG, "Ended call")
    }

    // ---------------------------
    // Peer connection & tracks
    // ---------------------------

    private fun ensurePeerConnection() {
        if (peerConnection != null) return
        remoteDescriptionSet = false
        remoteIceQueue.clear()

        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState?) {
                Log.d(TAG, "signaling=$newState")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "iceState=$state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> _callState.value = CallState.CONNECTED
                    PeerConnection.IceConnectionState.FAILED,
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.CLOSED -> {
                        if (_callState.value == CallState.CONNECTED) _callState.value = CallState.ENDED
                        cleanup()
                    }
                    else -> {}
                }
            }

            override fun onIceCandidate(c: org.webrtc.IceCandidate?) {
                c ?: return
                val room = currentRoomId ?: return
                val peer = currentPeerId ?: return
                WebRTCSignalingManager.sendIceCandidate(
                    roomId = room,
                    targetId = peer,
                    candidate = c.sdp,
                    sdpMid = c.sdpMid,
                    sdpMLineIndex = c.sdpMLineIndex
                )
            }

            override fun onIceCandidatesRemoved(candidates: Array<out org.webrtc.IceCandidate>?) {}
            override fun onAddStream(stream: org.webrtc.MediaStream?) {}
            override fun onRemoveStream(stream: org.webrtc.MediaStream?) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, mediaStreams: Array<out org.webrtc.MediaStream>?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
        })

        createAndAddAudioTrack()
        Log.d(TAG, "PeerConnection created")
    }

    private fun createAndAddAudioTrack() {
        val factory = peerConnectionFactory ?: return
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("audio0", audioSource)
        audioTrack?.setEnabled(true)
        peerConnection?.addTrack(audioTrack)
        Log.d(TAG, "Local audio track created and added")
    }

    // ---------------------------
    // Offer / Answer
    // ---------------------------

    private suspend fun createOffer() = withContext(Dispatchers.Default) {
        ensurePeerConnection()
        val pc = peerConnection ?: return@withContext
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        pc.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        val room = currentRoomId ?: return
                        val peer = currentPeerId ?: return
                        WebRTCSignalingManager.sendOffer(room, peer, sdp.description)
                        Log.d(TAG, "Offer created and sent")
                    }

                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "setLocalDescription failed: $error")
                    }
                }, sdp)
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createOffer failed: $error")
                _callState.value = CallState.ERROR
            }
        }, constraints)
    }

    private fun handleRemoteOffer(sdp: String) {
        ensurePeerConnection()
        val desc = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                drainRemoteIceQueue()
                scope.launch { createAnswer() }
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "setRemoteDescription (offer) failed: $error")
            }
        }, desc)
    }

    private suspend fun createAnswer() = withContext(Dispatchers.Default) {
        val pc = peerConnection ?: return@withContext
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        pc.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(object : SimpleSdpObserver() {
                    override fun onSetSuccess() {
                        val room = currentRoomId ?: return
                        val peer = currentPeerId ?: return
                        WebRTCSignalingManager.sendAnswer(room, peer, sdp.description)
                        Log.d(TAG, "Answer created and sent")
                    }
                }, sdp)
            }
        }, constraints)
    }

    private fun handleRemoteAnswer(sdp: String) {
        val desc = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                remoteDescriptionSet = true
                drainRemoteIceQueue()
                Log.d(TAG, "Remote answer set")
            }
        }, desc)
    }

    // ---------------------------
    // ICE candidates
    // ---------------------------

    private fun addRemoteIceCandidate(model: com.example.dam_android.models.IceCandidate) {
        if (!remoteDescriptionSet) {
            remoteIceQueue.add(model)
            Log.d(TAG, "Queued remote ICE candidate (queue size=${remoteIceQueue.size})")
            return
        }
        val rtc = org.webrtc.IceCandidate(model.sdpMid, model.sdpMLineIndex, model.candidate)
        peerConnection?.addIceCandidate(rtc)
        Log.d(TAG, "Remote ICE candidate added")
    }

    private fun drainRemoteIceQueue() {
        remoteIceQueue.forEach { model ->
            try {
                val rtc = org.webrtc.IceCandidate(model.sdpMid, model.sdpMLineIndex, model.candidate)
                peerConnection?.addIceCandidate(rtc)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to add queued ICE candidate", t)
            }
        }
        remoteIceQueue.clear()
        Log.d(TAG, "Drained ICE queue")
    }

    // ---------------------------
    // Mute / Speaker
    // ---------------------------

    fun toggleMute() {
        val muted = !_isMuted.value
        audioTrack?.setEnabled(!muted)
        _isMuted.value = muted
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        am?.isSpeakerphoneOn = _isSpeakerOn.value
    }

    // ---------------------------
    // Cleanup
    // ---------------------------

    private fun cleanup() {
        try {
            audioTrack?.dispose()
            audioTrack = null
            audioSource?.dispose()
            audioSource = null
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null
        } finally {
            remoteIceQueue.clear()
            remoteDescriptionSet = false
            currentRoomId = null
            currentPeerId = null
            isInitiator = false
            _isMuted.value = false
            _isSpeakerOn.value = false
            _callState.value = CallState.IDLE
        }
    }

    fun dispose() {
        cleanup()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        scope.cancel()
        Log.d(TAG, "WebRTCAudioCallManager disposed")
    }

    private open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(error: String?) {}
        override fun onSetFailure(error: String?) {}
    }
}
