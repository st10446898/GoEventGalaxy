package com.goevent.core.model

enum class EventSort { DATE, PRICE_LOW, PRICE_HIGH, TOP_RATED }

/** Everything the Home / Explore / Search screens can ask the events table for. */
data class EventQuery(
    val categoryId: String? = null,
    val city: String? = null,
    val search: String = "",
    val featuredOnly: Boolean = false,
    val upcomingOnly: Boolean = true,
    val dateFrom: String? = null, // yyyy-MM-dd
    val dateTo: String? = null,   // yyyy-MM-dd
    val sort: EventSort = EventSort.DATE,
    val limit: Int? = null
)
