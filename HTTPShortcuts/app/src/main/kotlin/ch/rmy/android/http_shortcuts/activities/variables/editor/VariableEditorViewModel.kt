package ch.rmy.android.http_shortcuts.activities.variables.editor

import android.app.Application
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.activities.variables.VariableTypeMappings
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.enums.VariableType
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable
import ch.rmy.android.http_shortcuts.variables.Variables

class VariableEditorViewModel(application: Application) : BaseViewModel<VariableEditorViewState>(application) {

    private val variableRepository = VariableRepository()

    private var variableId: String? = null
    private lateinit var variableType: VariableType

    private lateinit var variable: Variable

    private var variableKeyInputErrorRes: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updateViewState {
                    copy(variableKeyInputError = value?.let { StringResLocalizable(it) })
                }
                if (value != null) {
                    emitEvent(VariableEditorEvent.FocusVariableKeyInput)
                }
            }
        }

    fun initialize(variableId: String?, variableType: VariableType) {
        this.variableId = variableId
        this.variableType = variableType
        if (variableId != null) {
            variableRepository
                .getVariableById(variableId)
                .subscribe(
                    { variable ->
                        this.variable = variable
                        initialize()
                    },
                    ::handleInitializationError,
                )
                .attachTo(destroyer)
        } else {
            this.variable = Variable()
            initialize()
        }
    }

    private fun handleInitializationError(error: Throwable) {
        // TODO: Handle error better
        logException(error)
        finish()
    }

    override fun initViewState() = VariableEditorViewState(
        title = StringResLocalizable(if (variableId == null) R.string.create_variable else R.string.edit_variable),
        subtitle = StringResLocalizable(VariableTypeMappings.getTypeName(variableType)),
        titleInputVisible = variableType.hasDialogTitle,
        urlEncodeChecked = variable.urlEncode,
        jsonEncodeChecked = variable.jsonEncode,
        allowShareChecked = variable.isShareText,
    )

    fun onSaveButtonClicked() {
        TODO("Not yet implemented")
        // trySave()
    }

    private fun trySave() {
        // TODO
        /*
        compileVariable()
        if (validate()) {
            viewModel.save()
                .subscribe {
                    finish()
                }
                .attachTo(destroyer)
        }
         */
    }

    private fun validate(): Boolean {
        if (variable.key.isEmpty()) {
            variableKeyInputErrorRes = R.string.validation_key_non_empty
            return false
        }
        if (isKeyAlreadyInUse()) {
            variableKeyInputErrorRes = R.string.validation_key_already_exists
            return false
        }
        return true // TODO: fragment == null || fragment!!.validate()
    }

    private fun isKeyAlreadyInUse(): Boolean {
        // TODO
        /*
        val otherVariable = variableRepository.getVariableByKey(variable.key)
            .blockingGet()
            .firstOrNull() // FIXME
        return otherVariable != null && otherVariable.id != variable.id
         */
        return false
    }

    fun onBackPressed() {
        if (hasChanges()) {
            showDiscardDialog()
        } else {
            finish()
        }
    }

    private fun hasChanges() = false // TODO
    // fun hasChanges(): Boolean = !variable.isSameAs(getDetachedVariable(variableId))

    private fun showDiscardDialog() {
        emitEvent(ViewModelEvent.ShowDialog { context ->
            DialogBuilder(context)
                .message(R.string.confirm_discard_changes_message)
                .positive(R.string.dialog_discard) { onDiscardDialogConfirmed() }
                .negative(R.string.dialog_cancel)
                .showIfPossible()
        })
    }

    private fun onDiscardDialogConfirmed() {
        finish()
    }

    fun onVariableKeyChanged(key: String) {
        updateVariableKey(key)
    }

    private fun updateVariableKey(key: String) {
        variableKeyInputErrorRes = if (key.isEmpty() || Variables.isValidVariableKey(key)) {
            null
        } else {
            R.string.warning_invalid_variable_key
        }
    }

    fun onVariableTitleChanged(title: String) {
        updateViewState {
            copy(variableTitle = title)
        }
    }

    fun onUrlEncodeChanged(enabled: Boolean) {
        updateViewState {
            copy(urlEncodeChecked = enabled)
        }
    }

    fun onJsonEncodeChanged(enabled: Boolean) {
        updateViewState {
            copy(jsonEncodeChecked = enabled)
        }
    }

    fun onAllowShareChanged(enabled: Boolean) {
        updateViewState {
            copy(allowShareChecked = enabled)
        }
    }

    /*
    fun save(): Completable =
        variableRepository.saveVariable(variable)
            .observeOn(AndroidSchedulers.mainThread())
     */
}
