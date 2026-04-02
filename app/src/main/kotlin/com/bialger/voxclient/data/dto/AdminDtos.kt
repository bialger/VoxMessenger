package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class AdminStatsDto(
    @SerializedName("user_count")
    val userCount: Int,
    @SerializedName("device_count")
    val deviceCount: Int,
    @SerializedName("active_session_count")
    val activeSessionCount: Int,
    @SerializedName("conversation_count")
    val conversationCount: Int,
    @SerializedName("pending_envelope_count")
    val pendingEnvelopeCount: Int,
    @SerializedName("total_storage_bytes")
    val totalStorageBytes: Long,
)
