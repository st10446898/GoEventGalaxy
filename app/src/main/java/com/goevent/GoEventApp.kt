package com.goevent

import android.app.Application
import android.content.Context
import com.goevent.data.AuthRepository
import com.goevent.data.BookingRepository
import com.goevent.data.DataStoreManager
import com.goevent.data.EventRepository
import com.goevent.data.GoPointsRepository
import com.goevent.data.NotificationPreferencesRepository
import com.goevent.data.NotificationRealtime
import com.goevent.data.NotificationRepository
import com.goevent.data.ProfileRepository
import com.goevent.data.PushTokenRepository
import com.goevent.data.offline.ConnectivityMonitor
import com.goevent.data.offline.GoEventDatabase
import com.goevent.data.offline.OfflineStore
import com.goevent.data.offline.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class GoEventApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ConnectivityMonitor.start(this)
        container = AppContainer(applicationContext)
        container.sync.start(appScope)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "goevent_updates",
                "GoEvent updates",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Start live notifications when someone signs in, save push token, stop them when they sign out.
        appScope.launch {
            container.authRepository.authState.collect { userId ->
                if (userId == null) {
                    container.notificationRealtime.stop()
                } else {
                    container.notificationRealtime.start(userId)
                    runCatching {
                        val token = container.pushTokenRepository.fetchFcmToken()
                        if (token != null) container.pushTokenRepository.saveToken(userId, token)
                    }
                }
            }
        }
    }
}

/** Creates the app's shared repositories once. ViewModels read them from here. */
class AppContainer(context: Context) {
    val dataStore = DataStoreManager(context)

    val database by lazy { GoEventDatabase.create(context) }
    val offline by lazy { OfflineStore(database) }

    val profileRepository by lazy { ProfileRepository() }
    val authRepository by lazy { AuthRepository(profileRepository) }
    val eventRepository by lazy { EventRepository(offline) }
    val bookingRepository by lazy { BookingRepository(offline) }
    val goPointsRepository by lazy { GoPointsRepository() }
    val notificationRepository by lazy { NotificationRepository(offline) }
    val notificationRealtime by lazy { NotificationRealtime(context) }
    val pushTokenRepository by lazy { PushTokenRepository() }
    val notificationPreferencesRepository by lazy { NotificationPreferencesRepository() }

    val sync by lazy {
        SyncManager(
            store = offline,
            pushFavorite = { userId, eventId, isFavorite -> eventRepository.setFavoriteOnline(userId, eventId, isFavorite) },
            pushNotificationRead = { id -> notificationRepository.markReadOnline(id) }
        )
    }
}
