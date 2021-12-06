package com.wtb.dashTracker.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.wtb.dashTracker.MainActivity.Companion.APP
import com.wtb.dashTracker.databinding.FabFlyoutButtonBinding

class FabMenuButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val buttonInfo: FabMenuButtonInfo? = null,
    parent: ViewGroup? = null
) : LinearLayout(context, attrs) {

    val isVisible = false

    init {
        val binding = FabFlyoutButtonBinding.inflate(LayoutInflater.from(context), this, true)
        binding.fabFlyoutLabel.text = buttonInfo?.label
        buttonInfo?.resId?.let {
            val drawable = AppCompatResources.getDrawable(context, it)

            binding.fabFlyoutButtonButton.setImageDrawable(drawable)
        }
        binding.fabFlyoutButtonButton.setOnClickListener(buttonInfo?.action)
    }

    companion object {
        private const val TAG = APP + "FabMenuButton"

        fun newInstance(
            context: Context,
            buttonInfo: FabMenuButtonInfo,
            parent: ViewGroup
        ): FabMenuButton {
            return FabMenuButton(context, buttonInfo = buttonInfo, parent = parent)
        }
    }
}

data class FabMenuButtonInfo(
    val label: String,
    @DrawableRes val resId: Int,
    val action: (View) -> Unit,
)