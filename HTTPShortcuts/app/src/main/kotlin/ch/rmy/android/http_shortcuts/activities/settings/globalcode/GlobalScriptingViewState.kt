package ch.rmy.android.http_shortcuts.activities.settings.globalcode

import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable

data class GlobalScriptingViewState(
    val globalCode: CharSequence = "",
    val saveButtonVisible: Boolean = false,
    val variables: List<Variable> = emptyList(),
    val shortcuts: List<Shortcut> = emptyList(),
)