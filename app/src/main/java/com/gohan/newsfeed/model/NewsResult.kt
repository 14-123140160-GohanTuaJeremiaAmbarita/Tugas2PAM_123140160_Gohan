package com.gohan.newsfeed.model

/**
 * Sealed class untuk menangani status hasil operasi secara robust.
 */
sealed class NewsResult<out T> {
    data class Success<out T>(val data: T) : NewsResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : NewsResult<Nothing>()
    object Loading : NewsResult<Nothing>()
}
