package ch.rmy.android.http_shortcuts.activities.variables.editor.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.databinding.VariableEditorColorBinding

class ColorTypeFragment : BaseVariableTypeFragment<VariableEditorColorBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorColorBinding.inflate(inflater, container, false)

    /*
    override fun updateViews(variable: Variable) {
        binding.inputRememberValue.isChecked = variable.rememberValue
    }

    override fun compileIntoVariable(variable: Variable) {
        variable.rememberValue = binding.inputRememberValue.isChecked
    }
     */
}
