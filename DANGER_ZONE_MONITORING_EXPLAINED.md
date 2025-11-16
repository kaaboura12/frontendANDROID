# 🛡️ Danger Zone Monitoring - How It Works

## 📋 Overview

Your danger zone system is **backend-driven** - the server automatically detects when children enter/exit zones and sends notifications. Here's exactly how it works:

---

## 🔄 The Complete Flow

### 1️⃣ **Child's Location is Updated**

The **child device** (phone/watch) sends its GPS location to the backend:

```bash
PATCH /children/{childId}/location
Body: {
  "lat": 33.5731,
  "lng": -7.6598
}
```

**Who sends this?**
- Child's device (Android phone or watch app)
- Sent automatically in the background
- Usually every 30-60 seconds (configurable)

---

### 2️⃣ **Backend Automatically Checks Danger Zones**

When the location update is received, your backend **automatically**:

✅ Gets all **active** danger zones for that child  
✅ Calculates distance from child to each zone center (using Haversine formula)  
✅ Determines if child is INSIDE or OUTSIDE each zone  
✅ Compares with previous state (was inside/outside)  
✅ Detects **state changes** (entry or exit)  

**This happens in your backend code:**
```typescript
// From your danger-zone.service.ts
async checkDangerZones(child: Child): Promise<DangerZoneEvent[]> {
  // 1. Get all active zones for this child
  const dangerZones = await this.dangerZoneModel.find({
    parent: { $in: parentIds },
    status: DangerZoneStatus.ACTIVE,
    $or: [
      { children: { $size: 0 } },    // Applies to all children
      { children: child._id }         // Specifically for this child
    ]
  })
  
  // 2. For each zone, calculate distance
  const distance = this.calculateDistance(
    child.location.lat,
    child.location.lng,
    zone.center.lat,
    zone.center.lng
  )
  
  // 3. Check if inside zone
  const isInsideZone = distance <= zone.radiusMeters
  
  // 4. Detect state change (entry/exit)
  if (isInsideZone && !wasInsideZone) {
    // CHILD ENTERED ZONE! 🚨
    if (zone.notifyOnEntry) {
      // Create event and send notification
    }
  } else if (!isInsideZone && wasInsideZone) {
    // CHILD EXITED ZONE! ✅
    if (zone.notifyOnExit) {
      // Create event and send notification
    }
  }
}
```

---

### 3️⃣ **Event is Created**

If a state change is detected, a **DangerZoneEvent** record is created:

```json
{
  "_id": "event123",
  "child": "childId",
  "dangerZone": "zoneId",
  "type": "ENTER",  // or "EXIT"
  "location": {
    "lat": 33.5731,
    "lng": -7.6598
  },
  "notificationSent": false,
  "createdAt": "2025-11-15T10:30:00Z"
}
```

---

### 4️⃣ **Notifications are Sent**

The backend sends notifications to the **parent(s)** via:

**📧 Email** (HTML formatted):
```
Subject: ⚠️ Child Alert - Emma entered Danger Zone

Dear John,

Your child Emma Smith has ENTERED the danger zone "School Area" 
at 10:30 AM on November 15, 2025.

Location: 33.5731, -7.6598

Best regards,
Your Child Safety System
```

**📱 SMS** (plain text):
```
ALERT: Emma entered "School Area" at 10:30 AM
Location: 33.5731, -7.6598
```

---

## 🔔 Current Notification System

### ✅ What's Working Now:

| Method | Status | Sent To |
|--------|--------|---------|
| Email | ✅ Working | Parent's email |
| SMS | ✅ Working | Parent's phone number |
| Push Notifications | ❌ Not Implemented | Android app |

**Important:** Your backend currently sends **Email + SMS**, but does **NOT** send push notifications to the Android app.

---

## 📱 What's Missing: Real-Time App Notifications

Your Android app **does NOT receive** real-time notifications when a child enters/exits a zone.

### Current Behavior:
❌ No in-app push notifications  
❌ No real-time alerts  
❌ Parent must manually refresh to see events  

### What You See Now:
- Parent opens Location screen
- Taps a danger zone marker
- Sees list of events (entry/exit history)
- Must refresh manually to see new events

---

## 🚀 How to Add Real-Time Notifications to Android

You have **3 options** to implement real-time notifications:

---

### **Option 1: Firebase Cloud Messaging (FCM)** ⭐ RECOMMENDED

**Pros:**
✅ Industry standard  
✅ Reliable push notifications  
✅ Works even when app is closed  
✅ Free for most use cases  
✅ Official Android support  

**How it works:**
1. Android app registers for FCM and gets a token
2. Token is sent to your backend and stored in User model
3. When danger zone event occurs, backend sends FCM message
4. Android app receives notification (even if app is closed)
5. User taps notification → app opens to Location screen

**What you need to implement:**

**A. Backend Changes:**
```typescript
// 1. Add FCM token to User schema
fcmToken: { type: String, default: null }

// 2. Add FCM token registration endpoint
@Patch('users/fcm-token')
async updateFcmToken(@CurrentUser() user, @Body() { token }) {
  await this.userModel.updateOne(
    { _id: user.id },
    { fcmToken: token }
  )
}

// 3. Send FCM notification when event occurs
import * as admin from 'firebase-admin'

async sendDangerZoneNotification(event: DangerZoneEvent) {
  const parent = await this.userModel.findById(zone.parent)
  
  if (parent.fcmToken) {
    await admin.messaging().send({
      token: parent.fcmToken,
      notification: {
        title: '⚠️ Child Alert',
        body: `${child.firstName} ${event.type === 'ENTER' ? 'entered' : 'exited'} ${zone.name}`
      },
      data: {
        type: 'danger_zone_event',
        eventId: event._id,
        zoneId: zone._id,
        childId: child._id
      }
    })
  }
}
```

**B. Android Changes:**
```kotlin
// 1. Add FCM dependency to app/build.gradle.kts
implementation("com.google.firebase:firebase-messaging:23.3.1")

// 2. Create FCM service
class DamFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle notification
        val type = remoteMessage.data["type"]
        if (type == "danger_zone_event") {
            showDangerZoneNotification(remoteMessage)
        }
    }
    
    override fun onNewToken(token: String) {
        // Send token to backend
        sendTokenToServer(token)
    }
}

// 3. Show notification
fun showDangerZoneNotification(message: RemoteMessage) {
    val notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(message.notification?.title)
        .setContentText(message.notification?.body)
        .setSmallIcon(R.drawable.ic_notification)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()
    
    notificationManager.notify(notificationId, notification)
}
```

---

### **Option 2: Socket.IO Real-Time Events** 🔌

**Pros:**
✅ You already have Socket.IO in your app (for chat)  
✅ Real-time updates  
✅ No third-party service needed  

**Cons:**
❌ Only works when app is open  
❌ Doesn't work when app is closed/killed  
❌ Battery intensive if always connected  

**How it works:**
1. Parent opens app → connects to Socket.IO
2. Backend emits `danger_zone_event` when child enters/exits
3. Android app listens for event and shows in-app alert

**What you need to implement:**

**A. Backend:**
```typescript
// In danger-zone.service.ts
async checkDangerZones(child: Child) {
  // ... existing logic ...
  
  // Emit event via Socket.IO
  this.socketService.emitToUser(zone.parent, 'danger_zone_event', {
    eventId: event._id,
    type: event.type,
    childName: child.firstName,
    zoneName: zone.name,
    location: child.location
  })
}
```

**B. Android:**
```kotlin
// In LocationScreen or DamApplication
ChatSocketManager.socket?.on("danger_zone_event") { args ->
    val event = gson.fromJson(args[0].toString(), DangerZoneEventDto::class.java)
    
    // Show in-app notification
    showInAppAlert("${event.childName} ${event.type.lowercase()} ${event.zoneName}")
    
    // Refresh zones
    loadDangerZones()
}
```

---

### **Option 3: Polling** 🔄

**Pros:**
✅ Simple to implement  
✅ No additional services needed  

**Cons:**
❌ Not real-time (delay of 30-60 seconds)  
❌ Battery intensive  
❌ More server load  
❌ Only works when Location screen is open  

**How it works:**
1. While Location screen is open, poll every 30 seconds
2. Check for new events since last check
3. Show notification if new events found

**What you need to implement:**

**A. Android:**
```kotlin
// In LocationScreen
LaunchedEffect(Unit) {
    while (isActive) {
        delay(30_000) // Poll every 30 seconds
        
        // Check for new events
        val newEvents = ApiService.getRecentDangerZoneEvents(lastCheckTimestamp)
        
        if (newEvents.isNotEmpty()) {
            newEvents.forEach { event ->
                showInAppAlert("${event.childName} ${event.type} ${event.zoneName}")
            }
            lastCheckTimestamp = System.currentTimeMillis()
        }
    }
}
```

**B. Backend (optional):**
```typescript
// Add endpoint to get recent events
@Get('danger-zones/events/recent')
async getRecentEvents(@Query('since') timestamp: string) {
  return this.dangerZoneEventModel.find({
    createdAt: { $gt: new Date(timestamp) },
    parent: currentUser.id
  })
  .populate('child')
  .populate('dangerZone')
  .limit(50)
}
```

---

## 🎯 Recommendation

For your use case, I recommend **Option 1: FCM** because:

✅ **Real-time** notifications even when app is closed  
✅ **Reliable** and industry-standard  
✅ **Battery efficient** (system-managed)  
✅ **Professional** user experience  
✅ **Free** for most usage levels  

You already have:
- ✅ Backend logic to detect entry/exit
- ✅ Event creation working
- ✅ Email + SMS notifications working

You just need to add:
- 📱 FCM setup (1-2 hours)
- 🔧 Token registration (30 minutes)
- 📨 Send FCM message from backend (30 minutes)

**Total time: ~3 hours** to have professional push notifications! 🚀

---

## 🧪 Testing the Current System

### **Test 1: Verify Backend Detection**

1. Create a danger zone on the map
2. Note the zone center and radius
3. Use a child device to send location update:
   ```bash
   curl -X PATCH http://your-server/children/{childId}/location \
     -H "Authorization: Bearer $CHILD_TOKEN" \
     -d '{"lat": 33.5731, "lng": -7.6598}'
   ```
4. Check backend logs - should see danger zone check
5. Parent should receive email + SMS

### **Test 2: View Events in App**

1. Open Location screen
2. Tap danger zone marker
3. See event history with entry/exit records
4. Verify child name, timestamp, location

---

## 📊 Summary

| Component | Status | Notes |
|-----------|--------|-------|
| Zone Creation | ✅ Working | Via Android app |
| Zone Display | ✅ Working | Circles on map |
| Backend Detection | ✅ Working | Automatic on location update |
| Event Recording | ✅ Working | Saved to database |
| Email Notifications | ✅ Working | Sent to parent |
| SMS Notifications | ✅ Working | Sent to parent |
| Event History | ✅ Working | View in app |
| Push Notifications | ❌ Not Implemented | Need FCM |
| Real-time Updates | ❌ Not Implemented | Need FCM or Socket.IO |

---

## 🚀 Next Steps

To complete the danger zone system:

1. **Choose notification method** (FCM recommended)
2. **Set up Firebase** project (if FCM)
3. **Implement FCM** in Android app
4. **Add FCM token** endpoint to backend
5. **Send FCM messages** when events occur
6. **Test end-to-end**

Want me to help you implement FCM push notifications? 🔔

