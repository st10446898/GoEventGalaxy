package com.goevent.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    val email: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("gopoints_balance") val gopointsBalance: Int = 0,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Category(
    val id: String = "",
    val name: String = "",
    @SerialName("icon_key") val iconKey: String = "",
    @SerialName("icon_name") val iconName: String = "Star",
    @SerialName("color_hex") val colorHex: String = "#8B5CF6"
)

@Serializable
data class Event(
    val id: String = "",
    @SerialName("organizer_id") val organizerId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("image_url") val imageUrl: String? = null,
    val venue: String = "",
    val city: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("event_date") val eventDate: String = "",
    @SerialName("start_time") val startTime: String = "",
    @SerialName("end_time") val endTime: String = "",
    @SerialName("base_price_cents") val basePriceCents: Int = 0,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    val popularity: Int = 0,
    val rating: Double = 5.0,
    @SerialName("rating_count") val ratingCount: Int = 0,
    @SerialName("starting_price_cents") val startingPriceCents: Int = 0,
    val categories: Category? = null
)

@Serializable
data class TicketType(
    val id: String = "",
    @SerialName("event_id") val eventId: String = "",
    val name: String = "",
    @SerialName("price_cents") val priceCents: Int = 0,
    @SerialName("available_quantity") val availableQuantity: Int = 0
)

@Serializable
data class Booking(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("event_id") val eventId: String = "",
    @SerialName("booking_reference") val bookingReference: String = "",
    @SerialName("total_price_cents") val totalPriceCents: Int = 0,
    @SerialName("points_redeemed") val pointsRedeemed: Int = 0,
    @SerialName("discount_cents") val discountCents: Int = 0,
    @SerialName("final_paid_cents") val finalPaidCents: Int = 0,
    val status: String = "CONFIRMED",
    @SerialName("payment_method") val paymentMethod: String = "SECURE_PAYMENT",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Ticket(
    val id: String = "",
    @SerialName("booking_id") val bookingId: String = "",
    @SerialName("ticket_type_id") val ticketTypeId: String = "",
    @SerialName("ticket_reference") val ticketReference: String = "",
    @SerialName("qr_token") val qrToken: String = "",
    @SerialName("holder_name") val holderName: String = "",
    val status: String = "VALID",
    @SerialName("created_at") val createdAt: String = "",
    val bookings: BookingDetail? = null
)

@Serializable
data class BookingDetail(
    @SerialName("booking_reference") val bookingReference: String = "",
    @SerialName("event_id") val eventId: String = "",
    val events: Event? = null
)

@Serializable
data class TicketTypeShort(
    val name: String = ""
)

@Serializable
data class GoPointsHistoryItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val points: Int = 0,
    @SerialName("transaction_type") val transactionType: String = "",
    val reason: String = "",
    @SerialName("booking_id") val bookingId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class Reward(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("points_cost") val pointsCost: Int = 0,
    @SerialName("discount_cents") val discountCents: Int = 0,
    val icon: String = "Star",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class RewardRedemption(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("reward_id") val rewardId: String = "",
    @SerialName("redemption_code") val redemptionCode: String = "",
    @SerialName("redeemed_at") val redeemedAt: String = "",
    val status: String = "ACTIVE",
    val rewards: Reward? = null
)

@Serializable
data class Achievement(
    val id: String = "",
    val key: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("icon_key") val iconKey: String = "",
    @SerialName("icon_name") val iconName: String = "Star",
    val threshold: Int = 0,
    @SerialName("requirement_threshold") val requirementThreshold: Int = 0,
    @SerialName("reward_points") val rewardPoints: Int = 0,
    @SerialName("current_progress") val currentProgress: Int = 0,
    @SerialName("is_unlocked") val isUnlocked: Boolean = false
)

@Serializable
data class UserAchievement(
    @SerialName("user_id") val userId: String = "",
    @SerialName("achievement_id") val achievementId: String = "",
    @SerialName("unlocked_at") val unlockedAt: String = "",
    @SerialName("current_progress") val currentProgress: Int = 0,
    @SerialName("is_unlocked") val isUnlocked: Boolean = true,
    val achievements: Achievement? = null
)

@Serializable
data class NotificationItem(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val message: String = "",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("related_id") val relatedId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
