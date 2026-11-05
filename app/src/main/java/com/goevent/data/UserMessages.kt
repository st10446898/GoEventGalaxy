package com.goevent.data

import com.goevent.core.util.UserFacingException
import java.io.IOException

private val serverMessagePatterns = listOf(
    Regex("Only \\d+ tickets? available"),
    Regex("Insufficient GoPoints balance"),
    Regex("Quantity must be greater than zero"),
    Regex("You can book at most \\d+ tickets at a time"),
    Regex("Invalid GoPoints amount"),
    Regex("This event has already taken place"),
    Regex("Event not found or unavailable"),
    Regex("Selected ticket type does not exist for this event"),
    Regex("This booking can no longer be paid"),
    Regex("Booking not found or access denied"),
    Regex("Reward not found or unavailable"),
    Regex("Profile not found")
)

/** Converts any error into a short, friendly message for the screen. */
fun Throwable.toUserMessage(): String {
    if (this is UserFacingException) return message ?: "Something went wrong. Please try again."
    if (this is IOException) return "No internet connection. Check your network and try again."

    val text = message.orEmpty()
    return when {
        text.contains("Invalid login credentials", ignoreCase = true) ->
            "Incorrect email or password."
        text.contains("already registered", ignoreCase = true) ||
                text.contains("already exists", ignoreCase = true) ->
            "An account with this email already exists. Try signing in instead."
        text.contains("Password should be", ignoreCase = true) ->
            "That password is too weak. Use at least 8 characters with letters and numbers."
        text.contains("Email not confirmed", ignoreCase = true) ->
            "Please confirm your email first. Check your inbox."
        text.contains("rate limit", ignoreCase = true) ||
                text.contains("too many", ignoreCase = true) ->
            "Too many attempts. Please wait a moment and try again."
        text.contains("invalid email", ignoreCase = true) ->
            "Please enter a valid email address."
        text.contains("Failed to connect", ignoreCase = true) ||
                text.contains("Unable to resolve host", ignoreCase = true) ->
            "No internet connection. Check your network and try again."
        else -> serverMessagePatterns.firstNotNullOfOrNull { it.find(text)?.value }
            ?: "Something went wrong. Please try again."
    }
}