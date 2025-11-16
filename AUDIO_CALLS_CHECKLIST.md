# 🎙️ Audio Calls Complete Setup Checklist

## ✅ What's Already Done

### 1. ✅ Dependencies Added
- `io.getstream:stream-webrtc-android:1.1.3` in `app/build.gradle.kts`
- Socket.IO for signaling: `io.socket:socket.io-client:2.1.1`

### 2. ✅ Permissions in AndroidManifest.xml
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

### 3. ✅ WebRTC Implementation Files Created
- ✅ `WebRTCAudioCallManager.kt` - Core WebRTC logic
- ✅ `WebRTCSignalingManager.kt` - Signaling through Socket.IO
- ✅ `WebRTCModels.kt` - Data models for calls
- ✅ `ChatSocketManager.kt` - Socket connection with WebRTC support

### 4. ✅ UI Integration in ChatRoomScreen
- ✅ Call button in header
- ✅ Incoming call dialog
- ✅ Active call controls (mute, speaker, hang up)
- ✅ Call state management

### 5. ✅ Real-time Messages Fixed
- ✅ Messages now appear instantly without refresh

---

## 🚀 What You Need to Do Now

### Step 1: Request Runtime Permissions for Calls

The app only requests `RECORD_AUDIO` for voice messages. For **audio calls**, you need to also request **BLUETOOTH_CONNECT** on Android 12+.

**Add this to `ChatRoomScreen.kt`** after the existing permission code (around line 172):

```kotlin
// Request multiple permissions for audio calls
val callPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.BLUETOOTH_CONNECT
    )
} else {
    arrayOf(Manifest.permission.RECORD_AUDIO)
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
```

### Step 2: Request Permissions Before Starting Call

**Modify the `onCallClick` function** in `ChatRoomScreen.kt` (around line 270):

Find this code:
```kotlin
val onCallClick: () -> Unit = {
    Log.d(CHAT_ROOM_TAG, "📞 Call button clicked - current state: $callState")
```

Add permission check at the beginning:
```kotlin
val onCallClick: () -> Unit = {
    Log.d(CHAT_ROOM_TAG, "📞 Call button clicked - current state: $callState")
    
    // Check permissions first
    val hasRecordPermission = ContextCompat.checkSelfPermission(
        context, 
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasBluetoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Not needed on older Android
    }
    
    if (!hasRecordPermission || !hasBluetoothPermission) {
        Log.w(CHAT_ROOM_TAG, "⚠️ Missing permissions - requesting...")
        callPermissionLauncher.launch(callPermissions)
        return@Unit
    }
    
    // Rest of your existing call logic...
```

### Step 3: Sync Build

```bash
./gradlew clean build
```

Or in Android Studio: **File → Sync Project with Gradle Files**

---

## 🧪 How to Test Audio Calls

### Option A: Two Physical Devices (Recommended)
1. Install app on two phones
2. Both login and join same chat room
3. Click call button on Device 1
4. Accept call on Device 2
5. You should hear each other!

### Option B: One Emulator + One Physical Device
1. Device 1: Physical phone
2. Device 2: Android Emulator
3. **Important**: Use your computer's actual IP instead of `10.0.2.2`
   - In `ChatSocketManager.kt` line 34, change:
     ```kotlin
     private const val SOCKET_URL = "http://YOUR_COMPUTER_IP:3005"
     ```
   - Example: `http://192.168.1.100:3005`
4. Backend must be accessible on your local network

### Option C: Two Emulators (May Have Issues)
- Two emulators can test signaling, but audio may not work
- Emulators don't have real microphones
- Use for UI testing only

---

## 📱 Testing Steps

### 1. Start a Call
1. Open chat room with another user
2. Click the **phone icon** in top right
3. You should see:
   - Call button becomes red "phone" icon
   - Banner: "Appel en cours..."
   - Log: `📞 Starting call...`

### 2. Receive a Call
1. Other device should see:
   - Dialog: "Appel entrant de [Name]"
   - Two buttons: "Refuser" / "Accepter"
2. Click **Accepter**

### 3. During Call
Both devices should show:
- Green "Appel en cours..." banner
- Controls at bottom:
  - 🎤 **Mute/Unmute**
  - 🔊 **Speaker on/off**
  - ❌ **Hang up**

### 4. End Call
- Click red **hang up** button
- Or back button (calls `webrtcManager.endCall()`)

---

## 🔍 Debugging Tips

### Check Logcat for These Tags:
```bash
adb logcat -s ChatSocket:D WebRTCAudioCall:D WebRTCSignal:D ChatRoomScreen:D
```

### Expected Logs for Successful Call:

**Device 1 (Caller):**
```
📞 Starting call...
✅ Sent WebRTC signal: call_offer to room [roomId]
🔊 WebRTC signal received: call_answer
✅ Call connected
```

**Device 2 (Receiver):**
```
🔊 WebRTC signal received: call_offer
📞 Incoming call from: [userId]
[User accepts]
✅ Sent WebRTC signal: call_answer to room [roomId]
✅ Call connected
```

### Common Issues:

#### 1. "Cannot send WebRTC signal - socket not connected"
- **Fix**: Wait a few seconds after joining room, socket needs to connect
- Check: `Socket connected successfully` in logs

#### 2. No incoming call dialog
- **Fix**: Both users must be in the same room
- Check: `Joined room: [roomId]` in logs for both devices

#### 3. "Missing permissions"
- **Fix**: Complete Step 1 & 2 above to request all permissions
- Check: Settings → Apps → Your App → Permissions

#### 4. One-way audio (only one person can hear)
- **Fix**: Check microphone permissions on both devices
- Try toggling speaker mode on both

#### 5. Call drops immediately
- **Fix**: WebRTC needs STUN servers to work across networks
- Current implementation uses Google's public STUN servers (already configured)

---

## 🎯 Quick Status Check

Run this checklist:
- [ ] Sync Gradle (all dependencies downloaded)
- [ ] Permissions requested in code (Step 1 & 2 above)
- [ ] Backend running on `localhost:3005` or accessible IP
- [ ] Socket.IO connected (check logs: "Socket connected successfully")
- [ ] Both users in same chat room
- [ ] Microphone permission granted on both devices

---

## 🔧 Next Steps if Still Not Working

1. **Share logcat output** from both devices when clicking call button
2. **Check backend logs** - are WebRTC signals being received?
3. **Try with same network** - both devices on same WiFi first
4. **Verify Socket.IO connection** - send text message first to confirm real-time works

---

## 📚 Additional Resources

- [WebRTC Quick Start Guide](WEBRTC_QUICK_START.md)
- [WebRTC Technical Details](WEBRTC_AUDIO_CALLS.md)
- [Backend Real-time Fix](BACKEND_REALTIME_FIX.md)

---

**Ready to test? Follow Step 1 & 2 above, then try making a call!** 📞✨

