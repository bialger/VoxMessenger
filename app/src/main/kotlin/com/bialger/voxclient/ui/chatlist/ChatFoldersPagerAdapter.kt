package com.bialger.voxclient.ui.chatlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bialger.voxclient.databinding.ItemChatFolderPageBinding

class ChatFoldersPagerAdapter(
    private val onItemClicked: (ChatListItemUi) -> Unit,
) : RecyclerView.Adapter<ChatFoldersPagerAdapter.FolderPageViewHolder>() {

    private val folderTypes = listOf(TYPE_DM, TYPE_GROUP, TYPE_CHANNEL)
    private val listAdapters = folderTypes.associateWith { ChatListAdapter(onItemClicked) }
    private val folderItemsByType = folderTypes.associateWith { emptyList<ChatListItemUi>() }.toMutableMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderPageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return FolderPageViewHolder(ItemChatFolderPageBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: FolderPageViewHolder, position: Int) {
        val folderType = getFolderTypeAt(position)
        holder.bind(listAdapters.getValue(folderType))
    }

    override fun getItemCount(): Int = folderTypes.size

    fun submitItems(items: List<ChatListItemUi>) {
        folderTypes.forEach { folderType ->
            val filtered = items.filter { it.conversationType == folderType }
            folderItemsByType[folderType] = filtered
            listAdapters.getValue(folderType).submitList(filtered)
        }
    }

    fun getFolderTypeAt(position: Int): Int =
        folderTypes.getOrElse(position) { TYPE_DM }

    fun getCountForType(folderType: Int): Int =
        folderItemsByType[folderType]?.size ?: 0

    class FolderPageViewHolder(
        private val binding: ItemChatFolderPageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.chatFolderRecyclerView.layoutManager =
                LinearLayoutManager(binding.root.context).apply {
                    reverseLayout = false
                    stackFromEnd = false
                }
        }

        fun bind(adapter: ChatListAdapter) {
            if (binding.chatFolderRecyclerView.adapter !== adapter) {
                binding.chatFolderRecyclerView.adapter = adapter
            }
        }
    }

    companion object {
        const val TYPE_DM = 0
        const val TYPE_GROUP = 1
        const val TYPE_CHANNEL = 2
    }
}
