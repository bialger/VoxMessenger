package com.bialger.voxclient.ui.chatlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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
            binding.titleLabel.text = item.title
            binding.previewLabel.text = item.preview
            binding.timestampLabel.text = item.timestampText
            binding.typeLabel.text = item.typeLabel

            binding.unreadBadge.isVisible = item.unreadCount > 0
            binding.unreadBadge.text =
                when {
                    item.unreadCount > 99 -> "99+"
                    item.unreadCount > 0 -> item.unreadCount.toString()
                    else -> ""
                }

            binding.pinnedIndicator.isVisible = item.isPinned
            binding.mutedIndicator.isVisible = item.isMuted
            binding.encryptionIndicator.isVisible = item.isEncrypted
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
