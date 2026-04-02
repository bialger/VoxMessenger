package com.bialger.voxclient.domain.entity

data class VoxConversationSummary(
    val conversationId: String,
    val type: Int,
    val createdBy: String,
    val createdByUsername: String?,
    val peerUserId: String? = null,
    val peerUsername: String? = null,
    val createdAt: Long,
    val membershipVersion: Long,
    val lastActivityAt: Long?,
)

data class VoxConversationDetail(
    val conversationId: String,
    val type: Int,
    val createdBy: String,
    val createdByUsername: String?,
    val peerUserId: String? = null,
    val peerUsername: String? = null,
    val createdAt: Long,
    val membershipVersion: Long,
    val myRole: String?,
    val title: String?,
    val channelPostPolicy: String?,
)

data class VoxConversationMember(
    val userId: String,
    val username: String?,
    val role: String,
)

data class VoxConversationMembers(
    val conversationId: String,
    val membershipVersion: Long,
    val members: List<VoxConversationMember>,
    val admins: List<VoxConversationMember>,
    val subscribers: List<VoxConversationMember>,
    val subscriptionState: String?,
    val memberCount: Int?,
)

data class VoxConversationEnvelope(
    val envelopeId: String,
    val conversationId: String,
    val senderUserId: String?,
    val senderDeviceId: String,
    val ciphertext: String,
    val serverTimestamp: Long,
    val envelopeType: Int,
    val orderingEpoch: Long?,
)

data class VoxSendMessageCommand(
    val deviceId: String,
    val conversationId: String,
    val ciphertext: String,
    val envelopeId: String,
    val envelopeType: Int? = null,
    val orderingEpoch: Long? = null,
)

data class VoxSendMessageReceipt(
    val envelopeId: String,
    val serverTimestamp: Long,
    val deliveredToCount: Int,
)
