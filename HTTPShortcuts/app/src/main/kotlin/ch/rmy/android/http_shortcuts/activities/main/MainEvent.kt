package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.utils.LauncherShortcut
import ch.rmy.android.http_shortcuts.utils.text.Localizable

abstract class MainEvent : ViewModelEvent() {
    object ShowCreationDialog : MainEvent()

    data class ShowToolbarTitleChangeDialog(val oldTitle: String) : MainEvent()

    object ScheduleExecutions : MainEvent()

    data class UpdateLauncherShortcuts(val shortcuts: List<LauncherShortcut>) : MainEvent()

    object ShowChangeLogDialogIfNeeded : MainEvent()

    data class ShowUnlockDialog(val message: Localizable) : MainEvent()

    data class ShowShortcutPlacementDialog(val shortcutId: String) : MainEvent()
}