package com.goevent.data.offline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SyncStatus(val syncing: Boolean = false, val message: String? = null)

/**
 * Sends the queued offline changes to the server.
 * Runs when the phone comes back online, and every 30 seconds while changes are waiting.
 * A change is removed once the server accepts it. If the server rejects it (not a network problem),
 * it is removed too, because retrying would never work.
 */
class SyncManager(
    private val store: OfflineStore,
    private val pushFavorite: suspend (userId: String, eventId: String, isFavorite: Boolean) -> Result<Unit>,
    private val pushNotificationRead: suspend (notificationId: String) -> Result<Unit>
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    val pendingCount: Flow<Int> = store.pendingCount()

    fun start(scope: CoroutineScope) {
        scope.launch {
            ConnectivityMonitor.isOnline.filter { it }.collect { syncNow() }
        }
        scope.launch {
            while (isActive) {
                delay(RETRY_MILLIS)
                syncNow()
            }
        }
    }

    suspend fun syncNow() {
        if (!ConnectivityMonitor.isOnline.value) return
        mutex.withLock {
            val pending = store.pendingActions()
            if (pending.isEmpty()) return

            _status.value = SyncStatus(syncing = true)
            var done = 0
            var dropped = 0
            var waiting = false

            for (action in pending) {
                val result = when (action.type) {
                    OfflineStore.FAVORITE ->
                        pushFavorite(action.userId, action.targetId, action.value == "true")
                    OfflineStore.NOTIFICATION_READ ->
                        pushNotificationRead(action.targetId)
                    else -> Result.success(Unit)
                }
                val error = result.exceptionOrNull()
                when {
                    error == null -> {
                        store.removePending(action.id)
                        done++
                    }
                    ConnectivityMonitor.isNetworkError(error) -> {
                        waiting = true
                        break
                    }
                    else -> {
                        store.removePending(action.id)
                        dropped++
                    }
                }
            }

            val parts = mutableListOf<String>()
            if (done > 0) parts += "Synced $done change${if (done == 1) "" else "s"}."
            if (dropped > 0) parts += "$dropped change${if (dropped == 1) "" else "s"} could not be saved and were removed."
            if (waiting) parts += "Some changes are still waiting for a connection."
            _status.value = SyncStatus(syncing = false, message = parts.joinToString(" ").takeIf { it.isNotEmpty() })
        }
    }

    companion object {
        private const val RETRY_MILLIS = 30_000L
    }
}