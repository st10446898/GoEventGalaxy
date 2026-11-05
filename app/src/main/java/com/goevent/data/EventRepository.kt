package com.goevent.data

import com.goevent.core.model.Category
import com.goevent.core.model.Event
import com.goevent.core.model.EventQuery
import com.goevent.core.model.EventSort
import com.goevent.core.model.TicketType
import com.goevent.core.util.Validators
import com.goevent.core.util.safeCall
import com.goevent.data.offline.ConnectivityMonitor
import com.goevent.data.offline.OfflineStore
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

@Serializable
private data class CityRow(val city: String)

@Serializable
private data class FavoriteRow(@SerialName("event_id") val eventId: String)

@Serializable
private data class FavoriteInsert(
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String
)

/** Reads are saved on the phone, so browsing works offline. Saving favourites also works offline. */
class EventRepository(private val store: OfflineStore) {

    private val client = SupabaseClientManager.client
    private val categoryList = ListSerializer(Category.serializer())
    private val eventList = ListSerializer(Event.serializer())
    private val typeList = ListSerializer(TicketType.serializer())
    private val idList = ListSerializer(String.serializer())
    private val cityList = ListSerializer(String.serializer())

    suspend fun fetchCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        store.networkWithCache("categories", categoryList) {
            client.postgrest["categories"]
                .select { order("name", Order.ASCENDING) }
                .decodeList<Category>()
        }
    }

    suspend fun fetchEvents(query: EventQuery = EventQuery()): Result<List<Event>> = withContext(Dispatchers.IO) {
        store.networkWithCache("events:$query", eventList) {
            val from = query.dateFrom ?: if (query.upcomingOnly) todayIso() else null
            val term = Validators.sanitiseSearch(query.search)

            client.postgrest["events"]
                .select {
                    filter {
                        eq("is_published", true)
                        query.categoryId?.let { eq("category_id", it) }
                        query.city?.let { eq("city", it) }
                        if (query.featuredOnly) eq("is_featured", true)
                        from?.let { gte("event_date", it) }
                        query.dateTo?.let { lte("event_date", it) }
                        if (term.isNotEmpty()) {
                            or {
                                ilike("title", "%$term%")
                                ilike("venue", "%$term%")
                                ilike("city", "%$term%")
                            }
                        }
                    }
                    when (query.sort) {
                        EventSort.DATE -> {
                            order("event_date", Order.ASCENDING)
                            order("start_time", Order.ASCENDING)
                        }
                        EventSort.PRICE_LOW -> order("starting_price_cents", Order.ASCENDING)
                        EventSort.PRICE_HIGH -> order("starting_price_cents", Order.DESCENDING)
                        EventSort.TOP_RATED -> order("rating", Order.DESCENDING)
                    }
                    query.limit?.let { limit(it.toLong()) }
                }
                .decodeList<Event>()
        }
    }

    suspend fun fetchEventById(eventId: String): Result<Event> = withContext(Dispatchers.IO) {
        store.networkWithCache("event:$eventId", Event.serializer()) {
            client.postgrest["events"]
                .select { filter { eq("id", eventId) } }
                .decodeList<Event>()
                .firstOrNull() ?: throw NoSuchElementException("Event not found")
        }
    }

    suspend fun fetchEventsByIds(ids: List<String>): Result<List<Event>> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext Result.success(emptyList())
        store.networkWithCache("saved:" + ids.sorted().joinToString(","), eventList) {
            client.postgrest["events"]
                .select { filter { isIn("id", ids) } }
                .decodeList<Event>()
        }
    }

    /** The cities that currently have events (for the location filter). */
    suspend fun fetchCities(): Result<List<String>> = withContext(Dispatchers.IO) {
        store.networkWithCache("cities", cityList) {
            client.postgrest["events"]
                .select(Columns.raw("city")) { filter { eq("is_published", true) } }
                .decodeList<CityRow>()
                .map { it.city }
                .distinct()
                .sorted()
        }
    }

    suspend fun fetchTicketTypes(eventId: String): Result<List<TicketType>> = withContext(Dispatchers.IO) {
        store.networkWithCache("types:$eventId", typeList) {
            client.postgrest["ticket_types"]
                .select {
                    filter { eq("event_id", eventId) }
                    order("price_cents", Order.ASCENDING)
                }
                .decodeList<TicketType>()
        }
    }

    suspend fun fetchFavoriteEventIds(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        store.networkWithCache("favorites:$userId", idList) {
            client.postgrest["favorites"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<FavoriteRow>()
                .map { it.eventId }
        }
    }

    /**
     * Saves or removes a favourite. If there is no connection, the change is saved on the phone
     * and sent later, so this still returns success.
     */
    suspend fun setFavorite(userId: String, eventId: String, isFavorite: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            val online = setFavoriteOnline(userId, eventId, isFavorite)
            if (online.isSuccess || !ConnectivityMonitor.isNetworkError(online.exceptionOrNull())) {
                return@withContext online
            }
            runCatching { store.queueFavorite(userId, eventId, isFavorite) }
        }

    /** Sends the change straight to the server. Used by the sync manager. */
    suspend fun setFavoriteOnline(userId: String, eventId: String, isFavorite: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            safeCall {
                if (isFavorite) {
                    try {
                        client.postgrest["favorites"].insert(FavoriteInsert(userId, eventId))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Already saved (unique violation) counts as success
                        val text = e.message.orEmpty()
                        if (!text.contains("23505") && !text.contains("duplicate", ignoreCase = true)) throw e
                    }
                } else {
                    client.postgrest["favorites"].delete {
                        filter {
                            eq("user_id", userId)
                            eq("event_id", eventId)
                        }
                    }
                }
                Unit
            }
        }

    private fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}