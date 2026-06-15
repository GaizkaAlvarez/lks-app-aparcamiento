package com.parkinglksnext.util

/**
 * Sealed class wrapper for async operations (Loading, Success, Error).
 * Used across all repository and ViewModel layers.
 */
sealed class Resource<out T> {
    class Loading<T> : Resource<T>()
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String) : Resource<T>()
}
