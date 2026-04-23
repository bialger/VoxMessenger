package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.util.AttributeSet
import android.view.ContextThemeWrapper
import com.bialger.voxclient.R
import com.google.android.material.appbar.MaterialToolbar

class VoxTopBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.toolbarStyle,
) : MaterialToolbar(ContextThemeWrapper(context, R.style.Widget_Vox_TopBar), attrs, defStyleAttr)
