package com.bialger.voxclient.core.common.result

import com.bialger.voxclient.core.common.error.VoxError

sealed interface VoxResult<out T> {
    data class Success<T>(val value: T) : VoxResult<T>
    data class Failure(val error: VoxError) : VoxResult<Nothing>
}

inline fun <T, R> VoxResult<T>.map(transform: (T) -> R): VoxResult<R> =
    when (this) {
        is VoxResult.Success -> VoxResult.Success(transform(value))
        is VoxResult.Failure -> this
    }

inline fun <T, R> VoxResult<T>.flatMap(transform: (T) -> VoxResult<R>): VoxResult<R> =
    when (this) {
        is VoxResult.Success -> transform(value)
        is VoxResult.Failure -> this
    }

fun <T> VoxResult<T>.getOrNull(): T? = (this as? VoxResult.Success<T>)?.value

