package com.bialger.voxclient.ui.chatlist

data class ChatListItemUi(
    val conversationId: String,
    val conversationType: Int,
    val title: String,
    val preview: String,
    val timestampText: String,
    val unreadCount: Int,
    val typeLabel: String,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val isEncrypted: Boolean,
)

data class ChatListUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val items: List<ChatListItemUi> = emptyList(),
    val emptyMessage: String? = null,
)
