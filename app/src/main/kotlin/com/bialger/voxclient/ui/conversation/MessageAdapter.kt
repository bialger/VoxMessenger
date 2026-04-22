package com.bialger.voxclient.ui.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bialger.voxclient.R

class MessageAdapter : ListAdapter<ConversationMessageUi, MessageAdapter.MessageViewHolder>(DiffCallback) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isOutgoing) VIEW_TYPE_OUTGOING else VIEW_TYPE_INCOMING

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutRes =
            if (viewType == VIEW_TYPE_OUTGOING) {
                R.layout.item_message_outgoing
            } else {
                R.layout.item_message_incoming
            }
        val messageView =
            LayoutInflater.from(parent.context)
                .inflate(layoutRes, parent, false) as ConversationMessageItemView
        return MessageViewHolder(messageView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MessageViewHolder(
        private val messageView: ConversationMessageItemView,
    ) : RecyclerView.ViewHolder(messageView) {
        fun bind(item: ConversationMessageUi) {
            messageView.bind(item)
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
