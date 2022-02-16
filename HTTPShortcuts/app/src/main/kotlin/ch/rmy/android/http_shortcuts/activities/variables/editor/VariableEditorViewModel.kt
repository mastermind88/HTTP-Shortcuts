package ch.rmy.android.http_shortcuts.activities.variables.editor

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.utils.localization.StringResLocalizable
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.variables.VariableTypeMappings
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.enums.VariableType
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.variables.Variables

class VariableEditorViewModel(application: Application) : BaseViewModel<VariableEditorViewModel.InitData, VariableEditorViewState>(application) {

    private val variableRepository = VariableRepository()

    private val variableId: String?
        get() = initData.variableId
    private val variableType: VariableType
        get() = initData.variableType

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

    data class InitData(
        val variableId: String?,
        val variableType: VariableType,
    )

    override fun onInitializationStarted(data: InitData) {
        if (data.variableId != null) {
            variableRepository
                .getVariableById(data.variableId)
                .subscribe(
                    { variable ->
                        this.variable = variable
                        finalizeInitialization()
                    },
                    ::handleInitializationError,
                )
                .attachTo(destroyer)
        } else {
            this.variable = Variable()
            finalizeInitialization()
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
        variableKey = variable.key,
        variableTitle = variable.title,
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
        emitEvent(
            ViewModelEvent.ShowDialog { context ->
                DialogBuilder(context)
                    .message(R.string.confirm_discard_changes_message)
                    .positive(R.string.dialog_discard) { onDiscardDialogConfirmed() }
                    .negative(R.string.dialog_cancel)
                    .showIfPossible()
            }
        )
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
