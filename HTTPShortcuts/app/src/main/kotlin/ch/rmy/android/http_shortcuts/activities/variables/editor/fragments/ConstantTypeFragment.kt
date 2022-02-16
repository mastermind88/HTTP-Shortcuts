package ch.rmy.android.http_shortcuts.activities.variables.editor.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.databinding.VariableEditorConstantBinding
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.VariableViewUtils.bindVariableViews

class ConstantTypeFragment : BaseVariableTypeFragment<VariableEditorConstantBinding>() {

    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorConstantBinding.inflate(inflater, container, false)

    override fun setupViews() {
        bindVariableViews(binding.inputVariableValue, binding.variableButton, variablePlaceholderProvider, allowEditing = false)
            .attachTo(destroyer)
    }

    /*
    override fun updateViews(variable: Variable) {
        binding.inputVariableValue.rawString = variable.value ?: ""
    }

    override fun compileIntoVariable(variable: Variable) {
        variable.value = binding.inputVariableValue.rawString
    }
     */
}
