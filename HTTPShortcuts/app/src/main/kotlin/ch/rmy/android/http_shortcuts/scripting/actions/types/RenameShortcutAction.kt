package ch.rmy.android.http_shortcuts.scripting.actions.types

import android.content.Context
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.exceptions.ActionException
import ch.rmy.android.http_shortcuts.extensions.truncate
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager
import ch.rmy.android.http_shortcuts.variables.VariableManager
import ch.rmy.android.http_shortcuts.variables.Variables
import ch.rmy.android.http_shortcuts.widget.WidgetManager
import io.reactivex.Completable

class RenameShortcutAction(private val name: String, private val shortcutNameOrId: String?) : BaseAction() {

    override fun execute(executionContext: ExecutionContext): Completable =
        renameShortcut(
            executionContext.context,
            this.shortcutNameOrId ?: executionContext.shortcutId,
            executionContext.variableManager,
        )

    private fun renameShortcut(context: Context, shortcutNameOrId: String, variableManager: VariableManager): Completable {
        val newName = Variables.rawPlaceholdersToResolvedValues(name, variableManager.getVariableValuesByIds())
            .trim()
            .truncate(Shortcut.NAME_MAX_LENGTH)
        if (newName.isEmpty()) {
            return Completable.complete()
        }
        val shortcut = DataSource.getShortcutByNameOrId(shortcutNameOrId)
            ?: return Completable
                .error(
                    ActionException {
                        it.getString(R.string.error_shortcut_not_found_for_renaming, shortcutNameOrId)
                    }
                )
        return renameShortcut(shortcut.id, newName)
            .andThen(
                Completable.fromAction {
                    if (LauncherShortcutManager.supportsPinning(context)) {
                        LauncherShortcutManager.updatePinnedShortcut(
                            context = context,
                            shortcutId = shortcut.id,
                            shortcutName = newName,
                            shortcutIcon = shortcut.icon,
                        )
                    }
                    WidgetManager.updateWidgets(context, shortcut.id)
                }
            )
    }

    companion object {

        private fun renameShortcut(shortcutId: String, newName: String) =
            Transactions.commit { realm ->
                Repository.getShortcutById(realm, shortcutId)?.name = newName
            }
    }
}
