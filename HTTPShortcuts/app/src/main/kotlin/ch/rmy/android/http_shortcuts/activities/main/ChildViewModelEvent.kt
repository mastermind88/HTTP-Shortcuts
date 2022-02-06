package ch.rmy.android.http_shortcuts.activities.main

sealed interface ChildViewModelEvent {
    data class MovingModeChanged(val enabled: Boolean) : ChildViewModelEvent

    object ShortcutEdited : ChildViewModelEvent
}