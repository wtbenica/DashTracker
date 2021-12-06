package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import com.wtb.dashTracker.databinding.FabFlyoutButtonBinding

class FabMenuButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val buttonInfo: FabMenuButtonInfo? = null
) : LinearLayout(context, attrs) {

    init {
        val binding = FabFlyoutButtonBinding.inflate(LayoutInflater.from(context))
        binding.fabFlyoutLabel.text = buttonInfo?.label
        buttonInfo?.resId?.let { binding.fabFlyoutButtonButton.setImageResource(it) }
        binding.fabFlyoutButtonButton.setOnClickListener(buttonInfo?.action)
    }

    companion object {
        fun newInstance(context: Context, buttonInfo: FabMenuButtonInfo): FabMenuButton {
            return FabMenuButton(context, buttonInfo = buttonInfo)
        }
    }
}

data class FabMenuButtonInfo(
    val label: String,
    @DrawableRes val resId: Int,
    val action: (View) -> Unit
)