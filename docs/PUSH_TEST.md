# GoEvent Push Notifications Testing Guide

Follow these numbered steps to test push notifications on your physical Android device (Android 13+):

1. **Sign In & Grant Permission**:
   - Open the GoEvent app, sign in with your account.
   - When prompted for notification permissions, tap **Allow**.
   - Check the Supabase Table Editor (`device_tokens` table) to confirm a row has been inserted with your user ID and FCM token.

2. **Background / Closed Test**:
   - Put the app in the background, or close it completely by swiping it away from recent apps.

3. **Insert Notification**:
   - In the Supabase Table Editor (`notifications` table), insert a new row with:
     - `user_id`: Your user UUID
     - `title`: "Test Event Reminder"
     - `message`: "Your event starts in 1 hour!"
     - `type`: "event_update"

4. **Verify Notification**:
   - Expect a system notification to appear on your phone within ~10 seconds. Tapping it opens the app (`MainActivity`).

5. **Test Preferences (Settings Switches)**:
   - Go to Settings in GoEvent and turn **Event updates** off.
   - Insert another notification row with `type: "event_update"`.
   - **Expected**: No notification should be delivered.

6. **Sign Out**:
   - Go to Profile > Sign out.
   - **Expected**: Your device token row should be automatically deleted from `device_tokens`.

---
### Logcat Filter & Expected Errors
- **Logcat Filter**: `GoEvent` or `FirebaseMessaging` or `EdgeFunction`
- **Expected Success Logs**: FCM token received, token upserted successfully, FCM message sent (HTTP 200).
- **Expected Error Warnings**: `SecurityException` if notification permission was denied; `UNREGISTERED` cleaned up automatically if token is stale.
