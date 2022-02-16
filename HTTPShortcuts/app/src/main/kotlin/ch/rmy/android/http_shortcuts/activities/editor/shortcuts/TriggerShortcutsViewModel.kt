package ch.rmy.android.http_shortcuts.activities.editor.shortcuts

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.extensions.move
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholder
import ch.rmy.android.http_shortcuts.scripting.shortcuts.TriggerShortcutManager.getCodeFromTriggeredShortcutIds
import ch.rmy.android.http_shortcuts.scripting.shortcuts.TriggerShortcutManager.getTriggeredShortcutIdsFromCode

class TriggerShortcutsViewModel(application: Application) :
    BaseViewModel<TriggerShortcutsViewModel.InitData, TriggerShortcutsViewState>(application) {

    private val currentShortcutId
        get() = initData.currentShortcutId

    private var shortcutInitialized = false
    private lateinit var shortcutPlaceholders: List<ShortcutPlaceholder>

    private val temporaryShortcutRepository = TemporaryShortcutRepository()
    private val shortcutRepository = ShortcutRepository()

    override fun initViewState() = TriggerShortcutsViewState()

    override fun onInitialized() {
        shortcutRepository.getObservableShortcuts()
            .subscribe { shortcuts ->
                shortcutPlaceholders = shortcuts.map { ShortcutPlaceholder.fromShortcut(it) }
                if (!shortcutInitialized) {
                    shortcutInitialized = true
                    temporaryShortcutRepository.getTemporaryShortcut()
                        .subscribe(
                            ::initViewStateFromShortcut,
                            ::onInitializationError,
                        )
                        .attachTo(destroyer)
                }
            }
            .attachTo(destroyer)
    }

    private fun initViewStateFromShortcut(shortcut: Shortcut) {
        val triggerShortcuts = getTriggeredShortcutIdsFromCode(shortcut.codeOnPrepare)
            .map(::getShortcutPlaceholder)

        updateViewState {
            copy(
                triggerShortcuts = triggerShortcuts,
            )
        }
    }

    private fun getShortcutPlaceholder(shortcutId: String) =
        shortcutPlaceholders
            .firstOrNull { shortcut -> shortcut.id == shortcutId }
            ?: ShortcutPlaceholder.deletedShortcut(shortcutId)

    private fun onInitializationError(error: Throwable) {
        // TODO: Handle error better
        logException(error)
        finish()
    }

    fun onShortcutMoved(oldPosition: Int, newPosition: Int) {
        val newTriggerShortcuts = currentViewState.triggerShortcuts
            .move(oldPosition, newPosition)
        updateTriggerShortcuts(newTriggerShortcuts)
    }

    private fun updateTriggerShortcuts(newTriggerShortcuts: List<ShortcutPlaceholder>) {
        updateViewState {
            copy(triggerShortcuts = newTriggerShortcuts)
        }
        performOperation(
            temporaryShortcutRepository.setCodeOnPrepare(
                getCodeFromTriggeredShortcutIds(newTriggerShortcuts.map { it.id })
            )
        )
    }

    fun onAddButtonClicked() {
        val shortcutIdsInUse = currentViewState.triggerShortcuts.map { it.id }
        val placeholders = shortcutPlaceholders
            .filter { it.id != currentShortcutId && it.id !in shortcutIdsInUse }

        if (placeholders.isEmpty()) {
            showNoShortcutsError()
        } else {
            emitEvent(TriggerShortcutsEvent.ShowShortcutPickerForAdding(placeholders))
        }
    }

    private fun showNoShortcutsError() {
        emitEvent(
            ViewModelEvent.ShowDialog { context ->
                DialogBuilder(context)
                    .title(R.string.title_add_trigger_shortcut)
                    .message(R.string.error_add_trigger_shortcut_no_shortcuts)
                    .positive(R.string.dialog_ok)
                    .showIfPossible()
            }
        )
    }

    fun onAddShortcutDialogConfirmed(shortcutId: String) {
        val newTriggerShortcuts = currentViewState.triggerShortcuts
            .plus(getShortcutPlaceholder(shortcutId))
        updateTriggerShortcuts(newTriggerShortcuts)
    }

    fun onRemoveShortcutDialogConfirmed(shortcutId: String) {
        val newTriggerShortcuts = currentViewState.triggerShortcuts.filter { it.id != shortcutId }
        updateTriggerShortcuts(newTriggerShortcuts)
    }

    fun onShortcutClicked(shortcutId: String) {
        val shortcutPlaceholder = shortcutPlaceholders
            .firstOrNull { it.id == shortcutId }
            ?: return
        emitEvent(TriggerShortcutsEvent.ShowRemoveShortcutDialog(shortcutId, shortcutPlaceholder.name))
    }

    data class InitData(
        val currentShortcutId: String?,
    )
}
