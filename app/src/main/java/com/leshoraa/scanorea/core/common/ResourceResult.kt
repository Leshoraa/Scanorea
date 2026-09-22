package com.leshoraa.scanorea.core.common

/**
 * A generic class that holds a value with its loading status or error.
 */
sealed interface ResourceResult<out T> {
    data class Success<out T>(val data: T) : ResourceResult<T>
    data class Error(val exception: Throwable, val userMessage: String? = null) : ResourceResult<Nothing>
    data object Loading : ResourceResult<Nothing>
}
