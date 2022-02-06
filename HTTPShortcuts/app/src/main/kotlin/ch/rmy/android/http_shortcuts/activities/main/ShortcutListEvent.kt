package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent

abstract class ShortcutListEvent : ViewModelEvent() {
    data class ShowContextMenu(
        val shortcutId: String,
        val title: String,
        val isPending: Boolean,
        val isMovable: Boolean,
    ) : ViewModelEvent()

    data class ShowMoveOptionsDialog(val shortcutId: String) : ViewModelEvent()

    data class ShowMoveToCategoryDialog(val shortcutId: String, val categoryOptions: List<CategoryOption>) : ViewModelEvent() {
        data class CategoryOption(val categoryId: String, val name: String)
    }

    data class ShowShortcutInfoDialog(val shortcutId: String, val shortcutName: String) : ViewModelEvent()


}