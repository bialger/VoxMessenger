package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class HealthDto(
    @SerializedName("status")
    val status: String,
)
