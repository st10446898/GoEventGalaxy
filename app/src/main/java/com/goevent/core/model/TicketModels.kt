package com.goevent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A ticket together with its event and ticket-type details (one request, joined by the database). */
@Serializable
data class TicketDetail(
    val id: String,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("ticket_type_id") val ticketTypeId: String,
    @SerialName("ticket_reference") val ticketReference: String,
    @SerialName("qr_token") val qrToken: String,
    @SerialName("holder_name") val holderName: String,
    val status: String = "Valid",
    @SerialName("created_at") val createdAt: String? = null,
    val bookings: TicketBookingInfo? = null,
    @SerialName("ticket_types") val ticketType: TicketTypeName? = null
)

@Serializable
data class TicketBookingInfo(
    @SerialName("booking_reference") val bookingReference: String,
    @SerialName("event_id") val eventId: String,
    val events: TicketEventInfo? = null
)

@Serializable
data class TicketEventInfo(
    val id: String,
    val title: String,
    @SerialName("image_url") val imageUrl: String = "",
    val venue: String = "",
    val city: String = "",
    @SerialName("event_date") val eventDate: String = "",
    @SerialName("start_time") val startTime: String = ""
)

@Serializable
data class TicketTypeName(val name: String = "")