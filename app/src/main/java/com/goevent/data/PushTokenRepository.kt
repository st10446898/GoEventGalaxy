package com.goevent.data

import android.util.Log
import com.goevent.core.util.safeCall
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRecord(
    @SerialName("user_id") val userId: String,
    val token: String,
    val platform: String = "android"
)

/**
 * Handles FCM push token lifecycle. 
 * NOTE: I set up upsert with onConflict="token" so device tokens update cleanly 
 * when Firebase rotates them or when a user switches accounts on the same device.
 */
class PushTokenRepository {
    private val TAG = "GoEventPushToken"
    private val client = SupabaseClientManager.client

    suspend fun fetchFcmToken(): String? = try {
        val token = FirebaseMessaging.getInstance().token.await()
        Log.d(TAG, "Successfully retrieved FCM registration token.")
        token
    } catch (e: Exception) {
        Log.w(TAG, "Failed to retrieve FCM token: ${e.message}")
        null
    }

    suspend fun saveToken(userId: String, token: String): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            Log.d(TAG, "Upserting FCM token for user $userId to Supabase device_tokens")
            val record = DeviceTokenRecord(userId = userId, token = token)
            client.postgrest["device_tokens"]
                .upsert(listOf(record)) {
                    onConflict = "token"
                }
            Log.i(TAG, "FCM token saved successfully for user $userId")
            Unit
        }
    }

    suspend fun deleteToken(token: String): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            Log.d(TAG, "Deleting FCM token on sign-out")
            client.postgrest["device_tokens"]
                .delete { filter { eq("token", token) } }
            Log.i(TAG, "FCM token removed successfully from Supabase")
            Unit
        }
    }
}
