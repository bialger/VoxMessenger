package com.bialger.voxclient.ui.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bialger.voxclient.R

class ConversationMessageItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val authorLabel: TextView
    private val messageBubble: TextView
    private val timestampLabel: TextView

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_conversation_message_content, this, true)

        authorLabel = findViewById(R.id.authorLabel)
        messageBubble = findViewById(R.id.messageBubble)
        timestampLabel = findViewById(R.id.timestampLabel)

        val styledAttributes =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.ConversationMessageItemView,
                defStyleAttr,
                0,
            )
        val isOutgoing =
            styledAttributes.getBoolean(
                R.styleable.ConversationMessageItemView_voxOutgoing,
                false,
            )
        styledAttributes.recycle()
        applyDirection(isOutgoing)
    }

    fun bind(item: ConversationMessageUi) {
        authorLabel.isVisible = !item.authorName.isNullOrBlank()
        authorLabel.text = item.authorName.orEmpty()
        messageBubble.text = item.body
        timestampLabel.text = item.timestampText
        applyDirection(item.isOutgoing)
    }

    private fun applyDirection(isOutgoing: Boolean) {
        gravity = if (isOutgoing) Gravity.END else Gravity.START
        messageBubble.setBackgroundResource(
            if (isOutgoing) R.drawable.bg_message_outgoing else R.drawable.bg_message_incoming,
        )
        val messageColorRes =
            if (isOutgoing) {
                R.color.vox_color_on_primary
            } else {
                R.color.vox_color_on_surface
            }
        messageBubble.setTextColor(ContextCompat.getColor(context, messageColorRes))
    }
}
