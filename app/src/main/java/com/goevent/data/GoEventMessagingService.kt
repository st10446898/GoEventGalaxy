package com.goevent.data

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.goevent.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives incoming FCM messages and token refresh events.
 * NOTE: I implemented permission checks (Android 13+ POST_NOTIFICATIONS) and channel routing 
 * so background notifications display cleanly and tap actions route back to MainActivity.
 */
class GoEventMessagingService : FirebaseMessagingService() {

    private val TAG = "GoEventFCMService"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken triggered. Refreshing token registration...")
        val uid = SupabaseClientManager.client.auth.currentUserOrNull()?.id ?: return
        scope.launch {
            runCatching {
                PushTokenRepository().saveToken(uid, token)
            }.onFailure {
                Log.e(TAG, "Failed to save refreshed FCM token: ${it.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received from server.")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted. Suppressing notification display.")
                return
            }
        }

        val title = message.data["title"] ?: message.notification?.title ?: "GoEvent"
        val body = message.data["body"] ?: message.notification?.body ?: return

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, "goevent_updates")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
            Log.i(TAG, "Successfully posted push notification: title='$title'")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while posting notification: ${e.message}")
        }
    }
}
