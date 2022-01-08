package ch.rmy.android.http_shortcuts.extensions

import android.app.Activity
import android.content.Intent
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import ch.rmy.android.http_shortcuts.utils.SnackbarManager

fun Activity.showSnackbar(@StringRes message: Int, long: Boolean = false) {
    showSnackbar(getText(message), long)
}

fun Activity.showSnackbar(message: CharSequence, long: Boolean = false) {
    SnackbarManager.showSnackbar(this, message, long)
}

fun Fragment.showSnackbar(@StringRes message: Int, long: Boolean = false) {
    activity?.showSnackbar(message, long)
}

fun Fragment.showSnackbar(message: CharSequence, long: Boolean = false) {
    activity?.showSnackbar(message, long)
}

fun Activity.finishWithoutAnimation() {
    overridePendingTransition(0, 0)
    finish()
    overridePendingTransition(0, 0)
}

fun Activity.restartWithoutAnimation() {
    overridePendingTransition(0, 0)
    finish()
    startActivity(Intent(this, this::class.java))
    overridePendingTransition(0, 0)
}
