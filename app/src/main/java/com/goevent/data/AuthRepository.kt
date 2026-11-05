package com.goevent.data

import com.goevent.core.model.UserProfile
import com.goevent.core.util.UserFacingException
import com.goevent.core.util.safeCall
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Sign-up, sign-in, password and session handling, all done by Supabase Auth. */
class AuthRepository(private val profiles: ProfileRepository) {

    private val auth get() = SupabaseClientManager.client.auth
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUserId: String? get() = auth.currentUserOrNull()?.id
    val currentEmail: String? get() = auth.currentUserOrNull()?.email

    /** Emits the signed-in user's id (or null) now and whenever the login state changes. */
    val authState: Flow<String?> = SupabaseClientManager.client.auth.sessionStatus
        .map { (it as? SessionStatus.Authenticated)?.session?.user?.id }
        .distinctUntilChanged()

    suspend fun register(email: String, password: String, fullName: String): Result<UserProfile> = safeCall {
        val name = fullName.trim()
        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put("full_name", name) }
        }
        if (auth.currentSessionOrNull() == null) {
            throw UserFacingException("Account created. Check your email to confirm it, then sign in.")
        }
        loadProfile(requireUserId())
    }

    suspend fun signIn(email: String, password: String): Result<UserProfile> = safeCall {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        loadProfile(requireUserId())
    }

    suspend fun signInWithGoogle(): Result<UserProfile> = safeCall {
        auth.signInWith(Google)
        loadProfile(requireUserId())
    }

    /** Called at app start: null means nobody is signed in. */
    suspend fun restoreSession(): Result<UserProfile?> = safeCall {
        auth.awaitInitialization()
        if (currentUserId == null) null else loadProfile(requireUserId())
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = safeCall {
        auth.resetPasswordForEmail(email.trim())
        Unit
    }

    /** Checks the current password by signing in again, then sets the new one. */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> = safeCall {
        val email = currentEmail ?: throw UserFacingException("You are not signed in.")
        auth.signInWith(Email) {
            this.email = email
            this.password = currentPassword
        }
        auth.updateUser { password = newPassword }
        Unit
    }

    fun signOut() {
        scope.launch {
            runCatching {
                val token = PushTokenRepository().fetchFcmToken()
                if (token != null) {
                    PushTokenRepository().deleteToken(token)
                }
            }
            runCatching { auth.signOut() }
        }
    }

    // --- internals -------------------------------------------------------------------

    private fun requireUserId(): String =
        currentUserId ?: throw UserFacingException("You are not signed in.")

    private suspend fun loadProfile(uid: String): UserProfile =
        profiles.fetchProfile(uid).getOrThrow()
            ?: throw UserFacingException("Your profile could not be loaded. Please try again.")
}
