package ch.rmy.android.http_shortcuts.activities.categories

data class CategoriesViewState(
    val categories: List<CategoryListItem> = emptyList(),
) {
    val isDraggingEnabled: Boolean
        get() = categories.size > 1
}