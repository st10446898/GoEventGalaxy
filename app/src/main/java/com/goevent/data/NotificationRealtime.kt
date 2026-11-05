package com.goevent.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** The part of a new notification row the app needs to show a message. */
@Serializable
private data class NewNotice(
    val title: String = "",
    val message: String = ""
)

/**
 * Listens for new notifications for the signed-in user while the app is open,
 * and shows each one as a short message. Start it after sign-in, stop it on sign-out.
 */
class NotificationRealtime(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var job: Job? = null
    private var channel: RealtimeChannel? = null

    fun start(userId: String) {
        stop()
        job = scope.launch {
            try {
                val realtime = SupabaseClientManager.client.realtime
                val newChannel = realtime.channel("notifications-$userId")
                channel = newChannel

                val inserts = newChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "notifications"
                    filter("user_id", FilterOperator.EQ, userId)
                }

                newChannel.subscribe()

                inserts.collect { action ->
                    val notice = action.decodeRecord<NewNotice>()
                    showMessage(notice.title.ifBlank { notice.message })
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Live updates are a bonus. The notifications screen still loads normally.
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        val oldChannel = channel
        channel = null
        if (oldChannel != null) {
            scope.launch {
                runCatching { SupabaseClientManager.client.realtime.removeChannel(oldChannel) }
            }
        }
    }

    private fun showMessage(text: String) {
        if (text.isBlank()) return
        mainHandler.post {
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        }
    }
}