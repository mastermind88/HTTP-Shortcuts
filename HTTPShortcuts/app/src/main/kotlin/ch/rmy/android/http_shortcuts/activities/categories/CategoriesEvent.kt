package ch.rmy.android.http_shortcuts.activities.categories

import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.utils.text.Localizable

abstract class CategoriesEvent : ViewModelEvent() {
    object ShowCreateCategoryDialog : CategoriesEvent()
    data class ShowContextMenu(
        val categoryId: String,
        val title: Localizable,
        val hideOptionVisible: Boolean,
        val showOptionVisible: Boolean,
        val changeLayoutTypeOptionVisible: Boolean,
        val placeOnHomeScreenOptionVisible: Boolean,
        val deleteOptionVisible: Boolean,
    ) : CategoriesEvent()

    data class ShowRenameDialog(
        val categoryId: String,
        val prefill: String,
    ) : CategoriesEvent()

    data class ShowDeleteDialog(
        val categoryId: String,
    ) : CategoriesEvent()
}