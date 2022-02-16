package ch.rmy.android.framework.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import ch.rmy.android.framework.extensions.addRippleAnimation
import ch.rmy.android.framework.extensions.layoutInflater
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.databinding.ViewPanelButtonBinding

class PanelButton
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attrs, defStyleAttr) {

    private val binding = ViewPanelButtonBinding.inflate(layoutInflater, this)

    var title: CharSequence = ""
        set(value) {
            field = value
            binding.panelButtonTitle.text = value
        }

    var subtitle: CharSequence = ""
        set(value) {
            field = value
            binding.panelButtonSubtitle.text = value
        }

    init {
        addRippleAnimation()

        if (attrs != null) {
            var a: TypedArray? = null
            try {
                @SuppressLint("Recycle")
                a = context.obtainStyledAttributes(attrs, ATTRIBUTE_IDS)
                title = a.getText(ATTRIBUTE_IDS.indexOf(android.R.attr.text)) ?: ""
                subtitle = a.getText(ATTRIBUTE_IDS.indexOf(R.attr.subtitle)) ?: ""
            } finally {
                a?.recycle()
            }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        val alpha = if (enabled) 1f else 0.4f
        binding.panelButtonTitle.alpha = alpha
        binding.panelButtonSubtitle.alpha = alpha
    }

    companion object {

        private val ATTRIBUTE_IDS = intArrayOf(android.R.attr.text, R.attr.subtitle)
    }
}
