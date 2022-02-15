package ch.rmy.android.http_shortcuts.activities.editor.shortcuts

import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholder

data class TriggerShortcutsViewState(
    val triggerShortcuts: List<ShortcutPlaceholder> = emptyList(),
) {
    val isDraggingEnabled: Boolean
        get() = triggerShortcuts.size > 1
}
