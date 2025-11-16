# Child Chat Flow

## Overview
- Added `ChildChatScreen` to fetch the child's dedicated chat room via `GET /messages/room/child/{childId}`.
- Navigation now routes child users to `child_chat`, which automatically opens the existing `ChatRoomScreen` after the room is resolved.
- `ChatRoomScreen` infers the sender model (`User` vs `Child`) from the logged-in user so messages and audio uploads use the correct backend role.

## Manual Test Plan
1. Sign in with a child account.
2. From the child bottom navigation bar, tap **Chat**.
   - Expect a quick loading card followed by an automatic transition into the chat room.
   - If the backend has not created the room, the screen shows a retry prompt.
3. Send a text message.
   - Verify it appears immediately and backend receives `senderModel = "Child"`.
4. Send an audio message (optional, once audio upload is wired to storage).
   - Ensure recording permissions are granted and the bubble appears with playback controls.
5. Press the back button from the chat room.
   - You should return to the child home screen.

## Notes
- If the backend does not upload audio files yet, playback will continue to report HTTP errors. Storage integration is still required server-side.
- Parent routes and navigation remain unchanged; the new behavior only applies when the logged-in user has the `CHILD` role.

