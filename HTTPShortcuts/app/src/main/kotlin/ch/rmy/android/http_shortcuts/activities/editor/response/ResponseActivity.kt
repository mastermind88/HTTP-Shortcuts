package ch.rmy.android.http_shortcuts.activities.editor.response

import android.content.Context
import android.os.Bundle
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.data.models.ResponseHandling
import ch.rmy.android.http_shortcuts.databinding.ActivityResponseBinding
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.observeChecked
import ch.rmy.android.http_shortcuts.extensions.observeTextChanges
import ch.rmy.android.http_shortcuts.extensions.setHint
import ch.rmy.android.http_shortcuts.extensions.visible
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.VariableViewUtils

class ResponseActivity : BaseActivity() {

    private val viewModel: ResponseViewModel by bindViewModel()
    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    private lateinit var binding: ActivityResponseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = applyBinding(ActivityResponseBinding.inflate(layoutInflater))
        setTitle(R.string.label_response_handling)

        initViews()
        initUserInputBindings()
        initViewModelBindings()
        viewModel.initialize()
    }

    private fun initViews() {
        binding.inputResponseUiType.setItemsFromPairs(
            UI_TYPES.map {
                it.first to getString(it.second)
            }
        )
        binding.inputResponseSuccessOutput.setItemsFromPairs(
            SUCCESS_OUTPUT_TYPES.map {
                it.first to getString(it.second)
            }
        )
        binding.inputResponseFailureOutput.setItemsFromPairs(
            FAILURE_OUTPUT_TYPES.map {
                it.first to getString(it.second)
            }
        )

        binding.instructionsScriptingHint.text = getString(R.string.message_response_handling_scripting_hint, getString(R.string.label_scripting))
    }

    private fun initUserInputBindings() {
        binding.inputResponseUiType.selectionChanges
            .subscribe { responseUiType ->
                viewModel.onResponseUiTypeChanged(responseUiType)
            }
            .attachTo(destroyer)
        binding.inputResponseSuccessOutput
            .selectionChanges
            .subscribe { responseSuccessOutput ->
                viewModel.onResponseSuccessOutputChanged(responseSuccessOutput)
            }
            .attachTo(destroyer)
        binding.inputResponseFailureOutput
            .selectionChanges
            .subscribe { responseFailureOutput ->
                viewModel.onResponseFailureOutputChanged(responseFailureOutput)
            }
            .attachTo(destroyer)
        binding.inputSuccessMessage
            .observeTextChanges()
            .subscribe {
                viewModel.onSuccessMessageChanged(binding.inputSuccessMessage.rawString)
            }
            .attachTo(destroyer)
        binding.inputIncludeMetaInformation
            .observeChecked()
            .subscribe { isChecked ->
                viewModel.onIncludeMetaInformationChanged(isChecked)
            }
            .attachTo(destroyer)

        VariableViewUtils.bindVariableViews(binding.inputSuccessMessage, binding.variableButtonSuccessMessage, variablePlaceholderProvider)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.inputSuccessMessage.setHint(viewState.successMessageHint)
            binding.inputResponseUiType.selectedItem = viewState.responseUiType
            binding.inputResponseSuccessOutput.selectedItem = viewState.responseSuccessOutput
            binding.inputResponseFailureOutput.selectedItem = viewState.responseFailureOutput
            binding.inputIncludeMetaInformation.isChecked = viewState.includeMetaInformation
            binding.inputSuccessMessage.rawString = viewState.successMessage
            binding.inputResponseUiType.visible = viewState.responseUiTypeVisible
            binding.containerInputSuccessMessage.visible = viewState.successMessageVisible
            binding.inputIncludeMetaInformation.visible = viewState.includeMetaInformationVisible
            variablePlaceholderProvider.variables = viewState.variables
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder(context: Context) : BaseIntentBuilder(context, ResponseActivity::class.java)

    companion object {

        private val UI_TYPES = listOf(
            ResponseHandling.UI_TYPE_TOAST to R.string.option_response_handling_type_toast,
            ResponseHandling.UI_TYPE_DIALOG to R.string.option_response_handling_type_dialog,
            ResponseHandling.UI_TYPE_WINDOW to R.string.option_response_handling_type_window,
        )

        private val SUCCESS_OUTPUT_TYPES = listOf(
            ResponseHandling.SUCCESS_OUTPUT_RESPONSE to R.string.option_response_handling_success_output_response,
            ResponseHandling.SUCCESS_OUTPUT_MESSAGE to R.string.option_response_handling_success_output_message,
            ResponseHandling.SUCCESS_OUTPUT_NONE to R.string.option_response_handling_success_output_none,
        )

        private val FAILURE_OUTPUT_TYPES = listOf(
            ResponseHandling.FAILURE_OUTPUT_DETAILED to R.string.option_response_handling_failure_output_detailed,
            ResponseHandling.FAILURE_OUTPUT_SIMPLE to R.string.option_response_handling_failure_output_simple,
            ResponseHandling.FAILURE_OUTPUT_NONE to R.string.option_response_handling_failure_output_none,
        )
    }
}
