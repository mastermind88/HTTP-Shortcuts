package ch.rmy.android.http_shortcuts.activities.variables.editor

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.consume
import ch.rmy.android.framework.extensions.focus
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeChecked
import ch.rmy.android.framework.extensions.observeTextChanges
import ch.rmy.android.framework.extensions.setTextSafely
import ch.rmy.android.framework.extensions.visible
import ch.rmy.android.framework.ui.BaseFragment
import ch.rmy.android.framework.ui.BaseIntentBuilder
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.color.ColorTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.constant.ConstantTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.date.DateTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.select.SelectTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.slider.SliderTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.text.TextTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.time.TimeTypeFragment
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.toggle.ToggleTypeFragment
import ch.rmy.android.http_shortcuts.data.enums.VariableType
import ch.rmy.android.http_shortcuts.databinding.ActivityVariableEditorBinding

class VariableEditorActivity : BaseActivity() {

    private val variableId: String? by lazy {
        intent.getStringExtra(EXTRA_VARIABLE_ID)
    }
    private val variableType: VariableType by lazy {
        VariableType.parse(intent.getStringExtra(EXTRA_VARIABLE_TYPE))
    }

    private lateinit var defaultColor: ColorStateList

    private val viewModel: VariableEditorViewModel by bindViewModel()

    private lateinit var binding: ActivityVariableEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initialize(
            VariableEditorViewModel.InitData(
                variableId = variableId,
                variableType = variableType,
            ),
        )
        initViews()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initViews() {
        binding = applyBinding(ActivityVariableEditorBinding.inflate(layoutInflater))

        defaultColor = binding.inputVariableKey.textColors

        initVariableTypeFragment()
    }

    private fun initVariableTypeFragment() {
        val tag = "variable_edit_fragment_${variableType.type}"
        val fragment = supportFragmentManager.findFragmentByTag(tag) as? BaseFragment<*>
            ?: createEditorFragment()

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.variable_type_fragment_container, fragment, tag)
            .commitAllowingStateLoss()
    }

    private fun createEditorFragment(): BaseFragment<*> =
        when (variableType) {
            VariableType.CONSTANT -> ConstantTypeFragment.create(variableId)
            VariableType.TEXT -> TextTypeFragment()
            VariableType.NUMBER -> TextTypeFragment()
            VariableType.PASSWORD -> TextTypeFragment()
            VariableType.SELECT -> SelectTypeFragment()
            VariableType.TOGGLE -> ToggleTypeFragment()
            VariableType.COLOR -> ColorTypeFragment()
            VariableType.DATE -> DateTypeFragment()
            VariableType.TIME -> TimeTypeFragment()
            VariableType.SLIDER -> SliderTypeFragment()
        }

    private fun initUserInputBindings() {
        binding.inputVariableKey
            .observeTextChanges()
            .subscribe { text ->
                viewModel.onVariableKeyChanged(text?.toString() ?: "")
            }
            .attachTo(destroyer)

        binding.inputVariableTitle
            .observeTextChanges()
            .subscribe { text ->
                viewModel.onVariableTitleChanged(text?.toString() ?: "")
            }
            .attachTo(destroyer)

        binding.inputUrlEncode
            .observeChecked()
            .subscribe(viewModel::onUrlEncodeChanged)
            .attachTo(destroyer)

        binding.inputJsonEncode
            .observeChecked()
            .subscribe(viewModel::onJsonEncodeChanged)
            .attachTo(destroyer)

        binding.inputAllowShare
            .observeChecked()
            .subscribe(viewModel::onAllowShareChanged)
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            setTitle(viewState.title)
            setSubtitle(viewState.subtitle)
            binding.dialogTitleContainer.visible = viewState.titleInputVisible
            binding.inputVariableKey.error = viewState.variableKeyInputError?.localize(context)
            binding.inputVariableKey.setTextSafely(viewState.variableKey)
            binding.inputVariableTitle.setTextSafely(viewState.variableTitle)
            if (viewState.variableKeyErrorHighlighting) {
                binding.inputVariableKey.setTextColor(Color.RED)
            } else {
                binding.inputVariableKey.setTextColor(defaultColor)
            }
            binding.inputUrlEncode.isChecked = viewState.urlEncodeChecked
            binding.inputJsonEncode.isChecked = viewState.jsonEncodeChecked
            binding.inputAllowShare.isChecked = viewState.allowShareChecked
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is VariableEditorEvent.FocusVariableKeyInput -> binding.inputVariableKey.focus()
            else -> super.handleEvent(event)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.variable_editor_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override val navigateUpIcon = R.drawable.ic_clear

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_save_variable -> consume { viewModel.onSaveButtonClicked() }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder(type: VariableType) : BaseIntentBuilder(VariableEditorActivity::class.java) {

        init {
            intent.putExtra(EXTRA_VARIABLE_TYPE, type.type)
        }

        fun variableId(variableId: String) = also {
            intent.putExtra(EXTRA_VARIABLE_ID, variableId)
        }
    }

    companion object {

        private const val EXTRA_VARIABLE_ID = "ch.rmy.android.http_shortcuts.activities.variables.editor.VariableEditorActivity.variable_id"
        private const val EXTRA_VARIABLE_TYPE = "ch.rmy.android.http_shortcuts.activities.variables.editor.VariableEditorActivity.variable_type"
    }
}
