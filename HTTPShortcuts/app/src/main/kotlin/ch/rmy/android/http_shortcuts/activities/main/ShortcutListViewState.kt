package ch.rmy.android.http_shortcuts.activities.main

data class ShortcutListViewState(
    val inMovingMode: Boolean = false,
) {
    val isDraggingEnabled
        get() = inMovingMode
}