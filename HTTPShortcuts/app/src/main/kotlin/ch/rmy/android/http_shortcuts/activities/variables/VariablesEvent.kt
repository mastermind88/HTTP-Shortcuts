package ch.rmy.android.http_shortcuts.activities.variables

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.utils.text.Localizable

abstract class VariablesEvent : ViewModelEvent() {
    object ShowCreationDialog : VariablesEvent()

    class ShowContextMenu(
        val variableId: String,
        val title: Localizable,
    ) : VariablesEvent()

    class ShowDeletionDialog(
        val variableId: String,
        val message: Localizable,
    ) : VariablesEvent()
}