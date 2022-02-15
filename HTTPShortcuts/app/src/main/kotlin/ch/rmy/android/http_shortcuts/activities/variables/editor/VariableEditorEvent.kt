package ch.rmy.android.http_shortcuts.activities.variables.editor

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent

abstract class VariableEditorEvent : ViewModelEvent() {
    object FocusVariableKeyInput : VariableEditorEvent()
}
