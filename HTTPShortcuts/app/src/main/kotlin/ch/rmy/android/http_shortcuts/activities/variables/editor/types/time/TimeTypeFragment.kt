package ch.rmy.android.http_shortcuts.activities.variables.editor.types.time

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.ui.BaseFragment

import ch.rmy.android.http_shortcuts.databinding.VariableEditorTimeBinding

class TimeTypeFragment : BaseFragment<VariableEditorTimeBinding>() {

    private val viewModel: TimeTypeViewModel by bindViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorTimeBinding.inflate(inflater, container, false)

    override fun setupViews() {
        viewModel.initialize()
        initViews()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initViews() {

    }

    private fun initUserInputBindings() {

    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->

        }
        viewModel.events.observe(this, ::handleEvent)
    }

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
