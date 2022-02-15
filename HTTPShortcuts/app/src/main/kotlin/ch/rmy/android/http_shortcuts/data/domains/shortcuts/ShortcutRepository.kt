package ch.rmy.android.http_shortcuts.data.domains.shortcuts

import ch.rmy.android.http_shortcuts.data.BaseRepository
import ch.rmy.android.http_shortcuts.data.RealmTransactionContext
import ch.rmy.android.http_shortcuts.data.domains.getBase
import ch.rmy.android.http_shortcuts.data.domains.getCategoryById
import ch.rmy.android.http_shortcuts.data.domains.getShortcutById
import ch.rmy.android.http_shortcuts.data.domains.getShortcutByNameOrId
import ch.rmy.android.http_shortcuts.data.domains.getTemporaryShortcut
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.detachFromRealm
import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import ch.rmy.android.http_shortcuts.utils.UUIDUtils.newUUID
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class ShortcutRepository : BaseRepository() {

    fun getShortcutById(shortcutId: String): Single<Shortcut> =
        query {
            getShortcutById(shortcutId)
        }
            .map { it.first() }

    fun getShortcutByNameOrId(shortcutNameOrId: String): Single<Shortcut> =
        query {
            getShortcutByNameOrId(shortcutNameOrId)
        }
            .map { it.first() }

    fun getObservableShortcuts(): Observable<List<Shortcut>> =
        TODO()
    // TODO: Exclude temporary shortcut?

    fun getShortcuts(): Single<List<Shortcut>> {
        TODO()
    }

    fun moveShortcut(shortcutId: String, targetPosition: Int? = null, targetCategoryId: String? = null) =
        commitTransaction {
            moveShortcut(shortcutId, targetPosition, targetCategoryId)
        }

    private fun RealmTransactionContext.moveShortcut(shortcutId: String, targetPosition: Int? = null, targetCategoryId: String? = null) {
        val shortcut = getShortcutById(shortcutId)
            .findFirst() ?: return
        val categories = getBase()
            .findFirst()
            ?.categories ?: return
        val targetCategory = if (targetCategoryId != null) {
            getCategoryById(targetCategoryId)
                .findFirst()
        } else {
            categories.first { category -> category.shortcuts.any { it.id == shortcutId } }
        } ?: return

        for (category in categories) {
            category.shortcuts.remove(shortcut)
        }
        if (targetPosition != null) {
            targetCategory.shortcuts.add(targetPosition, shortcut)
        } else {
            targetCategory.shortcuts.add(shortcut)
        }
    }

    fun duplicateShortcut(shortcutId: String, newName: String, newPosition: Int?, categoryId: String) =
        commitTransaction {
            val shortcut = getShortcutById(shortcutId)
                .findFirst()
                ?: return@commitTransaction
            val newShortcut = copyShortcut(shortcut, newUUID())
            newShortcut.name = newName
            moveShortcut(newShortcut.id, newPosition, categoryId)
        }

    fun createTemporaryShortcutFromShortcut(shortcutId: String) =
        commitTransaction {
            val shortcut = getShortcutById(shortcutId)
                .findFirst()!!
            copyShortcut(shortcut, Shortcut.TEMPORARY_ID)
        }

    fun copyTemporaryShortcutToShortcut(shortcutId: String, categoryId: String?) =
        commitTransaction {
            val temporaryShortcut = getTemporaryShortcut()
                .findFirst() ?: return@commitTransaction
            val shortcut = copyShortcut(temporaryShortcut, shortcutId)
            if (categoryId != null) {
                val category = getCategoryById(categoryId)
                    .findFirst()
                    ?: return@commitTransaction
                category.shortcuts.add(shortcut)
            }
        }

    private fun RealmTransactionContext.copyShortcut(sourceShortcut: Shortcut, targetShortcutId: String): Shortcut =
        sourceShortcut.detachFromRealm()
            .apply {
                id = targetShortcutId
                parameters.forEach { parameter ->
                    parameter.id = newUUID()
                }
                headers.forEach { header ->
                    header.id = newUUID()
                }
                responseHandling?.id = newUUID()
            }
            .let {
                copyOrUpdate(it)
            }

    fun deleteShortcut(shortcutId: String) =
        commitTransaction {
            getShortcutById(shortcutId)
                .findFirst()
                ?.apply {
                    headers.deleteAllFromRealm()
                    parameters.deleteAllFromRealm()
                    responseHandling?.deleteFromRealm()
                    deleteFromRealm()
                }
        }

    fun setIcon(shortcutId: String, icon: ShortcutIcon): Completable =
        commitTransaction {
            getShortcutById(shortcutId)
                .findFirst()
                ?.icon = icon
        }

    fun setName(shortcutId: String, name: String): Completable =
        commitTransaction {
            getShortcutById(shortcutId)
                .findFirst()
                ?.name = name
        }

}