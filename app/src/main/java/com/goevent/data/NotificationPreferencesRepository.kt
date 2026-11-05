package com.goevent.data

import android.util.Log
import com.goevent.core.util.safeCall
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    @SerialName("user_id") val userId: String,
    @SerialName("events_enabled") val eventsEnabled: Boolean = true,
    @SerialName("rewards_enabled") val rewardsEnabled: Boolean = true,
    @SerialName("bookings_enabled") val bookingsEnabled: Boolean = true
)

/**
 * Manages user notification opt-in/opt-out preferences stored in Supabase.
 * NOTE: I created this so users can toggle event, reward, and booking notification categories
 * in Settings, which our Edge Function checks before dispatching FCM payloads.
 */
class NotificationPreferencesRepository {
    private val TAG = "GoEventPrefsRepo"
    private val client = SupabaseClientManager.client

    suspend fun fetchPreferences(userId: String): Result<NotificationPreferences> = withContext(Dispatchers.IO) {
        safeCall {
            Log.d(TAG, "Fetching notification preferences for user $userId")
            val existing = client.postgrest["notification_preferences"]
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<NotificationPreferences>()

            if (existing != null) {
                Log.d(TAG, "Preferences loaded successfully.")
                existing
            } else {
                Log.i(TAG, "No preferences row found; initializing default preferences for user $userId")
                val defaultPrefs = NotificationPreferences(userId = userId)
                client.postgrest["notification_preferences"]
                    .insert(defaultPrefs)
                defaultPrefs
            }
        }
    }

    suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            Log.d(TAG, "Upserting notification preferences for user ${preferences.userId}")
            client.postgrest["notification_preferences"]
                .upsert(listOf(preferences)) {
                    onConflict = "user_id"
                }
            Log.i(TAG, "Notification preferences updated successfully.")
            Unit
        }
    }
}
