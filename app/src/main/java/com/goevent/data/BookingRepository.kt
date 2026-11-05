package com.goevent.data

import android.util.Log
import com.goevent.core.model.TicketDetail
import com.goevent.core.util.safeCall
import com.goevent.data.offline.OfflineStore
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private const val TICKET_COLUMNS =
    "id,booking_id,ticket_type_id,ticket_reference,qr_token,holder_name,status,created_at," +
            "ticket_types(name)," +
            "bookings(booking_reference,event_id,events(id,title,image_url,venue,city,event_date,start_time))"

/**
 * Booking and payment always need the internet, so seats are never double-sold.
 * Tickets are saved on the phone, so My Tickets and QR codes work offline.
 * 
 * NOTE: I implemented this repository to call our Postgres atomic RPC functions directly 
 * via Supabase PostgREST, ensuring race conditions on ticket stock are handled entirely on the server side.
 */
class BookingRepository(private val store: OfflineStore) {

    private val TAG = "GoEventBookingRepo"
    private val client = SupabaseClientManager.client
    private val ticketList = ListSerializer(TicketDetail.serializer())

    /** Server-side booking: the database checks stock, price and GoPoints. */
    suspend fun createBooking(
        eventId: String,
        ticketTypeId: String,
        quantity: Int,
        redeemPoints: Int = 0
    ): Result<BookingResult> = withContext(Dispatchers.IO) {
        safeCall {
            Log.d(TAG, "Requesting atomic booking creation for event=$eventId, qty=$quantity, points=$redeemPoints")
            val params = buildJsonObject {
                put("p_event_id", eventId)
                put("p_ticket_type_id", ticketTypeId)
                put("p_quantity", quantity)
                put("p_redeem_points", redeemPoints)
            }
            val json = parse(client.postgrest.rpc("atomic_create_booking", params).data)
            val result = BookingResult(
                bookingId = json.text("booking_id"),
                bookingReference = json.text("booking_reference"),
                finalAmountCents = json.text("final_amount_cents").toIntOrNull() ?: 0,
                isFree = json.text("is_free").toBoolean(),
                status = json.text("status").ifEmpty { "Pending" }
            )
            Log.i(TAG, "Booking created successfully: ref=${result.bookingReference}, free=${result.isFree}")
            result
        }
    }

    /** outcome is SUCCESS, FAILED or CANCELLED. The result status can also be EXPIRED. */
    suspend fun verifySimulatedPayment(bookingId: String, outcome: String): Result<PaymentVerificationResult> =
        withContext(Dispatchers.IO) {
            safeCall {
                Log.d(TAG, "Verifying payment for booking=$bookingId with outcome=$outcome")
                val params = buildJsonObject {
                    put("p_booking_id", bookingId)
                    put("p_outcome", outcome)
                }
                val json = parse(client.postgrest.rpc("verify_simulated_payment", params).data)
                val result = PaymentVerificationResult(
                    status = json.text("status"),
                    pointsEarned = json.text("points_earned").toIntOrNull() ?: 0,
                    ticketCount = json.text("ticket_count").toIntOrNull() ?: 0
                )
                Log.i(TAG, "Payment verified: status=${result.status}, pointsEarned=${result.pointsEarned}")
                result
            }
        }

    suspend fun fetchTicketDetails(ticketId: String): Result<TicketDetail?> = withContext(Dispatchers.IO) {
        safeCall {
            Log.d(TAG, "Fetching ticket details for id=$ticketId")
            client.postgrest["tickets"]
                .select(Columns.raw(TICKET_COLUMNS)) {
                    filter { eq("id", ticketId) }
                }
                .decodeSingleOrNull<TicketDetail>()
        }
    }

    suspend fun fetchUserTickets(userId: String): Result<List<TicketDetail>> = withContext(Dispatchers.IO) {
        store.networkWithCache("tickets:$userId", ticketList) {
            Log.d(TAG, "Fetching user tickets from network for userId=$userId")
            client.postgrest["tickets"]
                .select(Columns.raw(TICKET_COLUMNS)) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TicketDetail>()
        }
    }

    suspend fun fetchTicketById(ticketId: String): Result<TicketDetail?> = fetchTicketDetails(ticketId)

    suspend fun validateTicket(ticketId: String): Result<TicketValidationResult> = withContext(Dispatchers.IO) {
        safeCall {
            val detail = fetchTicketDetails(ticketId).getOrNull()
            if (detail == null) {
                TicketValidationResult("INVALID", "Ticket not found", null, null)
            } else {
                TicketValidationResult("VALID", "Valid ticket", detail.bookings?.events?.title, detail.holderName)
            }
        }
    }

    private fun parse(raw: String): JsonObject =
        Json.parseToJsonElement(raw).jsonObject

    private fun JsonObject.text(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()
}

data class BookingResult(
    val bookingId: String,
    val bookingReference: String,
    val finalAmountCents: Int,
    val isFree: Boolean,
    val status: String
)

data class PaymentVerificationResult(
    val status: String,
    val pointsEarned: Int,
    val ticketCount: Int
)

data class TicketValidationResult(
    val status: String,
    val message: String,
    val eventTitle: String?,
    val holderName: String?
)
