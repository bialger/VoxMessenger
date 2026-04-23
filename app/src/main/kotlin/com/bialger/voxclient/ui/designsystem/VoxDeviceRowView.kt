package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.bialger.voxclient.R
import com.bialger.voxclient.databinding.ViewVoxDeviceRowContentBinding

class VoxDeviceRowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVoxDeviceRowContentBinding.inflate(LayoutInflater.from(context), this)

    init {
        val typedArray =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.VoxDeviceRowView,
                defStyleAttr,
                0,
            )
        val name = typedArray.getText(R.styleable.VoxDeviceRowView_voxDeviceName)
        val subtitle = typedArray.getText(R.styleable.VoxDeviceRowView_voxDeviceSubtitle)
        val statusColor =
            typedArray.getColor(
                R.styleable.VoxDeviceRowView_voxDeviceStatusColor,
                context.getColor(R.color.vox_color_success),
            )
        typedArray.recycle()

        if (!name.isNullOrBlank()) {
            setDeviceName(name)
        }
        if (!subtitle.isNullOrBlank()) {
            setDeviceSubtitle(subtitle)
        }
        setStatusColor(statusColor)
    }

    fun setDeviceName(value: CharSequence) {
        binding.deviceName.text = value
    }

    fun setDeviceSubtitle(value: CharSequence) {
        binding.deviceSubtitle.text = value
    }

    fun setStatusColor(color: Int) {
        binding.deviceStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }
}
