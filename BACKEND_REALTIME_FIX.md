# Fix Real-Time Messages - Backend Solution

## Problem

When Android sends messages via REST API (fallback), they're saved to DB but **NOT broadcasted via Socket.IO**, so other users don't see them in real-time.

## Solution: Share Socket.IO Server

### Option 1: Export ChatGateway Instance (Simplest)

**1. Update `chat.gateway.ts`:**

```typescript
import { Injectable } from '@nestjs/common';  // ADD

@Injectable()  // ADD
@WebSocketGateway({
  cors: {
    origin: '*',
    credentials: true,
  },
})
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  // ... rest of your code stays the same ...

  // ADD THIS METHOD:
  /**
   * Broadcast a message to a room (can be called from anywhere)
   */
  broadcastMessage(roomId: string, message: any) {
    this.server.to(`room:${roomId}`).emit('newMessage', message);
    console.log(`📨 Broadcasted message to room:${roomId}`);
  }
}
```

**2. Update `message.controller.ts`:**

```typescript
import { ChatGateway } from './chat.gateway';  // ADD

@ApiTags('Messages')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
@Controller('messages')
export class MessageController {
  constructor(
    private readonly messageService: MessageService,
    private readonly cloudinaryService: CloudinaryService,
    private readonly chatGateway: ChatGateway,  // ADD THIS
  ) {}

  /**
   * Send a text message
   */
  @Post('room/:roomId/text')
  async sendText(
    @Param('roomId') roomId: string,
    @Body() body: { text: string; senderModel: 'User' | 'Child'; senderId: string },
    @CurrentUser() currentUser: any,
  ) {
    this.assertObjectId(roomId, 'roomId');
    // ... validation ...
    
    const msg = await this.messageService.sendText(
      {
        roomId,
        text: body.text,
        senderModel: body.senderModel,
        senderId: body.senderId,
      },
      currentUser,
    );

    // ✅ BROADCAST VIA SOCKET.IO
    this.chatGateway.broadcastMessage(roomId, msg);

    return msg;
  }

  /**
   * Send an audio message
   */
  @Post('room/:roomId/audio')
  async sendAudio(...) {
    // ... existing code to save audio ...

    const msg = await this.messageService.sendAudio(...);

    // ✅ BROADCAST VIA SOCKET.IO
    this.chatGateway.broadcastMessage(roomId, msg);

    return msg;
  }
}
```

**3. Update `message.module.ts`:**

```typescript
import { ChatGateway } from './chat.gateway';  // ADD

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Room.name, schema: RoomSchema },
      { name: Message.name, schema: MessageSchema },
      { name: Child.name, schema: ChildSchema },
      { name: User.name, schema: UserSchema },
    ]),
  ],
  controllers: [MessageController],
  providers: [
    MessageService,
    CloudinaryService,
    ChatGateway,  // ✅ ADD THIS - Makes ChatGateway available for injection
  ],
  exports: [MessageService],
})
export class MessageModule {}
```

---

### Option 2: Create EventEmitter Service (More Scalable)

If you want a cleaner architecture:

**1. Create `events.service.ts`:**

```typescript
import { Injectable } from '@nestjs/common';
import { EventEmitter2 } from '@nestjs/event-emitter';

@Injectable()
export class EventsService {
  constructor(private eventEmitter: EventEmitter2) {}

  emitNewMessage(roomId: string, message: any) {
    this.eventEmitter.emit('message.created', { roomId, message });
  }
}
```

**2. Update `chat.gateway.ts` to listen:**

```typescript
import { OnEvent } from '@nestjs/event-emitter';

export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  // ... existing code ...

  @OnEvent('message.created')
  handleMessageCreated(payload: { roomId: string; message: any }) {
    this.server.to(`room:${payload.roomId}`).emit('newMessage', payload.message);
    console.log(`📨 Broadcasted message to room:${payload.roomId}`);
  }
}
```

**3. Update `message.service.ts`:**

```typescript
import { EventsService } from './events.service';

export class MessageService {
  constructor(
    // ... existing deps ...
    private readonly eventsService: EventsService,
  ) {}

  async sendText(dto: SendTextDto, currentUser: any): Promise<any> {
    // ... existing code to create msg ...

    // ✅ EMIT EVENT
    this.eventsService.emitNewMessage(dto.roomId, msg.toObject());

    return msg.toObject();
  }

  async sendAudio(dto: SendAudioDto, currentUser: any): Promise<any> {
    // ... existing code ...

    // ✅ EMIT EVENT
    this.eventsService.emitNewMessage(dto.roomId, msg.toObject());

    return msg.toObject();
  }
}
```

---

## Recommended: Option 1 (Simplest)

Option 1 is the quickest fix. Just:
1. Add `@Injectable()` to `ChatGateway`
2. Add `broadcastMessage()` method to `ChatGateway`
3. Inject `ChatGateway` into `MessageController`
4. Call `chatGateway.broadcastMessage()` after saving messages
5. Add `ChatGateway` to `providers` in `MessageModule`

---

## Testing After Fix

**1. Send message from Android**
**2. Check backend logs:**
```
📨 Broadcasted message to room:6915d676583815220a9187ff
```

**3. Check Android Logcat:**
```
ChatSocket: 📨 Received newMessage event
ChatSocket: 💬 Regular chat message received
ChatRoomScreen: ✅ Adding message to list
```

**4. Message appears instantly on other device!** ✨

---

## Why This Works

Before:
```
Android → REST API → Save to DB → ❌ No broadcast
```

After:
```
Android → REST API → Save to DB → ✅ Broadcast via Socket.IO → All clients receive
```

Now both paths work:
- **WebSocket path**: `ChatGateway.onSendText()` → broadcasts
- **REST API path**: `MessageController.sendText()` → broadcasts

---

## Alternative: Make WebSocket More Reliable

Instead of fixing the backend, you could also fix Android to always use WebSocket:

**In `ChatRoomScreen.kt`:**

```kotlin
suspend fun sendMessage() {
    val userId = currentUser?.id
    if (messageInput.isBlank() || userId.isNullOrBlank()) return
    
    isSending = true
    val trimmed = messageInput.trim()
    
    // Ensure socket is connected
    if (!ChatSocketManager.connectionState.value) {
        ChatSocketManager.connect()
        delay(1000) // Wait for connection
    }
    
    val socketResult = ChatSocketManager.sendTextMessage(
        roomId = roomId,
        text = trimmed,
        senderModel = resolvedSenderModel,
        senderId = userId
    )
    
    if (socketResult.isSuccess) {
        messageInput = ""
        isSending = false
        return
    }
    
    // Only use REST as fallback if socket truly failed
    Log.w(CHAT_ROOM_TAG, "Socket send failed, using REST fallback")
    val restResult = ApiService.sendChatTextMessage(...)
    // ... handle rest result ...
}
```

But the backend fix is better because it ensures real-time updates work regardless of which path is used!

