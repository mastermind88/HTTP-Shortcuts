package ch.rmy.android.http_shortcuts.activities.categories

import android.app.Activity
import android.app.Application
import android.content.Intent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryRepository
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.context
import ch.rmy.android.http_shortcuts.extensions.move
import ch.rmy.android.http_shortcuts.extensions.toLocalizable
import ch.rmy.android.http_shortcuts.utils.CategoryBackgroundType
import ch.rmy.android.http_shortcuts.utils.CategoryLayoutType
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager
import ch.rmy.android.http_shortcuts.utils.text.QuantityStringLocalizable
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

class CategoriesViewModel(application: Application) : BaseViewModel<CategoriesViewState>(application) {

    private val categoryRepository: CategoryRepository = CategoryRepository()
    private val launcherShortcutManager = LauncherShortcutManager

    private lateinit var categories: Set<Category>
    private var hasChanged = false

    override fun initViewState() = CategoriesViewState()

    override fun onInitialized() {
        categoryRepository.getObservableCategories()
            .subscribe { categories ->
                this.categories = categories.toSet()
                updateViewState {
                    copy(categories = mapCategories(categories))
                }
            }
            .attachTo(destroyer)
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish(hasChanged)
        }
    }

    private fun finish(hasChanges: Boolean) {
        finish(
            result = Activity.RESULT_OK,
            intent = Intent().apply {
                putExtra(CategoriesActivity.EXTRA_CATEGORIES_CHANGED, hasChanges)
            },
        )
    }

    fun onCategoryClicked(categoryId: String) {
        showContextMenu(categoryId)
    }

    private fun showContextMenu(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        emitEvent(CategoriesEvent.ShowContextMenu(
            categoryId = category.id,
            title = category.name.toLocalizable(),
            hideOptionVisible = !category.hidden && categories.count { !it.hidden } > 1,
            showOptionVisible = category.hidden,
            changeLayoutTypeOptionVisible = !category.hidden,
            placeOnHomeScreenOptionVisible = !category.hidden && launcherShortcutManager.supportsPinning(context),
            deleteOptionVisible = categories.size > 1,
        ))
    }

    private fun getCategory(categoryId: String) =
        categories.firstOrNull { it.id == categoryId }

    fun onCategoryMoved(oldPosition: Int, newPosition: Int) {
        val categoryListItem = currentViewState.categories[oldPosition]
        val categoryId = categoryListItem.id
        updateViewState {
            copy(categories = categories.move(oldPosition, newPosition))
        }
        performOperation(
            categoryRepository.moveCategory(categoryId, newPosition)
        )
    }

    fun onHelpButtonClicked() {
        openURL(ExternalURLs.CATEGORIES_DOCUMENTATION)
    }

    fun onCreateCategoryButtonClicked() {
        emitEvent(CategoriesEvent.ShowCreateCategoryDialog)
    }

    fun onCreateDialogConfirmed(name: String) {
        performOperation(
            categoryRepository.createCategory(name)
                .doOnComplete {
                    showSnackbar(R.string.message_category_created)
                }
        )
    }

    fun onRenameCategoryOptionSelected(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        emitEvent(CategoriesEvent.ShowRenameDialog(categoryId, prefill = category.name))
    }

    fun onRenameDialogConfirmed(categoryId: String, newName: String) {
        performOperation(
            categoryRepository.renameCategory(categoryId, newName)
                .doOnComplete {
                    LauncherShortcutManager.updatePinnedCategoryShortcut(context, categoryId, newName)
                    showSnackbar(R.string.message_category_renamed)
                }
        )
    }

    fun onCategoryVisibilityChanged(categoryId: String, hidden: Boolean) {
        performOperation(
            categoryRepository.toggleCategoryHidden(categoryId, hidden)
                .doOnComplete {
                    showSnackbar(if (hidden) R.string.message_category_hidden else R.string.message_category_visible)
                }
        )
    }

    fun onCategoryDeletionConfirmed(categoryId: String) {
        performOperation(
            categoryRepository.deleteCategory(categoryId)
                .doOnComplete {
                    showSnackbar(R.string.message_category_deleted)
                }
        )
    }

    fun onCategoryDeletionSelected(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        if (category.shortcuts.isEmpty()) {
            onCategoryDeletionConfirmed(categoryId)
        } else {
            emitEvent(CategoriesEvent.ShowDeleteDialog(categoryId))
        }
    }

    fun onPlaceOnHomeScreenSelected(categoryId: String) {
        val category = getCategory(categoryId) ?: return
        LauncherShortcutManager.pinCategory(context, category.id, category.name)
    }

    fun onLayoutTypeChanged(categoryId: String, layoutType: CategoryLayoutType) {
        performOperation(
            categoryRepository.setLayoutType(categoryId, layoutType)
                .doOnComplete {
                    showSnackbar(R.string.message_layout_type_changed)
                }
        )
    }

    fun onBackgroundTypeChanged(categoryId: String, backgroundType: CategoryBackgroundType) {
        performOperation(
            categoryRepository.setBackground(categoryId, backgroundType)
                .doOnComplete {
                    showSnackbar(R.string.message_background_type_changed)
                }
        )
    }

    companion object {
        private const val MAX_ICONS = 5

        private fun mapCategories(categories: List<Category>): List<CategoryListItem> =
            categories.map { category ->
                CategoryListItem(
                    id = category.id,
                    name = if (category.hidden) {
                        StringResLocalizable(R.string.label_category_hidden, category.name)
                    } else {
                        category.name.toLocalizable()
                    },
                    description = QuantityStringLocalizable(
                        R.plurals.shortcut_count,
                        count = category.shortcuts.size,
                    ),
                    icons = category.shortcuts
                        .take(MAX_ICONS)
                        .map { shortcut ->
                            shortcut.icon
                        },
                    layoutType = category.categoryLayoutType.takeUnless { category.hidden },
                )
            }
    }

}

