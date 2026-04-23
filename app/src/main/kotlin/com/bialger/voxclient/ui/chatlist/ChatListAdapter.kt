package com.bialger.voxclient.ui.chatlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bialger.voxclient.databinding.ItemChatBinding

class ChatListAdapter(
    private val onItemClicked: (ChatListItemUi) -> Unit,
) : ListAdapter<ChatListItemUi, ChatListAdapter.ChatItemViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ChatItemViewHolder(ItemChatBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: ChatItemViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClicked)
    }

    class ChatItemViewHolder(
        private val binding: ItemChatBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatListItemUi, onItemClicked: (ChatListItemUi) -> Unit) {
            binding.root.bind(
                title = item.title,
                preview = item.preview,
                timestamp = item.timestampText,
                typeLabel = item.typeLabel,
                unreadCount = item.unreadCount,
                isPinned = item.isPinned,
                isMuted = item.isMuted,
                isEncrypted = item.isEncrypted,
            )
            binding.root.setOnClickListener { onItemClicked(item) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ChatListItemUi>() {
        override fun areItemsTheSame(oldItem: ChatListItemUi, newItem: ChatListItemUi): Boolean =
            oldItem.conversationId == newItem.conversationId

        override fun areContentsTheSame(oldItem: ChatListItemUi, newItem: ChatListItemUi): Boolean =
            oldItem == newItem
    }
}
