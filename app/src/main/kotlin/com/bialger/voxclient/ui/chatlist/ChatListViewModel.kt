package com.bialger.voxclient.ui.chatlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatListViewModel : ViewModel() {

    private var allItems: List<ChatListItemUi> = emptyList()
    private val _uiState = MutableLiveData(ChatListUiState())
    val uiState: LiveData<ChatListUiState> = _uiState

    fun setLoading() {
        val current = _uiState.value ?: ChatListUiState()
        _uiState.value = current.copy(isLoading = true, emptyMessage = null)
    }

    fun setItems(items: List<ChatListItemUi>) {
        allItems = items
        applyFilter((_uiState.value ?: ChatListUiState()).query, emptyMessage = null)
    }

    fun addItem(item: ChatListItemUi) {
        allItems = listOf(item) + allItems.filterNot { it.conversationId == item.conversationId }
        applyFilter((_uiState.value ?: ChatListUiState()).query, emptyMessage = null)
    }

    fun setError(message: String) {
        allItems = emptyList()
        val currentQuery = (_uiState.value ?: ChatListUiState()).query
        _uiState.value = ChatListUiState(
            isLoading = false,
            query = currentQuery,
            items = emptyList(),
            emptyMessage = message,
        )
    }

    fun setNonBlockingError(message: String) {
        val current = _uiState.value ?: ChatListUiState()
        _uiState.value = current.copy(isLoading = false, emptyMessage = message)
    }

    fun loadMockItems() {
        setItems(
            listOf(
                ChatListItemUi(
                    conversationId = "cnv_dm_alex",
                    conversationType = TYPE_DM,
                    title = "alex",
                    preview = "Signed prekey rotation completed for this device.",
                    timestampText = "09:42",
                    unreadCount = 2,
                    typeLabel = "DM",
                    isMuted = false,
                    isPinned = true,
                    isEncrypted = true,
                ),
                ChatListItemUi(
                    conversationId = "cnv_group_mobile",
                    conversationType = TYPE_GROUP,
                    title = "Mobile Team",
                    preview = "Server discovery checklist is ready for QA review.",
                    timestampText = "08:15",
                    unreadCount = 0,
                    typeLabel = "GROUP",
                    isMuted = false,
                    isPinned = false,
                    isEncrypted = true,
                ),
                ChatListItemUi(
                    conversationId = "cnv_channel_ops",
                    conversationType = TYPE_CHANNEL,
                    title = "ops-announcements",
                    preview = "Nightly sync drift report is now available.",
                    timestampText = "Yesterday",
                    unreadCount = 12,
                    typeLabel = "CHANNEL",
                    isMuted = true,
                    isPinned = false,
                    isEncrypted = true,
                ),
                ChatListItemUi(
                    conversationId = "cnv_dm_qa",
                    conversationType = TYPE_DM,
                    title = "qa_bot",
                    preview = "Latest test run passed: 46 network contract checks.",
                    timestampText = "Mon",
                    unreadCount = 0,
                    typeLabel = "DM",
                    isMuted = false,
                    isPinned = false,
                    isEncrypted = true,
                ),
                ChatListItemUi(
                    conversationId = "cnv_group_design",
                    conversationType = TYPE_GROUP,
                    title = "Design Sync",
                    preview = "Updated chat list row spacing to 12dp baseline.",
                    timestampText = "Sun",
                    unreadCount = 4,
                    typeLabel = "GROUP",
                    isMuted = false,
                    isPinned = true,
                    isEncrypted = true,
                ),
            ),
        )
    }

    fun onSearchQueryChanged(rawQuery: String) {
        applyFilter(rawQuery.trim(), emptyMessage = (_uiState.value ?: ChatListUiState()).emptyMessage)
    }

    private fun applyFilter(query: String, emptyMessage: String?) {
        val loweredQuery = query.lowercase()
        val filteredItems =
            if (loweredQuery.isBlank()) {
                allItems
            } else {
                allItems.filter { item ->
                    item.title.lowercase().contains(loweredQuery) ||
                        item.preview.lowercase().contains(loweredQuery) ||
                        item.typeLabel.lowercase().contains(loweredQuery)
                }
            }

        _uiState.value = ChatListUiState(
            isLoading = false,
            query = query,
            items = filteredItems,
            emptyMessage = emptyMessage,
        )
    }

    private companion object {
        const val TYPE_DM = 0
        const val TYPE_GROUP = 1
        const val TYPE_CHANNEL = 2
    }
}
