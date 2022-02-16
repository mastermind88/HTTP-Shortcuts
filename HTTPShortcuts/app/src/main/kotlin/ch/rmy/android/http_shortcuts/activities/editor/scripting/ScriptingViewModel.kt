package ch.rmy.android.http_shortcuts.activities.editor.scripting

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.utils.localization.StringResLocalizable
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.type
import ch.rmy.android.http_shortcuts.utils.ExternalURLs

class ScriptingViewModel(application: Application) : BaseViewModel<Unit, ScriptingViewState>(application) {

    private val temporaryShortcutRepository = TemporaryShortcutRepository()
    private val shortcutRepository = ShortcutRepository()
    private val variableRepository = VariableRepository()

    private lateinit var shortcut: Shortcut

    override fun initViewState() = ScriptingViewState()

    override fun onInitialized() {
        temporaryShortcutRepository.getTemporaryShortcut()
            .subscribe(
                ::initViewStateFromShortcut,
                ::onInitializationError,
            )
            .attachTo(destroyer)

        shortcutRepository.getObservableShortcuts()
            .subscribe { shortcuts ->
                updateViewState {
                    copy(shortcuts = shortcuts)
                }
            }
            .attachTo(destroyer)

        variableRepository.getObservableVariables()
            .subscribe { variables ->
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    private fun initViewStateFromShortcut(shortcut: Shortcut) {
        this.shortcut = shortcut
        val type = shortcut.type
        updateViewState {
            copy(
                codeOnPrepare = shortcut.codeOnPrepare,
                codeOnSuccess = shortcut.codeOnSuccess,
                codeOnFailure = shortcut.codeOnFailure,
                codePrepareMinLines = getMinLinesForCode(type),
                codePrepareHint = StringResLocalizable(getHintText(type)),
                codePrepareVisible = type != ShortcutExecutionType.SCRIPTING,
                postRequestScriptingVisible = type.usesResponse,
            )
        }
    }

    private fun onInitializationError(error: Throwable) {
        // TODO: Handle error better
        logException(error)
        finish()
    }

    fun onAddCodeSnippetPrepareButtonClicked() {
        emitEvent(
            ScriptingEvent.ShowCodeSnippetPicker(
                target = ScriptingEvent.ShowCodeSnippetPicker.Target.PREPARE,
                includeResponseOptions = false,
                includeFileOptions = shortcut.type != ShortcutExecutionType.SCRIPTING,
                includeNetworkErrorOption = false,
            )
        )
    }

    fun onAddCodeSnippetSuccessButtonClicked() {
        emitEvent(
            ScriptingEvent.ShowCodeSnippetPicker(
                target = ScriptingEvent.ShowCodeSnippetPicker.Target.SUCCESS,
                includeResponseOptions = true,
                includeFileOptions = shortcut.type != ShortcutExecutionType.SCRIPTING,
                includeNetworkErrorOption = false,
            )
        )
    }

    fun onAddCodeSnippetFailureButtonClicked() {
        emitEvent(
            ScriptingEvent.ShowCodeSnippetPicker(
                target = ScriptingEvent.ShowCodeSnippetPicker.Target.FAILURE,
                includeResponseOptions = true,
                includeFileOptions = shortcut.type != ShortcutExecutionType.SCRIPTING,
                includeNetworkErrorOption = true,
            )
        )
    }

    fun onCodePrepareChanged(code: String) {
        updateViewState {
            copy(
                codeOnPrepare = code,
            )
        }
        performOperation(
            temporaryShortcutRepository.setCodeOnPrepare(code)
        )
    }

    fun onCodeSuccessChanged(code: String) {
        updateViewState {
            copy(
                codeOnSuccess = code,
            )
        }
        performOperation(
            temporaryShortcutRepository.setCodeOnSuccess(code)
        )
    }

    fun onCodeFailureChanged(code: String) {
        updateViewState {
            copy(
                codeOnFailure = code,
            )
        }
        performOperation(
            temporaryShortcutRepository.setCodeOnFailure(code)
        )
    }

    fun onHelpButtonClicked() {
        openURL(ExternalURLs.SCRIPTING_DOCUMENTATION)
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish()
        }
    }

    companion object {
        private fun getMinLinesForCode(type: ShortcutExecutionType) = if (type == ShortcutExecutionType.SCRIPTING) {
            18
        } else {
            6
        }

        private fun getHintText(type: ShortcutExecutionType) = if (type == ShortcutExecutionType.SCRIPTING) {
            R.string.placeholder_javascript_code_generic
        } else {
            R.string.placeholder_javascript_code_before
        }
    }
}
