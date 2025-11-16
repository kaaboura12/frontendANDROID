# 🔧 Backend WebRTC Signaling Fix

## Problem Identified

**Symptom**: Device 1 sends call request, but Device 2 receives nothing.

**Root Cause**: The backend's `sendText` handler in `chat.gateway.ts` was treating WebRTC signals as regular text messages:
1. Trying to save them to the database
2. Stripping out WebRTC-specific fields (`messageType`, `signalType`, `targetId`, `data`)
3. Only broadcasting the saved message (which lost all WebRTC data)

## Solution Applied

Modified `chat.gateway.ts` to detect and relay WebRTC signals **without** saving them to the database.

### Before (Line 84-100):
```typescript
@SubscribeMessage('sendText')
async onSendText(
  @ConnectedSocket() client: Socket,
  @MessageBody()
  body: { roomId: string; text: string; senderModel: 'User' | 'Child'; senderId: string },
) {
  if (!client.data.user) {
    return { error: 'Unauthorized' };
  }
  try {
    const msg = await this.messageService.sendText(body, client.data.user);
    this.server.to(`room:${body.roomId}`).emit('newMessage', msg);
    return msg;
  } catch (error: any) {
    return { error: error.message };
  }
}
```

### After (Line 85-110):
```typescript
@SubscribeMessage('sendText')
async onSendText(
  @ConnectedSocket() client: Socket,
  @MessageBody()
  body: any, // Changed to accept both text messages and WebRTC signals
) {
  if (!client.data.user) {
    return { error: 'Unauthorized' };
  }
  try {
    // Check if this is a WebRTC signaling message
    if (body.messageType === 'webrtc_signal') {
      // WebRTC signals are NOT saved to DB, just relayed in real-time
      console.log(`🔊 Relaying WebRTC signal: ${body.signalType} in room ${body.roomId}`);
      this.server.to(`room:${body.roomId}`).emit('newMessage', body);
      return { ok: true, type: 'webrtc_signal' };
    }
    
    // Regular text message - save to DB and broadcast
    const msg = await this.messageService.sendText(body, client.data.user);
    this.server.to(`room:${body.roomId}`).emit('newMessage', msg);
    return msg;
  } catch (error: any) {
    return { error: error.message };
  }
}
```

## What Changed

1. **Message body type**: Changed from strict type to `any` to accept both text messages and WebRTC signals
2. **WebRTC detection**: Added check for `body.messageType === 'webrtc_signal'`
3. **WebRTC relay**: Signals are broadcast directly without database save
4. **Regular messages**: Continue to work as before (saved to DB, then broadcast)

## WebRTC Signal Flow (Now Fixed)

### Device 1 (Caller):
```
1. User clicks call button
2. Android sends via Socket.IO:
   {
     "roomId": "6915d676583815220a9187ff",
     "messageType": "webrtc_signal",
     "signalType": "call-request",
     "senderId": "690ba99b2d7c5e27b98b59af",
     "targetId": "...",
     "senderModel": "User",
     "text": ""
   }
```

### Backend (NestJS):
```
3. Receives on 'sendText' event
4. Detects messageType === 'webrtc_signal'
5. Logs: "🔊 Relaying WebRTC signal: call-request in room ..."
6. Broadcasts to room via 'newMessage' event (KEEPS ALL FIELDS)
```

### Device 2 (Receiver):
```
7. Receives 'newMessage' event
8. ChatSocketManager detects messageType === 'webrtc_signal'
9. Routes to _incomingSignals flow
10. WebRTCSignalingManager processes signal
11. Shows incoming call dialog! ✅
```

## How to Deploy

### 1. Restart Backend
```bash
# In your backend directory
npm run start:dev
# or
pm2 restart your-app
```

### 2. Test the Fix

**Expected backend logs when call is initiated:**
```
🔊 Relaying WebRTC signal: call-request in room 6915d676583815220a9187ff
```

**Expected Android logs on Device 2:**
```
📨 Received newMessage event
📨 Message payload: {"messageType":"webrtc_signal",...}
🔊 WebRTC signal received: call-request
📞 Incoming call from: 690ba99b2d7c5e27b98b59af
```

### 3. Full Test Steps

1. **Restart backend** with the fix
2. **Keep both apps running** (don't reinstall Android app)
3. **Device 1**: Click call button
4. **Device 2**: Should now see incoming call dialog!
5. **Accept call** and test audio

## Why This Fix Works

### Before:
- WebRTC signals were treated as text messages
- Database save operation stripped custom fields
- Device 2 received incomplete data

### After:
- WebRTC signals are detected and relayed directly
- No database save (signals are ephemeral, not persistent messages)
- Device 2 receives full signal with all fields intact
- Regular text messages still work normally

## Benefits

✅ **No Android changes needed** - The Android app already handles this correctly  
✅ **No database schema changes** - WebRTC signals don't need persistence  
✅ **Backward compatible** - Regular messages work exactly as before  
✅ **Performance improvement** - Signals relay instantly without DB write  

## Verification Checklist

After deploying this fix, verify:

- [ ] Backend restarts successfully
- [ ] Regular text messages still work
- [ ] Audio messages still work
- [ ] Backend logs show "🔊 Relaying WebRTC signal..." when call is initiated
- [ ] Device 2 shows incoming call dialog
- [ ] Call can be accepted
- [ ] Audio flows both ways

## Troubleshooting

### If calls still don't work:

1. **Check backend logs** - Do you see "🔊 Relaying WebRTC signal..."?
   - YES → Backend is relaying, check Android logs on Device 2
   - NO → Backend isn't detecting the signal, check the fix was applied

2. **Check Device 2 Android logs**:
   ```bash
   adb logcat -s ChatSocket:D WebRTCSignal:D
   ```
   - Look for: `🔊 WebRTC signal received: call-request`
   - If missing: Device 2 isn't in the room or socket disconnected

3. **Verify both devices are in the same room**:
   - Backend logs should show: `Joined room: room:6915d676583815220a9187ff`
   - Use `presence` event to confirm

---

**This fix completes the WebRTC implementation!** 🎉  
Audio calls should now work end-to-end.

