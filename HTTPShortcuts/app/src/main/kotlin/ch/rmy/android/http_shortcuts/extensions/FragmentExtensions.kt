package ch.rmy.android.http_shortcuts.extensions

import android.os.Bundle
import androidx.fragment.app.Fragment

inline fun <T : Fragment> T.addArguments(block: Bundle.() -> Unit): T =
    apply {
        arguments = Bundle().apply(block)
    }
