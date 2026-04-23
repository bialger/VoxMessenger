package com.bialger.voxclient.ui.designsystem

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.widget.doAfterTextChanged
import com.bialger.voxclient.R
import com.bialger.voxclient.databinding.ViewVoxTextInputBinding

class VoxTextInput @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVoxTextInputBinding.inflate(LayoutInflater.from(context), this, true)

    var text: CharSequence
        get() = binding.editText.text ?: ""
        set(value) {
            binding.editText.setText(value)
        }

    init {
        val typedArray =
            context.obtainStyledAttributes(
                attrs,
                R.styleable.VoxTextInput,
                defStyleAttr,
                0,
            )
        val hint = typedArray.getText(R.styleable.VoxTextInput_voxHint)
        val inputType =
            typedArray.getInt(
                R.styleable.VoxTextInput_android_inputType,
                binding.editText.inputType,
            )
        val imeOptions =
            typedArray.getInt(
                R.styleable.VoxTextInput_android_imeOptions,
                binding.editText.imeOptions,
            )
        val initialText = typedArray.getText(R.styleable.VoxTextInput_android_text)
        typedArray.recycle()

        if (hint != null) {
            binding.inputLayout.hint = hint
        }
        binding.editText.inputType = inputType
        binding.editText.imeOptions = imeOptions
        if (!initialText.isNullOrBlank()) {
            binding.editText.setText(initialText)
        }
    }

    fun setHint(hint: CharSequence) {
        binding.inputLayout.hint = hint
    }

    fun doAfterTextChanged(afterTextChanged: (Editable?) -> Unit) {
        binding.editText.doAfterTextChanged(afterTextChanged)
    }
}
