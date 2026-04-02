package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class WebSocketAuthFrameDto(
    @SerializedName("type")
    val type: String = "auth",
    @SerializedName("access_token")
    val accessToken: String,
)

sealed interface VoxWebSocketEventDto {
    data class EnvelopeEvent(
        @SerializedName("type")
        val type: String,
        @SerializedName("envelope_id")
        val envelopeId: String,
        @SerializedName("conversation_id")
        val conversationId: String,
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
    ) : VoxWebSocketEventDto

    data class ConversationMembershipChangedEvent(
        @SerializedName("type")
        val type: String,
        @SerializedName("conversation_id")
        val conversationId: String,
        @SerializedName("membership_version")
        val membershipVersion: Long,
    ) : VoxWebSocketEventDto

    data class UserDevicesChangedEvent(
        @SerializedName("type")
        val type: String,
        @SerializedName("user_id")
        val userId: String,
    ) : VoxWebSocketEventDto

    data class SyncRecordChangedEvent(
        @SerializedName("type")
        val type: String,
        @SerializedName("collection")
        val collection: String,
        @SerializedName("record_id")
        val recordId: String,
        @SerializedName("version")
        val version: Long,
    ) : VoxWebSocketEventDto

    data class UnknownEvent(
        val type: String?,
        val rawPayload: String,
    ) : VoxWebSocketEventDto
}
