package ch.rmy.android.http_shortcuts.activities.variables.editor

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.data.enums.VariableType
import ch.rmy.android.http_shortcuts.databinding.ActivityVariableEditorBinding
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.focus
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.observeChecked
import ch.rmy.android.http_shortcuts.extensions.observeTextChanges
import ch.rmy.android.http_shortcuts.extensions.setSubtitle
import ch.rmy.android.http_shortcuts.extensions.setTitle
import ch.rmy.android.http_shortcuts.extensions.visible
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.variables.types.VariableEditorFragment

class VariableEditorActivity : BaseActivity() {

    private lateinit var defaultColor: ColorStateList

    private val viewModel: VariableEditorViewModel by bindViewModel()

    private lateinit var binding: ActivityVariableEditorBinding

    private var fragment: VariableEditorFragment<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = applyBinding(ActivityVariableEditorBinding.inflate(layoutInflater))

        initViews()
        initUserInputBindings()
        initViewModelBindings()
        viewModel.initialize(
            variableId = intent.getStringExtra(EXTRA_VARIABLE_ID),
            variableType = VariableType.parse(intent.getStringExtra(EXTRA_VARIABLE_TYPE)),
        )
    }

    private fun initViews() {
        // TODO
        defaultColor = binding.inputVariableKey.textColors
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
            binding.inputVariableKey.setText(viewState.variableKey)
            binding.inputVariableTitle.setText(viewState.variableTitle)
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

    /*

    private fun updateTypeEditor() {
        compileVariable()
        val variableType = VariableTypeFactory.getType(variable.type)

        fragment = variableType.getEditorFragment(supportFragmentManager)

        fragment?.let { fragment ->
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.variable_type_fragment_container, fragment, variableType.tag)
                .commitAllowingStateLoss()
        }
    }

    fun onFragmentStarted() {
        fragment?.updateViews(variable)
    }
     */

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

    private fun compileVariable() {
        // TODO: fragment?.compileIntoVariable(variable)
    }

    override fun onStop() {
        super.onStop()
        compileVariable()
    }

    class IntentBuilder : BaseIntentBuilder(VariableEditorActivity::class.java) {

        fun variableType(type: VariableType) = also {
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
