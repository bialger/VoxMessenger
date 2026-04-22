package com.bialger.voxclient.ui.conversation

data class ConversationMessageUi(
    val id: String,
    val body: String,
    val timestampText: String,
    val isOutgoing: Boolean,
    val authorName: String? = null,
)
