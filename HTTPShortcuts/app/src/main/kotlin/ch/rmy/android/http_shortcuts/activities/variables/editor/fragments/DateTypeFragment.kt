package ch.rmy.android.http_shortcuts.activities.variables.editor.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.databinding.VariableEditorDateBinding
import ch.rmy.android.http_shortcuts.extensions.showMessageDialog
import ch.rmy.android.http_shortcuts.variables.types.DateType
import java.text.SimpleDateFormat
import java.util.Locale

class DateTypeFragment : BaseVariableTypeFragment<VariableEditorDateBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorDateBinding.inflate(inflater, container, false)

    /*
    override fun updateViews(variable: Variable) {
        this.variable = variable
        binding.inputRememberValue.isChecked = variable.rememberValue
        binding.inputVariableDateFormat.setText(variable.dataForType[DateType.KEY_FORMAT] ?: DateType.DEFAULT_FORMAT)
    }

    override fun validate(): Boolean {
        try {
            SimpleDateFormat(variable.dataForType[DateType.KEY_FORMAT], Locale.US)
        } catch (e: Exception) {
            showMessageDialog(R.string.error_invalid_date_format)
            return false
        }
        return true
    }

    override fun compileIntoVariable(variable: Variable) {
        variable.rememberValue = binding.inputRememberValue.isChecked
        variable.dataForType = mapOf(DateType.KEY_FORMAT to binding.inputVariableDateFormat.text.toString())
    }
     */
}
