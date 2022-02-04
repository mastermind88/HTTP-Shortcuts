package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.extensions.takeUnlessEmpty
import ch.rmy.android.http_shortcuts.extensions.toLocalizable
import ch.rmy.android.http_shortcuts.utils.SelectionMode
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

data class MainViewState(
    val toolbarTitle: String = "",
    val isLocked: Boolean = false,
    val categoryTabItems: List<CategoryTabItem> = emptyList(),
    val selectionMode: SelectionMode = SelectionMode.NORMAL,
    val isInMovingMode: Boolean = false,
    val activeCategoryId: String = "",
) {
    val isRegularMenuButtonVisible
        get() = !isLocked

    val isUnlockButtonVisible
        get() = isLocked

    val isCreateButtonVisible
        get() = !isLocked && !isInMovingMode

    val isTabBarVisible
        get() = categoryTabItems.size > 1

    val isToolbarScrollable
        get() = categoryTabItems.size > 1

    val toolbarTitleLocalizable: Localizable
        get() = toolbarTitle.takeUnlessEmpty()?.toLocalizable() ?: StringResLocalizable(R.string.app_name)

    val activeCategoryIndex: Int
        get() = categoryTabItems.indexOfFirst { category ->
            category.categoryId == activeCategoryId
        }
            .takeUnless { it == -1 }
            ?: 0
}