package ch.rmy.android.http_shortcuts.activities.variables.editor.types.color

import android.view.LayoutInflater
import android.view.ViewGroup
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeChecked
import ch.rmy.android.framework.ui.BaseFragment
import ch.rmy.android.http_shortcuts.activities.main.ShortcutListViewModel
import ch.rmy.android.http_shortcuts.databinding.VariableEditorColorBinding

class ColorTypeFragment : BaseFragment<VariableEditorColorBinding>() {

    private val viewModel: ColorTypeViewModel by bindViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        VariableEditorColorBinding.inflate(inflater, container, false)

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
            binding.inputRememberValue.isChecked = viewState.rememberValue
        }
        viewModel.events.observe(this, ::handleEvent)
    }
}
