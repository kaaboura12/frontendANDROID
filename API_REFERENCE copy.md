# SOS Alert API Reference

## Base URL
```
http://localhost:3005/sos
```

## Authentication
All endpoints require JWT Bearer token authentication.

**Header Format:**
```
Authorization: Bearer <your_jwt_token>
```

---

## Endpoints

### 1. Trigger SOS Alert

Creates a new SOS alert for a child and notifies all linked parents in real-time.

**Endpoint:** `POST /sos/alert`

**Authentication:** Required

**Request Body:**
```typescript
{
  childId: string;     // MongoDB ObjectId of the child
  location: {
    lat: number;       // Latitude (-90 to 90)
    lng: number;       // Longitude (-180 to 180)
  };
}
```

**Example Request:**
```bash
curl -X POST http://localhost:3005/sos/alert \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "507f1f77bcf86cd799439011",
    "location": {
      "lat": 37.7749,
      "lng": -122.4194
    }
  }'
```

**Success Response (201):**
```json
{
  "success": true,
  "message": "SOS alert sent to all parents",
  "alert": {
    "alertId": "507f1f77bcf86cd799439012",
    "childId": "507f1f77bcf86cd799439011",
    "childName": "John Doe",
    "location": {
      "lat": 37.7749,
      "lng": -122.4194
    },
    "timestamp": "2025-11-15T12:00:00.000Z",
    "status": "ACTIVE",
    "triggeredBy": {
      "id": "507f1f77bcf86cd799439013",
      "name": "Jane Smith"
    }
  }
}
```

**Error Responses:**

| Status Code | Description | Response Body |
|------------|-------------|---------------|
| 400 | Invalid child ID format | `{ "statusCode": 400, "message": "Invalid child ID" }` |
| 400 | Validation failed (missing fields) | `{ "statusCode": 400, "message": ["childId should not be empty", ...] }` |
| 400 | Not authorized for this child | `{ "statusCode": 400, "message": "You are not authorized to trigger an alert for this child" }` |
| 401 | Missing or invalid token | `{ "statusCode": 401, "message": "Unauthorized" }` |
| 404 | Child not found | `{ "statusCode": 404, "message": "Child not found" }` |

**Side Effects:**
- Creates a new `SOSAlert` document in MongoDB
- Emits `sos_alert` event via WebSocket to all parents (main + linked)
- Logs alert notification in console

**Notes:**
- Only the main parent or linked parents can trigger alerts for a child
- All connected parent devices receive real-time notifications via WebSocket
- Future: Will also send push notifications, SMS, and email

---

### 2. Get My Alerts

Retrieves all SOS alerts for children linked to the authenticated parent.

**Endpoint:** `GET /sos/alerts`

**Authentication:** Required

**Query Parameters:** None

**Example Request:**
```bash
curl -X GET http://localhost:3005/sos/alerts \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Success Response (200):**
```json
[
  {
    "_id": "507f1f77bcf86cd799439012",
    "child": {
      "_id": "507f1f77bcf86cd799439011",
      "firstName": "John",
      "lastName": "Doe"
    },
    "triggeredBy": {
      "_id": "507f1f77bcf86cd799439013",
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane@example.com"
    },
    "location": {
      "lat": 37.7749,
      "lng": -122.4194
    },
    "status": "ACTIVE",
    "acknowledgedBy": null,
    "acknowledgedAt": null,
    "createdAt": "2025-11-15T12:00:00.000Z",
    "updatedAt": "2025-11-15T12:00:00.000Z"
  },
  {
    "_id": "507f1f77bcf86cd799439015",
    "child": {
      "_id": "507f1f77bcf86cd799439011",
      "firstName": "John",
      "lastName": "Doe"
    },
    "triggeredBy": {
      "_id": "507f1f77bcf86cd799439014",
      "firstName": "Bob",
      "lastName": "Johnson",
      "email": "bob@example.com"
    },
    "location": {
      "lat": 37.8044,
      "lng": -122.2712
    },
    "status": "ACKNOWLEDGED",
    "acknowledgedBy": {
      "_id": "507f1f77bcf86cd799439013",
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane@example.com"
    },
    "acknowledgedAt": "2025-11-15T11:55:00.000Z",
    "createdAt": "2025-11-15T11:50:00.000Z",
    "updatedAt": "2025-11-15T11:55:00.000Z"
  }
]
```

**Error Responses:**

| Status Code | Description | Response Body |
|------------|-------------|---------------|
| 401 | Missing or invalid token | `{ "statusCode": 401, "message": "Unauthorized" }` |

**Notes:**
- Returns up to 50 most recent alerts
- Sorted by creation date (newest first)
- Includes alerts for all children the parent is linked to (main or linked parent)
- Empty array if no alerts found

---

### 3. Acknowledge Alert

Marks an SOS alert as acknowledged by the current parent.

**Endpoint:** `PATCH /sos/alerts/:id/acknowledge`

**Authentication:** Required

**URL Parameters:**
- `id` (string, required): MongoDB ObjectId of the alert

**Request Body:** None

**Example Request:**
```bash
curl -X PATCH http://localhost:3005/sos/alerts/507f1f77bcf86cd799439012/acknowledge \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**Success Response (200):**
```json
{
  "success": true,
  "alert": {
    "_id": "507f1f77bcf86cd799439012",
    "child": "507f1f77bcf86cd799439011",
    "triggeredBy": "507f1f77bcf86cd799439013",
    "location": {
      "lat": 37.7749,
      "lng": -122.4194
    },
    "status": "ACKNOWLEDGED",
    "acknowledgedBy": "507f1f77bcf86cd799439014",
    "acknowledgedAt": "2025-11-15T12:05:00.000Z",
    "createdAt": "2025-11-15T12:00:00.000Z",
    "updatedAt": "2025-11-15T12:05:00.000Z"
  }
}
```

**Error Responses:**

| Status Code | Description | Response Body |
|------------|-------------|---------------|
| 400 | Invalid alert ID format | `{ "statusCode": 400, "message": "Invalid alert ID" }` |
| 400 | Not authorized for this alert | `{ "statusCode": 400, "message": "You are not authorized to acknowledge this alert" }` |
| 401 | Missing or invalid token | `{ "statusCode": 401, "message": "Unauthorized" }` |
| 404 | Alert not found | `{ "statusCode": 404, "message": "Alert not found" }` |
| 404 | Child not found | `{ "statusCode": 404, "message": "Child not found" }` |

**Side Effects:**
- Updates the alert status to `ACKNOWLEDGED`
- Sets `acknowledgedBy` to the current user's ID
- Sets `acknowledgedAt` to current timestamp
- Emits `sos_acknowledged` event via WebSocket to all other parents

**Notes:**
- Only parents linked to the child can acknowledge alerts
- Other parents are notified when an alert is acknowledged
- An alert can be acknowledged by multiple parents (last one wins)

---

## WebSocket Events

### Connection

Connect to the WebSocket server with JWT authentication:

```typescript
import io from 'socket.io-client';

const socket = io('http://localhost:3005', {
  auth: {
    token: 'YOUR_JWT_TOKEN'
  }
});

socket.on('connect', () => {
  console.log('Connected to WebSocket server');
});

socket.on('disconnect', () => {
  console.log('Disconnected from WebSocket server');
});
```

### Event: `sos_alert`

**Direction:** Server → Client

**Trigger:** When an SOS alert is triggered via `POST /sos/alert`

**Recipients:** All parents linked to the child (main + linked parents)

**Payload:**
```typescript
{
  alertId: string;         // MongoDB ObjectId
  childId: string;         // MongoDB ObjectId
  childName: string;       // "FirstName LastName"
  location: {
    lat: number;
    lng: number;
  };
  timestamp: string;       // ISO 8601 datetime
  status: "ACTIVE";
  triggeredBy: {
    id: string;           // MongoDB ObjectId
    name: string;         // "FirstName LastName"
  };
}
```

**Example:**
```javascript
socket.on('sos_alert', (data) => {
  console.log('🚨 SOS ALERT:', data);
  // {
  //   alertId: "507f1f77bcf86cd799439012",
  //   childId: "507f1f77bcf86cd799439011",
  //   childName: "John Doe",
  //   location: { lat: 37.7749, lng: -122.4194 },
  //   timestamp: "2025-11-15T12:00:00.000Z",
  //   status: "ACTIVE",
  //   triggeredBy: {
  //     id: "507f1f77bcf86cd799439013",
  //     name: "Jane Smith"
  //   }
  // }
  
  // Show urgent notification
  showNotification({
    title: 'SOS Alert!',
    message: `Emergency alert for ${data.childName}`,
    urgent: true
  });
  
  // Navigate to map
  navigateToMap(data.location);
});
```

---

### Event: `sos_acknowledged`

**Direction:** Server → Client

**Trigger:** When an alert is acknowledged via `PATCH /sos/alerts/:id/acknowledge`

**Recipients:** All parents linked to the child EXCEPT the one who acknowledged

**Payload:**
```typescript
{
  alertId: string;         // MongoDB ObjectId
  acknowledgedBy: string;  // MongoDB ObjectId of acknowledging parent
  acknowledgedAt: string;  // ISO 8601 datetime
}
```

**Example:**
```javascript
socket.on('sos_acknowledged', (data) => {
  console.log('✅ Alert acknowledged:', data);
  // {
  //   alertId: "507f1f77bcf86cd799439012",
  //   acknowledgedBy: "507f1f77bcf86cd799439014",
  //   acknowledgedAt: "2025-11-15T12:05:00.000Z"
  // }
  
  // Update UI to show acknowledgment
  updateAlertStatus(data.alertId, 'ACKNOWLEDGED');
  
  // Show info notification
  showNotification({
    title: 'Alert Acknowledged',
    message: 'Another parent has seen the alert',
    urgent: false
  });
});
```

---

## Data Models

### SOSAlert Schema

```typescript
{
  _id: ObjectId;           // Auto-generated
  child: ObjectId;         // Reference to Child document
  triggeredBy: ObjectId;   // Reference to User (parent) document
  location: {
    lat: number;          // Latitude (-90 to 90)
    lng: number;          // Longitude (-180 to 180)
  };
  status: string;          // "ACTIVE" | "ACKNOWLEDGED" | "RESOLVED"
  acknowledgedAt?: Date;   // When alert was acknowledged
  acknowledgedBy?: ObjectId; // Reference to User who acknowledged
  createdAt: Date;         // Auto-generated by timestamps
  updatedAt: Date;         // Auto-generated by timestamps
}
```

### Alert Status Values

| Status | Description |
|--------|-------------|
| `ACTIVE` | Alert is active and needs attention |
| `ACKNOWLEDGED` | Alert has been seen by at least one parent |
| `RESOLVED` | Alert has been resolved (future feature) |

---

## Error Handling

### Common Error Response Format

```typescript
{
  statusCode: number;
  message: string | string[];
  error?: string;
}
```

### HTTP Status Codes

| Code | Meaning | When It Occurs |
|------|---------|----------------|
| 200 | OK | Successful GET request |
| 201 | Created | Successful POST request (alert created) |
| 400 | Bad Request | Invalid input, validation error, or authorization failure |
| 401 | Unauthorized | Missing or invalid JWT token |
| 404 | Not Found | Resource (alert, child) not found |
| 500 | Internal Server Error | Unexpected server error |

---

## Rate Limiting

Currently, no rate limiting is implemented. Consider implementing rate limiting for production:
- Max 10 SOS alerts per child per hour
- Max 100 API requests per user per minute

---

## Security Considerations

### Authorization Rules

1. **Trigger Alert:**
   - User must be the main parent OR a linked parent of the child
   
2. **View Alerts:**
   - User can only view alerts for children they are linked to
   
3. **Acknowledge Alert:**
   - User must be the main parent OR a linked parent of the child

### Data Privacy

- Alerts contain sensitive location data
- Only authorized parents can access alert data
- WebSocket connections are authenticated via JWT
- Location data should be transmitted over HTTPS/WSS in production

### Best Practices

- Always use HTTPS in production
- Rotate JWT secrets regularly
- Implement rate limiting
- Log all alert triggers for audit trail
- Consider implementing alert retention policy (e.g., delete after 30 days)

---

## Testing

### Manual Testing Script

Save as `test-sos.sh`:

```bash
#!/bin/bash

# Configuration
API_URL="http://localhost:3005"
TOKEN="YOUR_JWT_TOKEN"
CHILD_ID="507f1f77bcf86cd799439011"

echo "1. Triggering SOS Alert..."
curl -X POST $API_URL/sos/alert \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"childId\": \"$CHILD_ID\",
    \"location\": {
      \"lat\": 37.7749,
      \"lng\": -122.4194
    }
  }" | jq

echo -e "\n2. Getting My Alerts..."
curl -X GET $API_URL/sos/alerts \
  -H "Authorization: Bearer $TOKEN" | jq

echo -e "\n3. Acknowledging Alert (replace ALERT_ID)..."
ALERT_ID="REPLACE_WITH_ACTUAL_ALERT_ID"
curl -X PATCH $API_URL/sos/alerts/$ALERT_ID/acknowledge \
  -H "Authorization: Bearer $TOKEN" | jq
```

### Postman Collection

Import this collection into Postman:

```json
{
  "info": {
    "name": "SOS Alert API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Trigger SOS Alert",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"childId\": \"{{child_id}}\",\n  \"location\": {\n    \"lat\": 37.7749,\n    \"lng\": -122.4194\n  }\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "{{base_url}}/sos/alert",
          "host": ["{{base_url}}"],
          "path": ["sos", "alert"]
        }
      }
    },
    {
      "name": "Get My Alerts",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          }
        ],
        "url": {
          "raw": "{{base_url}}/sos/alerts",
          "host": ["{{base_url}}"],
          "path": ["sos", "alerts"]
        }
      }
    },
    {
      "name": "Acknowledge Alert",
      "request": {
        "method": "PATCH",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{jwt_token}}"
          }
        ],
        "url": {
          "raw": "{{base_url}}/sos/alerts/{{alert_id}}/acknowledge",
          "host": ["{{base_url}}"],
          "path": ["sos", "alerts", "{{alert_id}}", "acknowledge"]
        }
      }
    }
  ],
  "variable": [
    {
      "key": "base_url",
      "value": "http://localhost:3005"
    },
    {
      "key": "jwt_token",
      "value": ""
    },
    {
      "key": "child_id",
      "value": ""
    },
    {
      "key": "alert_id",
      "value": ""
    }
  ]
}
```

---

## Changelog

### Version 1.0.0 (2025-11-15)
- Initial release
- POST /sos/alert endpoint
- GET /sos/alerts endpoint
- PATCH /sos/alerts/:id/acknowledge endpoint
- WebSocket support for real-time notifications
- User tracking for multi-device support

---

## Support & Contact

For questions or issues:
1. Check server logs for detailed error messages
2. Verify JWT token is valid
3. Ensure child relationships are properly set up
4. Test WebSocket connection separately
5. Review MongoDB data for inconsistencies

