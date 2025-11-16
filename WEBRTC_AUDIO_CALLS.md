# WebRTC Audio Calls Implementation

## Overview

This document describes the peer-to-peer audio call functionality implemented using WebRTC in the Android app. The implementation allows users to make real-time audio calls within chat rooms.

## Architecture

The WebRTC implementation consists of several key components:

### 1. **Data Models** (`WebRTCModels.kt`)
- `SignalType`: Enum for different signaling message types (OFFER, ANSWER, ICE_CANDIDATE, etc.)
- `SignalingMessage`: Wrapper for all signaling messages
- `CallState`: Current state of the call (IDLE, CALLING, INCOMING, CONNECTING, CONNECTED, etc.)
- `SessionDescription`: WebRTC session description (SDP)
- `IceCandidate`: WebRTC ICE candidate information

### 2. **WebRTC Signaling Manager** (`WebRTCSignalingManager.kt`)
Handles the signaling between peers using Socket.IO:
- Connects to the backend WebSocket server
- Sends and receives signaling messages
- Manages call request/accept/reject flow
- Handles offer/answer exchange
- Manages ICE candidate exchange

### 3. **WebRTC Audio Call Manager** (`WebRTCAudioCallManager.kt`)
Manages the actual WebRTC peer connection:
- Initializes WebRTC PeerConnectionFactory
- Creates and manages PeerConnection
- Handles local/remote audio tracks
- Manages offer/answer creation
- Handles ICE candidates
- Provides call controls (mute, speaker, end call)

### 4. **UI Integration** (`ChatRoomScreen.kt`)
- Call button in the room header
- Incoming call dialog
- Active call controls (mute, speaker, end call)
- Call state indicators

## Call Flow

### Initiating a Call

1. User clicks the call button in the chat room header
2. `WebRTCAudioCallManager.startCall()` is called
3. A `CALL_REQUEST` signal is sent to the peer via WebSocket
4. Local call state changes to `CALLING`

### Receiving a Call

1. Peer receives `CALL_REQUEST` signal
2. Call state changes to `INCOMING`
3. Incoming call dialog is displayed
4. User can accept or reject the call

### Accepting a Call

1. User clicks "Accept" in the dialog
2. `WebRTCAudioCallManager.acceptCall()` is called
3. `CALL_ACCEPTED` signal is sent to the caller
4. PeerConnection is initialized
5. Audio tracks are created and added

### WebRTC Negotiation

**Caller side (after receiving CALL_ACCEPTED):**
1. Creates WebRTC offer (SDP)
2. Sets local description
3. Sends `OFFER` signal to peer

**Callee side (after receiving OFFER):**
1. Sets remote description with received SDP
2. Creates WebRTC answer
3. Sets local description
4. Sends `ANSWER` signal to peer

**Caller side (after receiving ANSWER):**
1. Sets remote description with received SDP
2. Connection is established

### ICE Candidate Exchange

- Both peers generate ICE candidates during connection setup
- Each candidate is sent to the peer via `ICE_CANDIDATE` signal
- Peers add received candidates to their PeerConnection
- Connection is established when suitable candidate pair is found

### Connected Call

- Call state changes to `CONNECTED`
- Audio flows between peers
- Call controls are available (mute, speaker, end call)

### Ending a Call

1. User clicks end call button
2. `WebRTCAudioCallManager.endCall()` is called
3. `CALL_ENDED` signal is sent to peer
4. PeerConnection is closed and cleaned up
5. Call state returns to `IDLE`

## Backend Requirements

For the WebRTC implementation to work, your backend (`https://weldiwinbackend.vercel.app`) needs to implement the following Socket.IO events:

### Events to Listen For (from clients):

1. **`webrtc:join-room`**
   - Payload: `{ roomId: string, userId: string }`
   - Join a room for WebRTC signaling

2. **`webrtc:leave-room`**
   - Payload: `{ roomId: string, userId: string }`
   - Leave a WebRTC room

3. **`webrtc:call-request`**
   - Payload: `{ roomId: string, senderId: string, targetId: string }`
   - User initiates a call to another user

4. **`webrtc:call-accepted`**
   - Payload: `{ roomId: string, senderId: string, targetId: string }`
   - User accepts an incoming call

5. **`webrtc:call-rejected`**
   - Payload: `{ roomId: string, senderId: string, targetId: string }`
   - User rejects an incoming call

6. **`webrtc:call-ended`**
   - Payload: `{ roomId: string, senderId: string, targetId?: string }`
   - User ends the current call

7. **`webrtc:offer`**
   - Payload: `{ roomId: string, senderId: string, targetId: string, data: string }`
   - WebRTC offer (SDP) from caller

8. **`webrtc:answer`**
   - Payload: `{ roomId: string, senderId: string, targetId: string, data: string }`
   - WebRTC answer (SDP) from callee

9. **`webrtc:ice-candidate`**
   - Payload: `{ roomId: string, senderId: string, targetId: string, data: string }`
   - ICE candidate exchange

### Events to Emit (to clients):

The backend should relay these messages to the appropriate target user in the room:

- `webrtc:call-request`
- `webrtc:call-accepted`
- `webrtc:call-rejected`
- `webrtc:call-ended`
- `webrtc:offer`
- `webrtc:answer`
- `webrtc:ice-candidate`

### Example Backend Implementation (Node.js/Socket.IO):

```javascript
io.on('connection', (socket) => {
  // Join WebRTC room
  socket.on('webrtc:join-room', ({ roomId, userId }) => {
    socket.join(roomId);
    console.log(`User ${userId} joined WebRTC room ${roomId}`);
  });

  // Leave WebRTC room
  socket.on('webrtc:leave-room', ({ roomId, userId }) => {
    socket.leave(roomId);
    console.log(`User ${userId} left WebRTC room ${roomId}`);
  });

  // Relay signaling messages to target user
  const relaySignal = (eventName) => {
    socket.on(eventName, (data) => {
      const { roomId, targetId, senderId, ...rest } = data;
      
      // Send to specific user if targetId is provided
      if (targetId) {
        io.to(roomId).emit(eventName, {
          roomId,
          senderId,
          targetId,
          ...rest
        });
      } else {
        // Broadcast to room (except sender)
        socket.to(roomId).emit(eventName, {
          roomId,
          senderId,
          ...rest
        });
      }
    });
  };

  // Relay all signaling events
  relaySignal('webrtc:call-request');
  relaySignal('webrtc:call-accepted');
  relaySignal('webrtc:call-rejected');
  relaySignal('webrtc:call-ended');
  relaySignal('webrtc:offer');
  relaySignal('webrtc:answer');
  relaySignal('webrtc:ice-candidate');
});
```

## Permissions

The following Android permissions are required and have been added to `AndroidManifest.xml`:

- `INTERNET` - For network communication
- `RECORD_AUDIO` - For capturing audio
- `MODIFY_AUDIO_SETTINGS` - For managing audio routing
- `BLUETOOTH` - For Bluetooth audio devices
- `BLUETOOTH_CONNECT` - For connecting to Bluetooth devices (Android 12+)

## Dependencies

The following dependency has been added to `app/build.gradle.kts`:

```kotlin
implementation("io.getstream:stream-webrtc-android:1.1.3")
```

This library provides a wrapper around the native WebRTC library with Kotlin-friendly APIs.

## STUN Servers

The implementation uses Google's public STUN servers for NAT traversal:

- `stun:stun.l.google.com:19302`
- `stun:stun1.l.google.com:19302`
- `stun:stun2.l.google.com:19302`

For production use, you may want to consider:
1. Setting up your own STUN server
2. Adding TURN servers for better connectivity in restrictive networks

## Security Considerations

1. **Authentication**: All WebSocket connections use JWT tokens for authentication
2. **Room-based isolation**: Signaling is limited to users in the same chat room
3. **Peer-to-peer**: Audio data flows directly between peers (not through server)
4. **DTLS-SRTP**: WebRTC uses DTLS-SRTP for encrypted media transmission

## Testing

To test the audio call functionality:

1. **Two devices/emulators**: You need at least two devices or emulators
2. **Same room**: Both users must be in the same chat room
3. **Microphone permission**: Grant microphone permission when prompted
4. **Start call**: Click the green call button in the room header
5. **Accept call**: The other user will see an incoming call dialog
6. **Test controls**: Try mute, speaker, and end call buttons

## Troubleshooting

### Call doesn't connect

1. Check backend WebSocket implementation
2. Verify both users are in the same room
3. Check network connectivity
4. Look for errors in Logcat (filter by "WebRTC" tags)

### No audio

1. Check microphone permissions
2. Verify audio is not muted
3. Check device volume settings
4. Test with different STUN/TURN servers

### Connection drops

1. Consider adding TURN servers for better NAT traversal
2. Check network stability
3. Implement reconnection logic

## Future Enhancements

Possible improvements to consider:

1. **TURN servers**: Add TURN servers for better connectivity
2. **Call history**: Track call duration and history
3. **Group calls**: Support for multi-party audio calls
4. **Call notifications**: Push notifications for incoming calls
5. **Call quality indicators**: Show connection quality
6. **Echo cancellation**: Improve audio quality with advanced settings
7. **Recording**: Add call recording functionality
8. **Screen sharing**: Extend to support screen sharing

## Files Created/Modified

### New Files:
- `app/src/main/java/com/example/dam_android/models/WebRTCModels.kt`
- `app/src/main/java/com/example/dam_android/network/socket/WebRTCSignalingManager.kt`
- `app/src/main/java/com/example/dam_android/webrtc/WebRTCAudioCallManager.kt`

### Modified Files:
- `app/build.gradle.kts` - Added WebRTC dependency
- `app/src/main/AndroidManifest.xml` - Added audio and Bluetooth permissions
- `app/src/main/java/com/example/dam_android/screens/ChatRoomScreen.kt` - Added call UI and controls

## Support

For issues or questions:
1. Check Logcat for error messages
2. Verify backend implementation matches requirements
3. Test with different network conditions
4. Review WebRTC documentation: https://webrtc.org/

