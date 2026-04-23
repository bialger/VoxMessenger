package com.bialger.voxclient.ui.sdui.renderer

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bialger.voxclient.R
import com.bialger.voxclient.core.model.SduiAction
import com.bialger.voxclient.core.model.SduiButton
import com.bialger.voxclient.core.model.SduiNode

class SduiViewFactory {
    fun create(
        parent: ViewGroup,
        node: SduiNode,
        onAction: (SduiAction) -> Unit,
    ): View {
        return when (node) {
            is SduiNode.Text -> textView(parent, node)
            is SduiNode.Link -> linkView(parent, node)
            is SduiNode.Divider -> divider(parent)
            is SduiNode.Button -> button(parent, node.value, node.style, node.action, onAction)
            is SduiNode.ButtonRow -> buttonRow(parent, node.buttons, onAction)
        }
    }

    private fun textView(parent: ViewGroup, node: SduiNode.Text): TextView =
        TextView(parent.context).apply {
            text = node.value
            setTextColor(ContextCompat.getColor(context, R.color.vox_color_on_surface))
            textSize = if (node.style.equals("title", ignoreCase = true)) 20f else 14f
            if (node.style.equals("title", ignoreCase = true)) {
                setTypeface(typeface, Typeface.BOLD)
            }
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = 12.dp(context)
                }
        }

    private fun linkView(parent: ViewGroup, node: SduiNode.Link): TextView =
        TextView(parent.context).apply {
            text = node.label
            setTextColor(ContextCompat.getColor(context, R.color.vox_color_primary))
            textSize = 14f
            setOnClickListener {
                val uri = Uri.parse(node.url)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = 12.dp(context)
                }
        }

    private fun divider(parent: ViewGroup): View =
        View(parent.context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.vox_color_subtle_text))
            alpha = 0.25f
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dp(context),
                ).apply {
                    topMargin = 8.dp(context)
                    bottomMargin = 16.dp(context)
                }
        }

    private fun button(
        parent: ViewGroup,
        value: String,
        style: String,
        action: SduiAction,
        onAction: (SduiAction) -> Unit,
    ): Button =
        Button(parent.context).apply {
            text = value
            isAllCaps = false
            setOnClickListener { onAction(action) }
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = 12.dp(context)
                }
            if (style.equals("secondary", ignoreCase = true)) {
                setTextColor(ContextCompat.getColor(context, R.color.vox_color_primary))
            }
        }

    private fun buttonRow(
        parent: ViewGroup,
        buttons: List<SduiButton>,
        onAction: (SduiAction) -> Unit,
    ): View =
        LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = 12.dp(context)
                }

            buttons.forEachIndexed { index, b ->
                val btn =
                    button(this, b.value, b.style, b.action, onAction).apply {
                        layoutParams =
                            LinearLayout.LayoutParams(
                                0,
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                1f,
                            ).apply {
                                marginEnd = if (index == buttons.lastIndex) 0 else 12.dp(context)
                            }
                    }
                addView(btn)
            }
        }

    private fun Int.dp(context: android.content.Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}

