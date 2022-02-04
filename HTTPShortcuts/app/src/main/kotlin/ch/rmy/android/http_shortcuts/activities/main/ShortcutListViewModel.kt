package ch.rmy.android.http_shortcuts.activities.main

import android.app.Application
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.data.domains.pending_executions.PendingExecutionsRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.widgets.WidgetsRepository
import ch.rmy.android.http_shortcuts.utils.SelectionMode

class ShortcutListViewModel(application: Application) : BaseViewModel<ShortcutListViewState>(application) {

    private val shortcutRepository = ShortcutRepository()
    private val pendingExecutionsRepository = PendingExecutionsRepository()
    private val widgetsRepository = WidgetsRepository()

    fun initialize(categoryId: String, selectionMode: SelectionMode) {
        initialize()
    }

    override fun initViewState() = ShortcutListViewState()

    fun onPaused() {
        if (currentViewState.inMovingMode) {
            updateViewState {
                copy(inMovingMode = false)
            }
        }
    }

    fun onBackPressed() {
        if (currentViewState.inMovingMode) {
            updateViewState {
                copy(inMovingMode = false)
            }
        } else {
            finish()
        }
    }

    fun onMoveModeOptionSelected() {
        updateViewState {
            copy(inMovingMode = true)
        }
        showSnackbar(R.string.message_moving_enabled, long = true)
    }

    fun onShortcutMoved(oldPosition: Int, newPosition: Int) {
        // TODO
    }

    /*
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
