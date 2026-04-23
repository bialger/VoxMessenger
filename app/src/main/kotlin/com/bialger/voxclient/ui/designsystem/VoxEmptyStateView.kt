package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bialger.voxclient.R
import com.bialger.voxclient.databinding.ViewVoxEmptyStateContentBinding

class VoxEmptyStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVoxEmptyStateContentBinding.inflate(LayoutInflater.from(context), this)

    init {
        setPadding(
            resources.getDimensionPixelSize(R.dimen.vox_spacing_24),
            resources.getDimensionPixelSize(R.dimen.vox_spacing_24),
            resources.getDimensionPixelSize(R.dimen.vox_spacing_24),
            resources.getDimensionPixelSize(R.dimen.vox_spacing_24),
        )

        val typedArray =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.VoxStateView,
                defStyleAttr,
                0,
            )
        val title = typedArray.getText(R.styleable.VoxStateView_voxStateTitle)
        val subtitle = typedArray.getText(R.styleable.VoxStateView_voxStateSubtitle)
        typedArray.recycle()

        if (!title.isNullOrBlank()) {
            setTitleText(title)
        }
        if (!subtitle.isNullOrBlank()) {
            setSubtitleText(subtitle)
        }
    }

    fun setTitleText(value: CharSequence) {
        binding.stateTitle.text = value
    }

    fun setSubtitleText(value: CharSequence) {
        binding.stateSubtitle.text = value
    }
}
