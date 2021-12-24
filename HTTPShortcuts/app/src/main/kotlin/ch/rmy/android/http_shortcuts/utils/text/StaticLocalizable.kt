package ch.rmy.android.http_shortcuts.utils.text

import android.content.Context

data class StaticLocalizable(val string: String) : Localizable {
    override fun localize(context: Context): CharSequence =
        string
}