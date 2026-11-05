package com.goevent.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "goevent_settings")

/** Saves app preferences on the device (theme, language, onboarding, notification switches). */
class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_THEME = stringPreferencesKey("app_theme") // "System", "Light", "Dark"
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_LANGUAGE = stringPreferencesKey("app_language") // "English", "Afrikaans", "isiZulu", "isiXhosa"
        val KEY_NOTIFY_EVENTS = booleanPreferencesKey("notify_events")
        val KEY_NOTIFY_REWARDS = booleanPreferencesKey("notify_rewards")
        val KEY_NOTIFY_BOOKINGS = booleanPreferencesKey("notify_bookings")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { it[KEY_THEME] ?: "System" }
    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "English" }
    val notifyEventsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFY_EVENTS] ?: true }
    val notifyRewardsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFY_REWARDS] ?: true }
    val notifyBookingsFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_NOTIFY_BOOKINGS] ?: true }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setNotifyEvents(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_EVENTS] = enabled }
    }

    suspend fun setNotifyRewards(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_REWARDS] = enabled }
    }

    suspend fun setNotifyBookings(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_BOOKINGS] = enabled }
    }
}