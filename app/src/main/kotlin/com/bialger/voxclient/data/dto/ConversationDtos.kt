package com.bialger.voxclient.data.dto

import com.google.gson.annotations.SerializedName

data class ConversationsResponseDto(
    @SerializedName("conversations")
    val conversations: List<ConversationSummaryDto>,
)

data class ConversationSummaryDto(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("type")
    val type: Int,
    @SerializedName("created_by")
    val createdBy: String,
    @SerializedName("created_by_username")
    val createdByUsername: String? = null,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("membership_version")
    val membershipVersion: Long,
    @SerializedName("last_activity_at")
    val lastActivityAt: Long? = null,
)

data class ConversationDetailDto(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("type")
    val type: Int,
    @SerializedName("created_by")
    val createdBy: String,
    @SerializedName("created_by_username")
    val createdByUsername: String? = null,
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("membership_version")
    val membershipVersion: Long,
    @SerializedName("my_role")
    val myRole: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("channel_post_policy")
    val channelPostPolicy: String?,
)

data class ConversationMembersResponseDto(
    @SerializedName("conversation_id")
    val conversationId: String,
    @SerializedName("membership_version")
    val membershipVersion: Long,
    @SerializedName("members")
    val members: List<ConversationMemberDto>?,
    @SerializedName("admins")
    val admins: List<ConversationMemberDto>?,
    @SerializedName("subscribers")
    val subscribers: List<ConversationMemberDto>?,
    @SerializedName("subscription_state")
    val subscriptionState: String?,
    @SerializedName("member_count")
    val memberCount: Int?,
)

data class ConversationMemberDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("role")
    val role: String,
)

data class CreateConversationRequestDto(
    @SerializedName("type")
    val type: String,
    @SerializedName("peer_user_id")
    val peerUserId: String? = null,
    @SerializedName("members")
    val members: List<String>? = null,
    @SerializedName("admins")
    val admins: List<String>? = null,
    @SerializedName("subscribers")
    val subscribers: List<String>? = null,
)

data class CreateConversationResponseDto(
    @SerializedName("conversation_id")
    val conversationId: String,
)

data class AddConversationMemberRequestDto(
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("role")
    val role: String? = null,
)
