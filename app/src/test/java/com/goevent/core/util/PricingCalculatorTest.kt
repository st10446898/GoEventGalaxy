package com.goevent.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingCalculatorTest {

    @Test
    fun subtotal_multipliesPriceByQuantity() {
        assertEquals(50_000, PricingCalculator.subtotal(25_000, 2))
    }

    @Test
    fun subtotal_neverGoesNegative() {
        assertEquals(0, PricingCalculator.subtotal(-100, 3))
        assertEquals(0, PricingCalculator.subtotal(100, -3))
    }

    @Test
    fun pointsConversion_500PointsIsR50() {
        assertEquals(5_000, PricingCalculator.pointsToCents(500))
    }

    @Test
    fun clampQuantity_respectsAvailabilityAndTenTicketCap() {
        assertEquals(3, PricingCalculator.clampQuantity(8, available = 3))
        assertEquals(10, PricingCalculator.clampQuantity(25, available = 50))
        assertEquals(1, PricingCalculator.clampQuantity(-4, available = 50))
        assertEquals(0, PricingCalculator.clampQuantity(2, available = 0))
    }

    @Test
    fun quote_appliesPointsDiscount() {
        // 2 x R250 = R500; redeem 500 points = R50 off -> R450
        val quote = PricingCalculator.quote(priceCents = 25_000, quantity = 2, requestedPoints = 500, balance = 1_000)
        assertEquals(50_000, quote.subtotalCents)
        assertEquals(500, quote.pointsRedeemed)
        assertEquals(5_000, quote.discountCents)
        assertEquals(45_000, quote.totalCents)
        assertFalse(quote.isFree)
    }

    @Test
    fun quote_cannotSpendMoreThanBalance() {
        val quote = PricingCalculator.quote(25_000, 1, requestedPoints = 500, balance = 100)
        assertEquals(100, quote.pointsRedeemed)
        assertEquals(1_000, quote.discountCents)
        assertEquals(24_000, quote.totalCents)
    }

    @Test
    fun quote_cannotDiscountMoreThanTheOrderIsWorth() {
        // R10 ticket, user tries to spend 500 points (R50): only 100 points (R10) are used
        val quote = PricingCalculator.quote(1_000, 1, requestedPoints = 500, balance = 500)
        assertEquals(100, quote.pointsRedeemed)
        assertEquals(0, quote.totalCents)
        assertTrue(quote.isFree)
    }

    @Test
    fun quote_ignoresNegativePointRequests() {
        val quote = PricingCalculator.quote(10_000, 1, requestedPoints = -50, balance = 500)
        assertEquals(0, quote.pointsRedeemed)
        assertEquals(10_000, quote.totalCents)
    }

    @Test
    fun estimatedPointsEarned_matchesServerRule() {
        assertEquals(250, PricingCalculator.estimatedPointsEarned(25_000)) // R250
        assertEquals(10, PricingCalculator.estimatedPointsEarned(500))     // minimum for a paid booking
        assertEquals(0, PricingCalculator.estimatedPointsEarned(0))        // free booking
    }
}
