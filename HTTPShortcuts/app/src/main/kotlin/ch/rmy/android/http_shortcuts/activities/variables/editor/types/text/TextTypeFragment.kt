package ch.rmy.android.http_shortcuts.activities.variables.editor.types.text

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.ui.BaseFragment
import ch.rmy.android.http_shortcuts.databinding.VariableEditorTextBinding

class TextTypeFragment : BaseFragment<VariableEditorTextBinding>() {

    private val viewModel: TextTypeViewModel by bindViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorTextBinding.inflate(inflater, container, false)

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
        binding.inputRememberValue.isChecked = variable.rememberValue
        binding.inputMultiline.isChecked = variable.isMultiline
    }

    override fun compileIntoVariable(variable: Variable) {
        variable.rememberValue = binding.inputRememberValue.isChecked
        variable.isMultiline = binding.inputMultiline.isChecked
    }
     */
}
