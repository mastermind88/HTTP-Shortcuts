package ch.rmy.android.http_shortcuts.activities.main

import android.app.Application
import android.net.Uri
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.ExecuteActivity
import ch.rmy.android.http_shortcuts.activities.editor.ShortcutEditorActivity
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryRepository
import ch.rmy.android.http_shortcuts.data.domains.pending_executions.PendingExecutionsRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.widgets.WidgetsRepository
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.data.models.PendingExecution
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.context
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.extensions.move
import ch.rmy.android.http_shortcuts.extensions.showSnackbar
import ch.rmy.android.http_shortcuts.scheduling.ExecutionScheduler
import ch.rmy.android.http_shortcuts.utils.SelectionMode
import ch.rmy.android.http_shortcuts.utils.Settings
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

class ShortcutListViewModel(application: Application) : BaseViewModel<ShortcutListViewState>(application) {

    private val shortcutRepository = ShortcutRepository()
    private val categoryRepository = CategoryRepository()
    private val pendingExecutionsRepository = PendingExecutionsRepository()
    private val widgetsRepository = WidgetsRepository()
    private val eventBridge = ChildViewModelEventBridge()

    private val settings = Settings(context)

    private lateinit var selectionMode: SelectionMode
    private lateinit var category: Category
    private var categories: List<Category> = emptyList()
    private var pendingShortcuts: List<PendingExecution> = emptyList()

    fun initialize(categoryId: String, selectionMode: SelectionMode) {
        this.selectionMode = selectionMode
        categoryRepository.getCategory(categoryId)
            .subscribe(
                { category ->
                    this.category = category // TODO: Update this whenever the category or its shortcuts change
                    initialize()
                },
                ::onInitializationError,
            )
            .attachTo(destroyer)

        categoryRepository.getCategories()
            .subscribe { categories ->
                this.categories = categories
            }
            .attachTo(destroyer)

        pendingExecutionsRepository.getObservablePendingExecutions()
            .subscribe { pendingShortcuts ->
                this.pendingShortcuts = pendingShortcuts
                // TODO: Recompute shortcut list
            }
            .attachTo(destroyer)
    }

    private fun onInitializationError(error: Throwable) {
        // TODO: Handle error better
        logException(error)
    }

    override fun initViewState() = ShortcutListViewState(
        shortcuts = emptyList(), // TODO
        background = category.categoryBackgroundType,
    )

    fun onPaused() {
        if (currentViewState.isInMovingMode) {
            disableMovingMode()
        }
    }

    fun onBackPressed() {
        if (currentViewState.isInMovingMode) {
            disableMovingMode()
        } else {
            finish() // TODO: This might not need to be here
        }
    }

    fun onMoveModeOptionSelected() {
        enableMovingMode()
    }

    private fun enableMovingMode() {
        updateViewState {
            copy(isInMovingMode = true)
        }
        eventBridge.submit(ChildViewModelEvent.MovingModeChanged(true))
        showSnackbar(R.string.message_moving_enabled, long = true)
    }

    private fun disableMovingMode() {
        updateViewState {
            copy(isInMovingMode = false)
        }
        eventBridge.submit(ChildViewModelEvent.MovingModeChanged(false))
    }

    fun onShortcutMoved(oldPosition: Int, newPosition: Int) {
        val shortcutListItem = currentViewState.shortcuts[oldPosition] as? ShortcutListItem.Shortcut ?: return
        updateViewState {
            copy(shortcuts = shortcuts.move(oldPosition, newPosition))
        }
        performOperation(
            shortcutRepository.moveShortcut(shortcutListItem.id, newPosition)
        )
    }

    fun onShortcutClicked(shortcutId: String) {
        if (currentViewState.isInMovingMode) {
            showSnackbar(R.string.message_moving_enabled)
            return
        }
        if (selectionMode != SelectionMode.NORMAL) {
            selectShortcut(shortcutId)
            return
        }
        if (currentViewState.isAppLocked) {
            executeShortcut(shortcutId)
            return
        }
        when (settings.clickBehavior) {
            Settings.CLICK_BEHAVIOR_RUN -> executeShortcut(shortcutId)
            Settings.CLICK_BEHAVIOR_EDIT -> editShortcut(shortcutId)
            Settings.CLICK_BEHAVIOR_MENU -> showContextMenu(shortcutId)
        }
    }

    private fun selectShortcut(shortcutId: String) {

        // TODO
    }

    private fun executeShortcut(shortcutId: String) {
        openActivity(ExecuteActivity.IntentBuilder(shortcutId))
    }

    private fun editShortcut(shortcutId: String) {
        openActivity(
            ShortcutEditorActivity.IntentBuilder()
                .categoryId(category.id)
                .shortcutId(shortcutId),
            requestCode = ListFragment.REQUEST_EDIT_SHORTCUT,
        )
    }

    private fun showContextMenu(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        emitEvent(ShortcutListEvent.ShowContextMenu(
            shortcutId,
            title = shortcut.name,
            isPending = pendingShortcuts.any { it.shortcutId == shortcut.id },
            isMovable = canMoveWithinCategory() || canMoveAcrossCategories(),
        ))
    }

    private fun canMoveWithinCategory() =
        category.shortcuts.size > 1

    private fun canMoveAcrossCategories() =
        categories.size > 1

    fun onShortcutLongClicked(shortcutId: String) {
        if (currentViewState.isLongClickingEnabled) {
            showContextMenu(shortcutId)
        }
    }

    private fun getShortcutById(shortcutId: String): Shortcut? =
        category.shortcuts.firstOrNull { it.id == shortcutId }

    fun onPlaceOnHomeScreenOptionSelected(shortcutId: String) {
        TODO("Not yet implemented")
        // tabHost?.placeShortcutOnHomeScreen(shortcutData.value ?: return@item)
    }

    fun onExecuteOptionSelected(shortcutId: String) {
        executeShortcut(shortcutId)
    }

    fun onCancelPendingExecutionOptionSelected(shortcutId: String) {
        cancelPendingExecution(shortcutId)
    }

    private fun cancelPendingExecution(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        pendingExecutionsRepository.removePendingExecution(shortcutId)
            .subscribe {
                showSnackbar(StringResLocalizable(R.string.pending_shortcut_execution_cancelled, shortcut.name))
                ExecutionScheduler.schedule(context)
            }
            .attachTo(destroyer)
    }

    fun onEditOptionSelected(shortcutId: String) {
        editShortcut(shortcutId)
    }

    fun onMoveOptionSelected(shortcutId: String) {
        val canMoveWithinCategory = canMoveWithinCategory()
        val canMoveAcrossCategories = canMoveAcrossCategories()
        when {
            canMoveWithinCategory && canMoveAcrossCategories -> emitEvent(ShortcutListEvent.ShowMoveOptionsDialog(shortcutId))
            canMoveWithinCategory -> enableMovingMode()
            canMoveAcrossCategories -> onMoveToCategoryOptionSelected(shortcutId)
        }
    }

    fun onDuplicateOptionSelected(shortcutId: String) {
        TODO("Not yet implemented")
        // duplicateShortcut(shortcutData.value ?: return@item)
    }

    fun onDeleteOptionSelected(shortcutId: String) {
        TODO("Not yet implemented")
        // showDeleteDialog(shortcutData)
    }

    fun onShowInfoOptionSelected(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        emitEvent(ShortcutListEvent.ShowShortcutInfoDialog(shortcut.id, shortcut.name))
    }

    fun onExportOptionSelected(shortcutId: String) {
        TODO("Not yet implemented")
        // showExportChoiceDialog(shortcutData)
    }

    fun onMoveToCategoryOptionSelected(shortcutId: String) {
        TODO("Not yet implemented")
        // showMoveToCategoryDialog(shortcutData)
    }

    fun onMoveTargetCategorySelected(shortcutId: String, categoryId: String) {
        TODO("Not yet implemented")
        // showSnackbar(String.format(getString(R.string.shortcut_moved), name))
    }

    fun onShortcutEdited() {
        eventBridge.submit(ChildViewModelEvent.ShortcutEdited)
    }

    fun onExportDestinationSelected(uri: Uri) {
        TODO("Not yet implemented")
        // startExport(intent?.data ?: return, viewModel.exportedShortcutId!!)
    }

    /*
    // TODO
    fun deleteShortcut(shortcutId: String) =
        shortcutRepository.deleteShortcut(shortcutId)
            .mergeWith(pendingExecutionsRepository.removePendingExecution(shortcutId))
            .andThen(widgetsRepository.deleteDeadWidgets())
            .observeOn(AndroidSchedulers.mainThread())

    fun removePendingExecution(shortcutId: String) =
        pendingExecutionsRepository
            .removePendingExecution(shortcutId)
            .observeOn(AndroidSchedulers.mainThread())

    fun duplicateShortcut(shortcutId: String, newName: String, newPosition: Int?, categoryId: String) =
        shortcutRepository.duplicateShortcut(shortcutId, newName, newPosition, categoryId)
     */
}
