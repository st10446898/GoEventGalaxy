package com.goevent.data.offline

import com.goevent.core.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/** Saves server data on the phone and queues changes made while offline. */
class OfflineStore(private val db: GoEventDatabase) {

    companion object {
        const val FAVORITE = "FAVORITE"
        const val NOTIFICATION_READ = "NOTIFICATION_READ"
    }

    private val json = Json { ignoreUnknownKeys = true }
    private val idList = ListSerializer(String.serializer())
    private val notificationList = ListSerializer(NotificationItem.serializer())

    fun pendingCount(): Flow<Int> = db.pendingDao().count()

    suspend fun <T> readCache(key: String, serializer: KSerializer<T>): T? = withContext(Dispatchers.IO) {
        val row = db.cacheDao().get(key) ?: return@withContext null
        runCatching { json.decodeFromString(serializer, row.json) }.getOrNull()
    }

    suspend fun <T> writeCache(key: String, serializer: KSerializer<T>, value: T): Unit = withContext(Dispatchers.IO) {
        db.cacheDao().put(CacheEntity(key, json.encodeToString(serializer, value), System.currentTimeMillis()))
    }

    /**
     * Online: fetch fresh data and save a copy.
     * Offline (network error): return the saved copy if there is one.
     * Any other error (for example, "not allowed") is returned as a failure.
     */
    suspend fun <T> networkWithCache(key: String, serializer: KSerializer<T>, fetch: suspend () -> T): Result<T> =
        try {
            val fresh = fetch()
            runCatching { writeCache(key, serializer, fresh) }
            Result.success(fresh)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cached = if (ConnectivityMonitor.isNetworkError(e)) readCache(key, serializer) else null
            if (cached != null) Result.success(cached) else Result.failure(e)
        }

    /** Updates the saved favourites list and queues the change for sync. */
    suspend fun queueFavorite(userId: String, eventId: String, isFavorite: Boolean): Unit = withContext(Dispatchers.IO) {
        val key = "favorites:$userId"
        val current = readCache(key, idList).orEmpty()
        val updated = if (isFavorite) (current + eventId).distinct() else current - eventId
        writeCache(key, idList, updated)
        db.pendingDao().deleteFor(FAVORITE, userId, eventId)
        db.pendingDao().insert(
            PendingActionEntity(
                type = FAVORITE, userId = userId, targetId = eventId,
                value = isFavorite.toString(), createdAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    /** Marks one notification read in the saved list and queues the change for sync. */
    suspend fun queueNotificationRead(userId: String, notificationId: String): Unit = withContext(Dispatchers.IO) {
        val key = "notifications:$userId"
        val items = readCache(key, notificationList)
        if (items != null) {
            writeCache(key, notificationList, items.map { if (it.id == notificationId) it.copy(isRead = true) else it })
        }
        db.pendingDao().deleteFor(NOTIFICATION_READ, userId, notificationId)
        db.pendingDao().insert(
            PendingActionEntity(
                type = NOTIFICATION_READ, userId = userId, targetId = notificationId,
                value = "", createdAt = System.currentTimeMillis()
            )
        )
        Unit
    }

    suspend fun queueAllNotificationsRead(userId: String) {
        val items = readCache("notifications:$userId", notificationList).orEmpty()
        items.filter { !it.isRead }.forEach { queueNotificationRead(userId, it.id) }
    }

    suspend fun pendingActions(): List<PendingActionEntity> = withContext(Dispatchers.IO) {
        db.pendingDao().all()
    }

    suspend fun removePending(id: Long): Unit = withContext(Dispatchers.IO) {
        db.pendingDao().delete(id)
    }
}