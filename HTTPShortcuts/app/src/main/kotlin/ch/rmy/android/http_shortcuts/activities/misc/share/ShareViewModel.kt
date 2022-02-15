package ch.rmy.android.http_shortcuts.activities.misc.share

import android.net.Uri
import androidx.annotation.StringRes
import ch.rmy.android.http_shortcuts.Application
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.ExecuteActivity
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.mapFor
import ch.rmy.android.http_shortcuts.variables.VariableLookup
import ch.rmy.android.http_shortcuts.variables.VariableManager
import ch.rmy.android.http_shortcuts.variables.VariableResolver

class ShareViewModel(application: Application) : BaseViewModel<Unit>(application) {

    private val shortcutRepository = ShortcutRepository()
    private val variableRepository = VariableRepository()

    private var initialized = false

    private lateinit var shortcuts: List<Shortcut>
    private lateinit var variables: List<Variable>
    private lateinit var text: String
    private lateinit var fileUris: List<Uri>

    override fun initViewState() = Unit

    fun initialize(text: String?, fileUris: List<Uri>) {
        if (initialized) {
            return
        }
        initialized = true
        this.text = text ?: ""
        this.fileUris = fileUris
        shortcutRepository.getShortcuts()
            .subscribe { shortcuts ->
                this.shortcuts = shortcuts
                variableRepository.getVariables()
                    .subscribe { variables ->
                        this.variables = variables
                        initialize()
                    }
                    .attachTo(destroyer)
            }
            .attachTo(destroyer)
    }

    override fun onInitialized() {
        if (text.isEmpty()) {
            handleFileSharing()
        } else {
            handleTextSharing()
        }
    }

    private fun handleTextSharing() {
        val variableLookup = VariableManager(variables)
        val variables = getTargetableVariablesForTextSharing()
        val variableIds = variables.map { it.id }.toSet()
        val shortcuts = getTargetableShortcutsForTextSharing(variableIds, variableLookup)

        val variableValues = variables.associate { variable -> variable.key to text }
        when (shortcuts.size) {
            0 -> showInstructions(R.string.error_not_suitable_shortcuts)
            1 -> {
                executeShortcut(shortcuts[0].id, variableValues = variableValues)
                finish(skipAnimation = true)
            }
            else -> {
                showShortcutSelection(shortcuts, variableValues = variableValues)
            }
        }
    }

    private fun getTargetableVariablesForTextSharing() =
        variables
            .filter { it.isShareText }
            .toSet()

    private fun getTargetableShortcutsForTextSharing(variableIds: Set<String>, variableLookup: VariableLookup): List<Shortcut> =
        shortcuts
            .filter { it.hasShareVariable(variableIds, variableLookup) }

    private fun getTargetableShortcutsForFileSharing(): List<Shortcut> =
        shortcuts
            .filter { it.hasFileParameter() || it.usesFileBody() }

    private fun handleFileSharing() {
        val shortcutsForFileSharing = getTargetableShortcutsForFileSharing()
        when (shortcutsForFileSharing.size) {
            0 -> {
                showInstructions(R.string.error_not_suitable_shortcuts)
            }
            1 -> {
                executeShortcut(shortcutsForFileSharing[0].id)
                finish(skipAnimation = true)
            }
            else -> showShortcutSelection(shortcutsForFileSharing)
        }
    }

    private fun executeShortcut(shortcutId: String, variableValues: Map<String, String> = emptyMap()) {
        openActivity(
            ExecuteActivity.IntentBuilder(shortcutId)
                .variableValues(variableValues)
                .files(fileUris)
        )
    }

    private fun showInstructions(@StringRes text: Int) {
        emitEvent(
            ViewModelEvent.ShowDialog { context ->
                DialogBuilder(context)
                    .message(text)
                    .dismissListener {
                        onInstructionsDismissed()
                    }
                    .positive(R.string.dialog_ok)
                    .showIfPossible()
            }
        )
    }

    private fun onInstructionsDismissed() {
        finish(skipAnimation = true)
    }

    private fun showShortcutSelection(shortcuts: List<Shortcut>, variableValues: Map<String, String> = emptyMap()) {
        emitEvent(
            ViewModelEvent.ShowDialog { context ->
                DialogBuilder(context)
                    .mapFor(shortcuts) { shortcut ->
                        item(name = shortcut.name, shortcutIcon = shortcut.icon) {
                            executeShortcut(shortcut.id, variableValues)
                        }
                    }
                    .dismissListener {
                        onShortcutSelectionDismissed()
                    }
                    .showIfPossible()
            }
        )
    }

    private fun onShortcutSelectionDismissed() {
        finish(skipAnimation = true)
    }

    companion object {

        private fun Shortcut.hasShareVariable(variableIds: Set<String>, variableLookup: VariableLookup): Boolean {
            val variableIdsInShortcut = VariableResolver.extractVariableIds(this, variableLookup)
            return variableIds.any { variableIdsInShortcut.contains(it) }
        }

        private fun Shortcut.hasFileParameter(): Boolean =
            parameters.any { it.isFileParameter || it.isFilesParameter }
    }
}