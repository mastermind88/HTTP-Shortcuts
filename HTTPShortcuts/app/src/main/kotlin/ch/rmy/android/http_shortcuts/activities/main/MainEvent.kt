package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent

abstract class MainEvent : ViewModelEvent() {
    object ShowCreationDialog : MainEvent()
}