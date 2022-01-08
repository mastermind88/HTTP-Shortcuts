package ch.rmy.android.http_shortcuts.data.domains.categories

import ch.rmy.android.http_shortcuts.data.BaseRepository
import ch.rmy.android.http_shortcuts.data.domains.getBase
import ch.rmy.android.http_shortcuts.data.domains.getCategoryById
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.utils.UUIDUtils.newUUID
import io.reactivex.Completable
import io.reactivex.Observable

class CategoryRepository : BaseRepository() {

    fun getObservableCategories(): Observable<List<Category>> =
        observeItem {
            getBase()
        }
            .map { base ->
                base.categories
            }

    fun createCategory(name: String): Completable =
        commitTransaction {
            val base = getBase()
                .findFirst()
                ?: return@commitTransaction
            val categories = base.categories
            val category = Category(name)
            category.id = newUUID()
            categories.add(copy(category))
        }

    fun deleteCategory(categoryId: String): Completable =
        commitTransaction {
            val category = getCategoryById(categoryId)
                .findFirst()
                ?: return@commitTransaction
            for (shortcut in category.shortcuts) {
                shortcut.headers.deleteAllFromRealm()
                shortcut.parameters.deleteAllFromRealm()
            }
            category.shortcuts.deleteAllFromRealm()
            category.deleteFromRealm()
        }

    fun setBackground(categoryId: String, background: String): Completable =
        commitTransaction {
            getCategoryById(categoryId)
                .findFirst()
                ?.background = background
        }

    fun renameCategory(categoryId: String, newName: String): Completable =
        commitTransaction {
            getCategoryById(categoryId)
                .findFirst()
                ?.name = newName
        }

    fun toggleCategoryHidden(categoryId: String, hidden: Boolean): Completable =
        commitTransaction {
            getCategoryById(categoryId)
                .findFirst()
                ?.hidden = hidden
        }

    fun setLayoutType(categoryId: String, layoutType: String): Completable =
        commitTransaction {
            getCategoryById(categoryId)
                .findFirst()
                ?.layoutType = layoutType
        }

    fun moveCategory(categoryId: String, position: Int): Completable =
        commitTransaction {
            val base = getBase().findFirst() ?: return@commitTransaction
            val category = getCategoryById(categoryId).findFirst() ?: return@commitTransaction
            val categories = base.categories
            val oldPosition = categories.indexOf(category)
            categories.move(oldPosition, position)
        }

}
