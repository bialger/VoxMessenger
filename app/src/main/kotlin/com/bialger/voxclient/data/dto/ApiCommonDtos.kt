package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class EmptyResponseDto(
    @SerializedName("empty")
    val empty: String? = null,
)

data class ApiErrorEnvelopeDto(
    @SerializedName("error")
    val error: ApiErrorDto,
)

data class ApiErrorDto(
    @SerializedName("code")
    val code: Int,
    @SerializedName("message")
    val message: String,
)
