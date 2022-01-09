package ch.rmy.android.http_shortcuts.activities.editor.scripting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.databinding.ActivityScriptingBinding
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.color
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.insertAroundCursor
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.observeTextChanges
import ch.rmy.android.http_shortcuts.extensions.setHint
import ch.rmy.android.http_shortcuts.extensions.setTextSafely
import ch.rmy.android.http_shortcuts.extensions.visible
import ch.rmy.android.http_shortcuts.icons.IconPicker
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholderProvider
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutSpanManager
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.Variables

class ScriptingActivity : BaseActivity() {

    private val currentShortcutId: String? by lazy {
        intent.getStringExtra(EXTRA_SHORTCUT_ID)
    }

    private val viewModel: ScriptingViewModel by bindViewModel()

    private val shortcutPlaceholderProvider = ShortcutPlaceholderProvider()
    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    private var iconPickerShortcutPlaceholder: String? = null

    private val iconPicker: IconPicker by lazy {
        IconPicker(this) { icon ->
            codeSnippetPicker.insertChangeIconSnippet(
                iconPickerShortcutPlaceholder ?: return@IconPicker,
                getCodeInsertion(lastActiveCodeInput ?: return@IconPicker),
                icon,
            )
        }
    }
    private val codeSnippetPicker by lazy {
        CodeSnippetPicker(
            context,
            currentShortcutId,
            variablePlaceholderProvider,
            shortcutPlaceholderProvider,
        ) { shortcutPlaceholder ->
            iconPickerShortcutPlaceholder = shortcutPlaceholder
            iconPicker.openIconSelectionDialog()
        }
    }
    private val variablePlaceholderColor by lazy {
        color(context, R.color.variable)
    }
    private val shortcutPlaceholderColor by lazy {
        color(context, R.color.shortcut)
    }

    private lateinit var binding: ActivityScriptingBinding

    private var lastActiveCodeInput: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = applyBinding(ActivityScriptingBinding.inflate(layoutInflater))
        setTitle(R.string.label_scripting)

        initUserInputBindings()
        initViewModelBindings()
        viewModel.initialize()
    }

    private fun initUserInputBindings() {
        binding.buttonAddCodeSnippetPre.setOnClickListener {
            viewModel.onAddCodeSnippetPrepareButtonClicked()
        }
        binding.buttonAddCodeSnippetSuccess.setOnClickListener {
            viewModel.onAddCodeSnippetSuccessButtonClicked()
        }
        binding.buttonAddCodeSnippetFailure.setOnClickListener {
            lastActiveCodeInput = binding.inputCodeFailure
            viewModel.onAddCodeSnippetFailureButtonClicked()
        }

        binding.inputCodePrepare
            .observeTextChanges()
            .subscribe {
                viewModel.onCodePrepareChanged(binding.inputCodePrepare.text.toString())
            }
            .attachTo(destroyer)
        binding.inputCodeSuccess
            .observeTextChanges()
            .subscribe {
                viewModel.onCodeSuccessChanged(binding.inputCodeSuccess.text.toString())
            }
            .attachTo(destroyer)
        binding.inputCodeFailure
            .observeTextChanges()
            .subscribe {
                viewModel.onCodeFailureChanged(binding.inputCodeFailure.text.toString())
            }
            .attachTo(destroyer)
    }

    private fun processTextForView(input: String): CharSequence {
        val text = SpannableStringBuilder(input)
        Variables.applyVariableFormattingToJS(
            text,
            variablePlaceholderProvider,
            variablePlaceholderColor,
        )
        ShortcutSpanManager.applyShortcutFormattingToJS(
            text,
            shortcutPlaceholderProvider,
            shortcutPlaceholderColor,
        )
        return text
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.scripting_activity_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_show_help -> consume { viewModel.onHelpButtonClicked() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.inputCodePrepare.minLines = viewState.codePrepareMinLines
            binding.inputCodePrepare.setHint(viewState.codePrepareHint)
            binding.labelCodePrepare.visible = viewState.codePrepareVisible
            binding.containerPostRequestScripting.visible = viewState.postRequestScriptingVisible
            binding.inputCodeSuccess.setTextSafely(processTextForView(viewState.codeOnSuccess))
            binding.inputCodeFailure.setTextSafely(processTextForView(viewState.codeOnFailure))
            binding.inputCodePrepare.setTextSafely(processTextForView(viewState.codeOnPrepare))

            shortcutPlaceholderProvider.shortcuts = viewState.shortcuts
            variablePlaceholderProvider.variables = viewState.variables
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is ScriptingEvent.ShowCodeSnippetPicker -> {
                val view = when (event.target) {
                    ScriptingEvent.ShowCodeSnippetPicker.Target.PREPARE -> {
                        binding.inputCodePrepare
                    }
                    ScriptingEvent.ShowCodeSnippetPicker.Target.SUCCESS -> {
                        binding.inputCodeSuccess
                    }
                    ScriptingEvent.ShowCodeSnippetPicker.Target.FAILURE -> {
                        binding.inputCodeFailure
                    }
                }
                showCodeSnippetPicker(
                    view,
                    includeFileOptions = event.includeFileOptions,
                    includeResponseOptions = event.includeResponseOptions,
                    includeNetworkErrorOption = event.includeNetworkErrorOption,
                )
            }
            else -> super.handleEvent(event)
        }
    }

    private fun showCodeSnippetPicker(
        editText: EditText,
        includeFileOptions: Boolean,
        includeResponseOptions: Boolean,
        includeNetworkErrorOption: Boolean,
    ) {
        lastActiveCodeInput = editText
        codeSnippetPicker.showCodeSnippetPicker(
            getCodeInsertion(editText),
            includeResponseOptions = includeResponseOptions,
            includeFileOptions = includeFileOptions,
            includeNetworkErrorOption = includeNetworkErrorOption,
        )
    }

    private fun getCodeInsertion(codeInput: EditText): InsertText =
        { before, after ->
            codeInput.insertAroundCursor(before, after)
            Variables.applyVariableFormattingToJS(codeInput.text, variablePlaceholderProvider, variablePlaceholderColor)
            ShortcutSpanManager.applyShortcutFormattingToJS(codeInput.text, shortcutPlaceholderProvider, shortcutPlaceholderColor)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            codeSnippetPicker.handleRequestResult(
                getCodeInsertion(lastActiveCodeInput ?: return),
                requestCode,
                data,
            )
        }
        iconPicker.handleResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder : BaseIntentBuilder(ScriptingActivity::class.java) {

        fun shortcutId(shortcutId: String?) = also {
            intent.putExtra(EXTRA_SHORTCUT_ID, shortcutId)
        }
    }

    companion object {
        private const val EXTRA_SHORTCUT_ID = "shortcutId"
    }
}
