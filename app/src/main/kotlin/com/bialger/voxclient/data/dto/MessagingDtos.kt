package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class SendMessageRequestDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("ciphertext")
    val ciphertext: String,
    @SerializedName("envelope_id")
    val envelopeId: String,
    @SerializedName("envelope_type")
    val envelopeType: Int? = null,
    @SerializedName("ordering_epoch")
    val orderingEpoch: Long? = null,
)

data class SendMessageResponseDto(
    @SerializedName("envelope_id")
    val envelopeId: String,
    @SerializedName("server_timestamp")
    val serverTimestamp: Long,
    @SerializedName("delivered_to_count")
    val deliveredToCount: Int,
)

data class AckEnvelopeRequestDto(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("envelope_id")
    val envelopeId: String,
)

data class EnvelopeDto(
    @SerializedName("envelope_id")
    val envelopeId: String,
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("sender_user_id")
    val senderUserId: String? = null,
    @SerializedName("sender_device_id")
    val senderDeviceId: String,
    @SerializedName("ciphertext")
    val ciphertext: String,
    @SerializedName("server_timestamp")
    val serverTimestamp: Long,
    @SerializedName("envelope_type")
    val envelopeType: Int,
    @SerializedName("ordering_epoch")
    val orderingEpoch: Long?,
)

data class EnvelopesPageResponseDto(
    @SerializedName("envelopes")
    val envelopes: List<EnvelopeDto>,
    @SerializedName("next_cursor")
    val nextCursor: String?,
    @SerializedName("has_more")
    val hasMore: Boolean,
)
