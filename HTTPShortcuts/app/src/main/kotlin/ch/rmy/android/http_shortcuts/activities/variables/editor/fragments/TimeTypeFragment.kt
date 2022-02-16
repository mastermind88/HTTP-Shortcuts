package ch.rmy.android.http_shortcuts.activities.variables.editor.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.databinding.VariableEditorTimeBinding
import ch.rmy.android.http_shortcuts.extensions.showMessageDialog
import ch.rmy.android.http_shortcuts.variables.types.TimeType
import java.text.SimpleDateFormat
import java.util.Locale

class TimeTypeFragment : BaseVariableTypeFragment<VariableEditorTimeBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorTimeBinding.inflate(inflater, container, false)

    /*
    override fun updateViews(variable: Variable) {
        this.variable = variable
        binding.inputRememberValue.isChecked = variable.rememberValue
        binding.inputVariableTimeFormat.setText(variable.dataForType[TimeType.KEY_FORMAT] ?: TimeType.DEFAULT_FORMAT)
    }

    override fun validate() =
        try {
            SimpleDateFormat(variable.dataForType[TimeType.KEY_FORMAT], Locale.US)
            true
        } catch (e: Exception) {
            showMessageDialog(R.string.error_invalid_time_format)
            false
        }

    override fun compileIntoVariable(variable: Variable) {
        variable.rememberValue = binding.inputRememberValue.isChecked
        variable.dataForType = mapOf(TimeType.KEY_FORMAT to binding.inputVariableTimeFormat.text.toString())
    }
     */
}
