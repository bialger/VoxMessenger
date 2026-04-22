package com.bialger.voxclient.core.common.error

sealed interface VoxError {
    val message: String

    data class Validation(override val message: String) : VoxError

    data class Network(
        val code: Int,
        override val message: String,
    ) : VoxError

    data class Conflict(override val message: String) : VoxError

    data class Unknown(
        override val message: String,
        val cause: Throwable? = null,
    ) : VoxError
}

