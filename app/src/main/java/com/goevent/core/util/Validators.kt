package com.goevent.core.util

/** Form validation. Each function returns an error message, or null when the value is valid. */
object Validators {
    private val EMAIL = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")
    private val NAME = Regex("^\\p{L}[\\p{L} '\\-]*$")
    private val SEARCH_UNSAFE = Regex("[,()%*\\\\]")
    private val WHITESPACE = Regex("\\s+")

    fun emailError(email: String): String? = when {
        email.isBlank() -> "Email is required"
        !EMAIL.matches(email.trim()) -> "Enter a valid email address"
        else -> null
    }

    fun passwordError(password: String): String? = when {
        password.isEmpty() -> "Password is required"
        password.length < 8 -> "Use at least 8 characters"
        password.none { it.isUpperCase() } -> "Add an uppercase letter"
        password.none { it.isLowerCase() } -> "Add a lowercase letter"
        password.none { it.isDigit() } -> "Add a number"
        else -> null
    }

    fun confirmPasswordError(password: String, confirmation: String): String? = when {
        confirmation.isEmpty() -> "Please confirm your password"
        password != confirmation -> "Passwords do not match"
        else -> null
    }

    fun nameError(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Full name is required"
            trimmed.length < 2 -> "Name is too short"
            trimmed.length > 60 -> "Name is too long"
            !NAME.matches(trimmed) -> "Use letters, spaces, hyphens or apostrophes only"
            else -> null
        }
    }

    /** Removes characters that could break a database search filter. */
    fun sanitiseSearch(query: String): String =
        query.replace(SEARCH_UNSAFE, " ").replace(WHITESPACE, " ").trim().take(60)
}