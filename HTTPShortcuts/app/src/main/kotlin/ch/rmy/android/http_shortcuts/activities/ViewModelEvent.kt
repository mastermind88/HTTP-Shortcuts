package ch.rmy.android.http_shortcuts.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

abstract class ViewModelEvent {
    data class Finish(
        val result: Int? = null,
        val intent: Intent? = null,
        val skipAnimation: Boolean = false,
    ) : ViewModelEvent()

    data class OpenURL(val url: String) : ViewModelEvent()

    data class OpenActivity(val intentBuilder: (context: Context) -> BaseIntentBuilder) : ViewModelEvent()

    // TODO: Refactor this so that it does not depend on actual view classes, and only contains data
    class ShowDialog(val dialogBuilder: (context: Context) -> Dialog?) : ViewModelEvent()

    class ShowSnackbar(
        val message: Localizable,
        val long: Boolean = false,
    ) : ViewModelEvent() {
        constructor(@StringRes message: Int, long: Boolean = false) : this(StringResLocalizable(message, long))
    }
}