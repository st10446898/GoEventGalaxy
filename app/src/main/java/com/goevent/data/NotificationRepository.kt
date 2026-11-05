package com.goevent.data

import com.goevent.core.model.NotificationItem
import com.goevent.core.util.safeCall
import com.goevent.data.offline.ConnectivityMonitor
import com.goevent.data.offline.OfflineStore
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

class NotificationRepository(private val store: OfflineStore) {

    private val client = SupabaseClientManager.client
    private val list = ListSerializer(NotificationItem.serializer())

    suspend fun fetchNotifications(userId: String): Result<List<NotificationItem>> = withContext(Dispatchers.IO) {
        store.networkWithCache("notifications:$userId", list) {
            client.postgrest["notifications"]
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<NotificationItem>()
        }
    }

    /** Sends the read flag to the server. Used by the sync manager. */
    suspend fun markReadOnline(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["notifications"]
                .update({ set("is_read", true) }) { filter { eq("id", notificationId) } }
            Unit
        }
    }

    /** Marks one notification read. Offline, the change is queued and sent later. */
    suspend fun markAsRead(notificationId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val online = markReadOnline(notificationId)
        val uid = SupabaseClientManager.client.auth.currentUserOrNull()?.id
        if (online.isSuccess || !ConnectivityMonitor.isNetworkError(online.exceptionOrNull()) || uid == null) {
            online
        } else {
            runCatching { store.queueNotificationRead(uid, notificationId) }
        }
    }

    suspend fun markAllReadOnline(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["notifications"]
                .update({ set("is_read", true) }) {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
            Unit
        }
    }

    /** Marks everything read. Offline, each unread item is queued and sent later. */
    suspend fun markAllAsRead(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val online = markAllReadOnline(userId)
        if (online.isSuccess || !ConnectivityMonitor.isNetworkError(online.exceptionOrNull())) {
            online
        } else {
            runCatching { store.queueAllNotificationsRead(userId) }
        }
    }
}