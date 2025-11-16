# Debug WebRTC Calls - Not Working

## Current Issue
When clicking the call button, nothing happens.

## Debug Steps

### 1. Check Logcat

Run the app and filter Logcat by **"ChatRoomScreen"** or **"WebRTC"**

When you click the call button, you should see:
```
ChatRoomScreen: Call button clicked
ChatRoomScreen: Current user ID: 67890...
ChatRoomScreen: Room participants: [12345..., 67890...]
ChatRoomScreen: Starting call to: 12345...
WebRTCSignaling: Sent call request to 12345... in room room_abc
ChatSocket: Sent WebRTC signal: call-request
```

### 2. Common Problems & Solutions

#### Problem A: Empty Participants List
```
ChatRoomScreen: Room participants: []
ChatRoomScreen: No other participant found to call
```

**Cause**: Backend isn't returning participants in room details

**Solution**: Check your backend API response for `/rooms/{roomId}` endpoint

Expected response should include:
```json
{
  "id": "room_123",
  "childName": "Alice",
  "participants": ["user_1", "user_2"],  ← This field
  "lastMessage": { ... }
}
```

If missing, update your backend or modify the fallback.

---

#### Problem B: Only Current User in Participants
```
ChatRoomScreen: Room participants: [current_user_id]
ChatRoomScreen: No other participant found to call
```

**Cause**: Room only has one participant (you)

**Solution**: Ensure the other user has joined the room. Check:
1. Both users opened the same room?
2. Backend adds both users to participants array?
3. Try sending a text message first (to ensure room is active)

---

#### Problem C: Call Button Disabled
The call button might be disabled if `callState != CallState.IDLE`

**Check**: Look at the call button color
- 🟢 Green = Ready (idle)
- 🟠 Orange = Calling/Connecting
- ⚫ Gray = Disabled

**Solution**: If stuck in calling state, restart the app

---

#### Problem D: WebRTC Signal Not Sent
```
ChatRoomScreen: Starting call to: 12345...
(no "Sent call request" message)
```

**Cause**: Socket not connected or WebRTC manager not initialized

**Check Logcat for**:
```
ChatSocket: Socket connected
WebRTCSignaling: Using existing chat room: room_123
```

**Solution**: Ensure chat socket is connected (try sending a text message first)

---

### 3. Manual Test with Hardcoded User ID

If participants list is empty, you can temporarily hardcode a test:

**In ChatRoomScreen.kt** (line ~549), temporarily replace with:
```kotlin
onCallClick = {
    Log.d(CHAT_ROOM_TAG, "Call button clicked")
    
    // TEMPORARY: Hardcode the other user's ID for testing
    val testParticipantId = "PASTE_OTHER_USER_ID_HERE"
    
    Log.d(CHAT_ROOM_TAG, "Testing call to: $testParticipantId")
    webrtcManager.startCall(roomId, testParticipantId)
},
```

Replace `PASTE_OTHER_USER_ID_HERE` with the actual user ID of the person you want to call.

This bypasses the participant lookup to test if the call mechanism works.

---

### 4. Check Backend Message Relay

After clicking call, check if the signal reaches the backend:

**Server logs should show**:
```
Received sendText event
Broadcasting to room: room_123
```

**Other device Logcat should show**:
```
ChatSocket: newMessage received
WebRTCSignaling: Received WebRTC signal: CALL_REQUEST
WebRTCAudioCall: Incoming call
```

If the message doesn't reach the other device:
1. Check both devices are in same room
2. Check Socket.IO connection on both devices
3. Check backend is broadcasting to room correctly

---

### 5. Full Debug Checklist

Run through this checklist:

- [ ] **Socket connected?**
  - Logcat: "Socket connected" ✅
  
- [ ] **User ID available?**
  - Logcat shows valid user ID ✅
  
- [ ] **Room loaded?**
  - Can see chat messages ✅
  
- [ ] **Participants list?**
  - Logcat shows participants array ✅
  
- [ ] **Other user in list?**
  - Participants has more than just current user ✅
  
- [ ] **Call button enabled?**
  - Button is green/clickable ✅
  
- [ ] **Call request sent?**
  - Logcat: "Sent WebRTC signal: call-request" ✅
  
- [ ] **Microphone permission?**
  - Granted when prompted ✅

---

### 6. Test on Two Devices Simultaneously

**Device A (Caller):**
1. Open room
2. Click call button
3. Watch Logcat for "Sent call request"
4. Should see "Calling..." indicator

**Device B (Receiver):**
1. Open same room
2. Should see incoming call dialog
3. Logcat: "Received WebRTC signal: CALL_REQUEST"
4. Accept or reject

**If Device B doesn't receive:**
- Check both in exact same room ID
- Check Socket.IO relay on backend
- Try sending text message to verify socket works

---

### 7. Network/Firewall Issues

If call connects but no audio:
- Check both devices have internet
- Try on WiFi (not cellular) for testing
- Check firewall/NAT settings
- STUN servers might be blocked

---

## Quick Fixes

### Fix 1: Use Message Sender as Participant

The code now has a fallback that finds the other participant from message history. This should work if you've exchanged messages.

### Fix 2: Add Debug Button

Add a temporary debug button to test calls directly:

```kotlin
// Add after the call button
Button(
    onClick = {
        // Test call with first message sender
        val testId = messages.firstOrNull { it.senderId != currentUser?.id }?.senderId
        testId?.let { webrtcManager.startCall(roomId, it) }
            ?: run { errorMessage = "No messages found" }
    }
) {
    Text("Debug Call")
}
```

---

## Expected Full Flow

1. Click call button
2. See "Appel en cours..." banner (orange)
3. Call button turns orange with spinner
4. Other device sees incoming call dialog
5. Accept → Both see green "Appel en cours" card
6. Connecting → Connected (talk!)

---

## Next Steps

1. Run app with Logcat open
2. Filter by "ChatRoomScreen"
3. Click call button
4. Share the log output

Then we can pinpoint exactly where it's failing!

---

## Most Likely Issues

Based on typical setups:

1. **Participants array not populated** (80% likely)
   - Backend doesn't return participants
   - Fix: Use message sender fallback or update backend

2. **Socket not connected** (15% likely)
   - WebSocket disconnected
   - Fix: Ensure chat messages work first

3. **Permission issues** (5% likely)
   - Microphone permission denied
   - Fix: Grant permission when prompted

