package com.goevent.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val SIGN_IN = "sign_in"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val CHANGE_PASSWORD = "change_password"
    const val INTERESTS = "interests"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val TICKETS = "tickets"
    const val SAVED = "saved"
    const val PROFILE = "profile"
    const val GOPOINTS = "gopoints"
    const val GOPOINTS_HISTORY = "gopoints_history"
    const val REWARDS = "rewards"
    const val ACHIEVEMENTS = "achievements"
    const val EDIT_PROFILE = "edit_profile"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val LANGUAGE = "language"
    const val HELP = "help"
    const val ABOUT = "about"

    const val EVENT = "event/{id}"
    const val MAP = "map/{id}"
    const val BOOKING_FLOW = "booking/{eventId}"
    const val TICKET = "ticket/{id}"
    const val QR = "qr/{id}"

    fun event(id: String) = "event/$id"
    fun map(id: String) = "map/$id"
    fun bookingFlow(eventId: String) = "booking/$eventId"
    fun ticket(id: String) = "ticket/$id"
    fun qr(id: String) = "qr/$id"
    fun explore(categoryId: String?) = if (categoryId == null) EXPLORE else "explore?category=$categoryId"

    val publicRoutes = setOf(SPLASH, ONBOARDING, SIGN_IN, REGISTER, FORGOT_PASSWORD)
    val bottomRoutes = setOf(HOME, EXPLORE, TICKETS, SAVED, PROFILE)
}
