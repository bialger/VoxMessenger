package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.bialger.voxclient.R

class VoxUnreadBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle,
) : AppCompatTextView(ContextThemeWrapper(context, R.style.Widget_Vox_UnreadBadge), attrs, defStyleAttr) {

    fun setUnreadCount(unreadCount: Int) {
        isVisible = unreadCount > 0
        text =
            when {
                unreadCount > 99 -> "99+"
                unreadCount > 0 -> unreadCount.toString()
                else -> ""
            }
    }
}
