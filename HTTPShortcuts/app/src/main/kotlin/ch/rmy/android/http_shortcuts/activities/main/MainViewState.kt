package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.utils.text.Localizable

data class MainViewState(
    val toolbarTitle: Localizable = Localizable.EMPTY,
    val isLocked: Boolean = false,
    val categoryTabItems: List<CategoryTabItem> = emptyList(),
    val isInMovingMode: Boolean = false,
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
}