package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bialger.voxclient.R
import com.bialger.voxclient.databinding.ViewVoxChatCellContentBinding

class VoxChatCell @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVoxChatCellContentBinding.inflate(LayoutInflater.from(context), this)

    init {
        background = ContextCompat.getDrawable(context, R.drawable.bg_chat_item)
        minimumHeight = resources.getDimensionPixelSize(R.dimen.vox_chat_item_min_height)
        val innerPadding = resources.getDimensionPixelSize(R.dimen.vox_spacing_12)
        setPadding(innerPadding, innerPadding, innerPadding, innerPadding)
    }

    fun bind(
        title: CharSequence,
        preview: CharSequence,
        timestamp: CharSequence,
        typeLabel: CharSequence,
        unreadCount: Int,
        isPinned: Boolean,
        isMuted: Boolean,
        isEncrypted: Boolean,
    ) {
        binding.titleLabel.text = title
        binding.previewLabel.text = preview
        binding.timestampLabel.text = timestamp
        binding.typeLabel.text = typeLabel
        binding.pinnedIndicator.isVisible = isPinned
        binding.mutedIndicator.isVisible = isMuted
        binding.encryptionIndicator.setSecured(isEncrypted)
        binding.unreadBadge.setUnreadCount(unreadCount)
    }
}
