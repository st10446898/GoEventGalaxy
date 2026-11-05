package com.goevent.core.util

/**
 * Turns the image_url stored in the database into a loadable https URL.
 * Full URLs are used as-is; bare file names (e.g. event_001.jpg) are looked up in the
 * matching public Supabase Storage bucket.
 */
object ImageUrlResolver {
    const val EVENT_BUCKET = "event-images"
    const val REWARD_BUCKET = "reward-images"

    fun resolve(raw: String?, bucket: String, supabaseUrl: String): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("https://")) return value
        if (value.startsWith("http://")) return "https://" + value.removePrefix("http://")
        val base = supabaseUrl.trim().trimEnd('/')
        return "$base/storage/v1/object/public/$bucket/${value.trimStart('/')}"
    }

    fun event(raw: String?, supabaseUrl: String): String? = resolve(raw, EVENT_BUCKET, supabaseUrl)

    fun reward(raw: String?, supabaseUrl: String): String? = resolve(raw, REWARD_BUCKET, supabaseUrl)
}