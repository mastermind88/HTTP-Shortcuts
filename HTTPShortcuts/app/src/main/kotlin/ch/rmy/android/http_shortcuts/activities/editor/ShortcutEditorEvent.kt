package ch.rmy.android.http_shortcuts.activities.editor

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent

abstract class ShortcutEditorEvent : ViewModelEvent() {
    object FocusNameInputField : ShortcutEditorEvent()
}
