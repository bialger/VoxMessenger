package com.bialger.voxclient.core.model

enum class ConversationType {
    DM,
    GROUP,
    CHANNEL,
}

enum class MessageDeliveryState {
    QUEUED,
    ENCRYPTING,
    SENDING,
    SENT,
    DELIVERED,
    FAILED,
}

data class ConversationSummary(
    val id: ConversationId,
    val unreadCount: Int,
)

data class ChatMessage(
    val envelopeId: EnvelopeId,
    val conversationId: ConversationId,
    val authorUserId: UserId,
    val body: String,
    val sentAt: Long,
)

sealed interface SendState {
    data object Idle : SendState
    data object Encrypting : SendState
    data object Sending : SendState
    data class Failed(val reason: String) : SendState
    data object Sent : SendState
}

fun titleFor(type: ConversationType): String =
    when (type) {
        ConversationType.DM -> "Direct Message"
        ConversationType.GROUP -> "Group"
        ConversationType.CHANNEL -> "Channel"
    }

fun ChatMessage.preview(max: Int = 40): String =
    if (body.length <= max) body else body.take(max) + "..."

fun totalUnread(items: List<ConversationSummary>): Int =
    items.fold(0) { acc, item -> acc + item.unreadCount }

