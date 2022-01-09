package ch.rmy.android.http_shortcuts.activities.settings.globalcode

import android.app.Application
import android.text.SpannableStringBuilder
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.data.domains.app.AppRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.color
import ch.rmy.android.http_shortcuts.extensions.takeUnlessEmpty
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholderProvider
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutSpanManager
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.Variables
import io.reactivex.android.schedulers.AndroidSchedulers

class GlobalScriptingViewModel(application: Application) : BaseViewModel<GlobalScriptingViewState>(application) {

    private val appRepository = AppRepository()
    private val shortcutRepository = ShortcutRepository()
    private val variableRepository = VariableRepository()

    private var hasChanges = false

    private val variablePlaceholderColor by lazy {
        color(application, R.color.variable)
    }
    private val shortcutPlaceholderColor by lazy {
        color(application, R.color.shortcut)
    }
    private val shortcutPlaceholderProvider = ShortcutPlaceholderProvider()
    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    private var shortcutsInitialized = false
    private var variablesInitialized = false
    private var globalCodeInitialized = false

    var iconPickerShortcutPlaceholder: String? = null

    override fun initViewState() = GlobalScriptingViewState()

    override fun onInitialized() {
        shortcutRepository.getObservableShortcuts()
            .subscribe { shortcuts ->
                shortcutPlaceholderProvider.shortcuts = shortcuts
                shortcutsInitialized = true
                updateViewState {
                    copy(shortcuts = shortcuts)
                }
            }
            .attachTo(destroyer)

        variableRepository.getObservableVariables()
            .subscribe { variables ->
                variablePlaceholderProvider.variables = variables
                variablesInitialized = true
                initializeGlobalCodeIfPossible()
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    private fun initializeGlobalCodeIfPossible() {
        if (globalCodeInitialized || !shortcutsInitialized || !variablesInitialized) {
            return
        }
        globalCodeInitialized = true
        appRepository.getGlobalCode()
            .subscribe { globalCode ->
                updateViewState {
                    copy(globalCode = processTextForView(globalCode))
                }
            }
            .attachTo(destroyer)
    }

    private fun processTextForView(input: String): CharSequence {
        val text = SpannableStringBuilder(input)
        Variables.applyVariableFormattingToJS(
            text,
            variablePlaceholderProvider,
            variablePlaceholderColor,
        )
        ShortcutSpanManager.applyShortcutFormattingToJS(
            text,
            shortcutPlaceholderProvider,
            shortcutPlaceholderColor,
        )
        return text
    }

    fun onHelpButtonClicked() {
        openURL(ExternalURLs.SCRIPTING_DOCUMENTATION)
    }

    fun onBackPressed() {
        if (currentViewState.saveButtonVisible) {
            emitEvent(ViewModelEvent.ShowDialog { context ->
                DialogBuilder(context)
                    .message(R.string.confirm_discard_changes_message)
                    .positive(R.string.dialog_discard) { onDiscardDialogConfirmed() }
                    .negative(R.string.dialog_cancel)
                    .showIfPossible()
            })
        } else {
            finish()
        }
    }

    private fun onDiscardDialogConfirmed() {
        finish()
    }

    fun onSaveButtonClicked() {
        appRepository.setGlobalCode(currentViewState.globalCode.toString().trim().takeUnlessEmpty())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                finish()
            }
            .attachTo(destroyer)
    }

    fun onGlobalCodeChanged(globalCode: CharSequence) {
        if (!globalCodeInitialized) {
            return
        }
        updateViewState {
            copy(
                globalCode = globalCode,
                saveButtonVisible = true,
            )
        }
    }
}
