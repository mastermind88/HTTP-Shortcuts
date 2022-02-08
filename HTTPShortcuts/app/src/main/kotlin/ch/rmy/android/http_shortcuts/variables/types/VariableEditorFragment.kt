package ch.rmy.android.http_shortcuts.variables.types

import androidx.viewbinding.ViewBinding
import ch.rmy.android.http_shortcuts.activities.BaseFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.VariableEditorActivity
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider

abstract class VariableEditorFragment<Binding : ViewBinding> : BaseFragment<Binding>() {

    private val variablesRepository = VariableRepository()

    protected val variablePlaceholderProvider by lazy {
        VariablePlaceholderProvider(/*variablesRepository.getObservableVariables()*/)
    }

    override fun onStart() {
        super.onStart()
        // TODO (activity as VariableEditorActivity).onFragmentStarted()
    }

    open fun updateViews(variable: Variable) {
    }

    open fun compileIntoVariable(variable: Variable) {
    }

    open fun validate() = true
}
