package ch.rmy.android.http_shortcuts.activities.categories

import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import ch.rmy.android.http_shortcuts.utils.CategoryLayoutType
import ch.rmy.android.http_shortcuts.utils.text.Localizable

data class CategoryListItem(
    val id: String,
    val name: Localizable,
    val description: Localizable,
    val icons: List<ShortcutIcon>,
    val layoutType: CategoryLayoutType?,
)