package ch.rmy.android.http_shortcuts.activities.main

import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.extensions.toLauncherShortcut
import ch.rmy.android.http_shortcuts.utils.LauncherShortcut

class LauncherShortcutMapper {

    operator fun invoke(categories: List<Category>): List<LauncherShortcut> =
        categories.flatMap { category ->
            category.shortcuts
        }
            .filter { shortcut ->
                shortcut.launcherShortcut
            }
            .map { shortcut ->
                shortcut.toLauncherShortcut()
            }
}
