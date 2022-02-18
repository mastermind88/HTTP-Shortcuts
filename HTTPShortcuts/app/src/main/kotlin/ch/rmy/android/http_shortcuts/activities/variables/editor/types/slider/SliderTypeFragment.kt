package ch.rmy.android.http_shortcuts.activities.variables.editor.types.slider

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeChecked
import ch.rmy.android.framework.ui.BaseFragment
import ch.rmy.android.http_shortcuts.databinding.VariableEditorSliderBinding

class SliderTypeFragment : BaseFragment<VariableEditorSliderBinding>() {

    private val viewModel: SliderTypeViewModel by bindViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorSliderBinding.inflate(inflater, container, false)

    override fun setupViews() {
        viewModel.initialize()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initUserInputBindings() {
        binding.inputRememberValue
            .observeChecked()
            .subscribe(viewModel::onRememberValueChanged)
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.inputSliderMin.setText(viewState.minValueText)
            binding.inputSliderMax.setText(viewState.maxValueText)
            binding.inputSliderStep.setText(viewState.stepSizeText)
            binding.inputRememberValue.isChecked = viewState.rememberValue
        }
        viewModel.events.observe(this, ::handleEvent)
    }
}
