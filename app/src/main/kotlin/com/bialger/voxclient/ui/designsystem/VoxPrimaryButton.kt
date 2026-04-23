package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import com.bialger.voxclient.R
import com.google.android.material.button.MaterialButton

class VoxPrimaryButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialButtonStyle,
) : MaterialButton(ContextThemeWrapper(context, R.style.Widget_Vox_PrimaryButton), attrs, defStyleAttr)
