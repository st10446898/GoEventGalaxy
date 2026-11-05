package com.goevent.data

import com.goevent.core.model.UserProfile
import com.goevent.core.util.safeCall
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class InterestRow(@SerialName("category_id") val categoryId: String)

@Serializable
private data class InterestInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("category_id") val categoryId: String
)

class ProfileRepository {

    private val client = SupabaseClientManager.client

    /** Returns null when the user has no profile row yet. */
    suspend fun fetchProfile(userId: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeList<UserProfile>()
                .firstOrNull()
        }
    }

    suspend fun updateProfile(userId: String, fullName: String, avatarUrl: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            safeCall {
                client.postgrest["profiles"].update({
                    set("full_name", fullName.trim())
                    if (avatarUrl != null) set("avatar_url", avatarUrl)
                }) {
                    filter { eq("id", userId) }
                }
                Unit
            }
        }

    /** Uploads a profile picture to the user's own folder and returns its public URL. */
    suspend fun uploadAvatar(userId: String, jpegBytes: ByteArray): Result<String> =
        withContext(Dispatchers.IO) {
            safeCall {
                val path = "$userId/${System.currentTimeMillis()}.jpg"
                val bucket = client.storage.from("avatars")
                bucket.upload(path, jpegBytes)
                bucket.publicUrl(path)
            }
        }

    suspend fun fetchInterestIds(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        safeCall {
            client.postgrest["user_interests"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<InterestRow>()
                .map { it.categoryId }
        }
    }

    suspend fun saveInterests(userId: String, categoryIds: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            safeCall {
                client.postgrest["user_interests"].delete { filter { eq("user_id", userId) } }
                if (categoryIds.isNotEmpty()) {
                    client.postgrest["user_interests"]
                        .insert(categoryIds.distinct().map { InterestInsert(userId, it) })
                }
                Unit
            }
        }
}