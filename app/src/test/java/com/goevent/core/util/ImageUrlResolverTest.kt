package com.goevent.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlResolverTest {
    private val base = "https://example.supabase.co"

    @Test
    fun fullHttpsUrl_isKept() {
        assertEquals("https://cdn.site/a.jpg", ImageUrlResolver.event("https://cdn.site/a.jpg", base))
    }

    @Test
    fun httpUrl_isUpgradedToHttps() {
        assertEquals("https://cdn.site/a.jpg", ImageUrlResolver.event("http://cdn.site/a.jpg", base))
    }

    @Test
    fun bareFileName_pointsAtTheEventBucket() {
        assertEquals(
            "https://example.supabase.co/storage/v1/object/public/event-images/event_001.jpg",
            ImageUrlResolver.event("event_001.jpg", "$base/")
        )
    }

    @Test
    fun rewardFileName_pointsAtTheRewardBucket() {
        assertEquals(
            "https://example.supabase.co/storage/v1/object/public/reward-images/reward_01.jpg",
            ImageUrlResolver.reward("/reward_01.jpg", base)
        )
    }

    @Test
    fun blankValues_giveNull() {
        assertNull(ImageUrlResolver.event(null, base))
        assertNull(ImageUrlResolver.event("   ", base))
    }
}
