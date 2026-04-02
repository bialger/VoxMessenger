package com.bialger.voxclient.ui.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bialger.voxclient.databinding.ItemMessageIncomingBinding
import com.bialger.voxclient.databinding.ItemMessageOutgoingBinding

class MessageAdapter : ListAdapter<ConversationMessageUi, RecyclerView.ViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_OUTGOING) {
            OutgoingMessageViewHolder(ItemMessageOutgoingBinding.inflate(inflater, parent, false))
        } else {
            IncomingMessageViewHolder(ItemMessageIncomingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is IncomingMessageViewHolder -> holder.bind(item)
            is OutgoingMessageViewHolder -> holder.bind(item)
        }
    }

    class IncomingMessageViewHolder(
        private val binding: ItemMessageIncomingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ConversationMessageUi) {
            binding.authorLabel.isVisible = !item.authorName.isNullOrBlank()
            binding.authorLabel.text = item.authorName.orEmpty()
            binding.messageBubble.text = item.body
            binding.timestampLabel.text = item.timestampText
        }
    }

    class OutgoingMessageViewHolder(
        private val binding: ItemMessageOutgoingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ConversationMessageUi) {
            binding.authorLabel.isVisible = !item.authorName.isNullOrBlank()
            binding.authorLabel.text = item.authorName.orEmpty()
            binding.messageBubble.text = item.body
            binding.timestampLabel.text = item.timestampText
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ConversationMessageUi>() {
        override fun areItemsTheSame(oldItem: ConversationMessageUi, newItem: ConversationMessageUi): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ConversationMessageUi, newItem: ConversationMessageUi): Boolean =
            oldItem == newItem
    }

    private companion object {
        const val VIEW_TYPE_INCOMING = 0
        const val VIEW_TYPE_OUTGOING = 1
    }
}
