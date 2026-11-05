package com.goevent.core.util

import android.util.Log

/**
 * Client-side preview of booking maths. I designed this to mirror our Postgres RPC function 
 * (atomic_create_booking) precisely so the UI stays in sync before sending the payload to Supabase.
 * Rule: 500 GoPoints = R50, so 1 point = 10 cents.
 */
object PricingCalculator {
    private const val TAG = "GoEventPricing"
    const val CENTS_PER_POINT = 10
    const val MAX_TICKETS_PER_BOOKING = 10

    data class Quote(
        val subtotalCents: Int,
        val pointsRedeemed: Int,
        val discountCents: Int,
        val totalCents: Int
    ) {
        val isFree: Boolean get() = totalCents == 0
    }

    fun subtotal(priceCents: Int, quantity: Int): Int =
        priceCents.coerceAtLeast(0) * quantity.coerceAtLeast(0)

    /** The most tickets a user may pick for a ticket type with [available] seats left. */
    fun maxQuantity(available: Int): Int = minOf(MAX_TICKETS_PER_BOOKING, available.coerceAtLeast(0))

    /** Keeps a requested quantity inside 1..max (or 0 when sold out). */
    fun clampQuantity(requested: Int, available: Int): Int {
        val max = maxQuantity(available)
        return if (max == 0) 0 else requested.coerceIn(1, max)
    }

    fun pointsToCents(points: Int): Int = points.coerceAtLeast(0) * CENTS_PER_POINT

    /** Points can never exceed the balance, nor the value of the order. */
    fun maxRedeemablePoints(balance: Int, subtotalCents: Int): Int =
        minOf(balance.coerceAtLeast(0), subtotalCents.coerceAtLeast(0) / CENTS_PER_POINT)

    fun quote(priceCents: Int, quantity: Int, requestedPoints: Int, balance: Int): Quote {
        val subtotal = subtotal(priceCents, quantity)
        val points = requestedPoints.coerceAtLeast(0)
            .coerceAtMost(maxRedeemablePoints(balance, subtotal))
        val discount = pointsToCents(points)
        val total = subtotal - discount
        
        Log.d(TAG, "Calculated quote: subtotal=${subtotal}c, pointsRedeemed=$points, discount=${discount}c, total=${total}c")
        
        return Quote(
            subtotalCents = subtotal,
            pointsRedeemed = points,
            discountCents = discount,
            totalCents = total
        )
    }

    /** Mirrors the server rule: 10 points per R10 paid, minimum 10 for a paid booking. */
    fun estimatedPointsEarned(finalCents: Int): Int =
        if (finalCents <= 0) 0 else maxOf(10, finalCents / 1000 * 10)
}
