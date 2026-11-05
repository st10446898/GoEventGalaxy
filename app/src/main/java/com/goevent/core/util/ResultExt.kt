package com.goevent.core.util

import kotlin.coroutines.cancellation.CancellationException

/** An error whose message is safe and friendly to show directly to the user. */
class UserFacingException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Like runCatching, but never swallows coroutine cancellation
 * (so screens that are closed stop their work properly).
 */
inline fun <T> safeCall(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }