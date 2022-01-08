package ch.rmy.android.http_shortcuts.activities.editor.basicsettings

import android.content.Context
import android.os.Bundle
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.databinding.ActivityBasicRequestSettingsBinding
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.observeTextChanges
import ch.rmy.android.http_shortcuts.extensions.visible
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.VariableViewUtils.bindVariableViews
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

class BasicRequestSettingsActivity : BaseActivity() {

    private val viewModel: BasicRequestSettingsViewModel by bindViewModel()

    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    private lateinit var binding: ActivityBasicRequestSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = applyBinding(ActivityBasicRequestSettingsBinding.inflate(layoutInflater))
        setTitle(R.string.section_basic_request)

        initViews()
        initUserInputBindings()
        initViewModelBindings()
        viewModel.initialize()
    }

    private fun initViews() {
        binding.inputMethod.setItemsFromPairs(
            METHODS.map {
                it to it
            }
        )
        bindVariableViews(binding.inputUrl, binding.variableButtonUrl, variablePlaceholderProvider)
            .attachTo(destroyer)
    }

    private fun initUserInputBindings() {
        binding.inputMethod.selectionChanges
            .subscribe { method ->
                viewModel.onMethodChanged(method)
            }
            .attachTo(destroyer)

        binding.inputUrl.observeTextChanges()
            .debounce(300, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                viewModel.onUrlChanged(binding.inputUrl.rawString)
            }
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.inputMethod.visible = viewState.methodVisible
            binding.inputMethod.selectedItem = viewState.method
            binding.inputUrl.rawString = viewState.url

            variablePlaceholderProvider.variables = viewState.variables
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder(context: Context) : BaseIntentBuilder(context, BasicRequestSettingsActivity::class.java)

    companion object {

        private val METHODS = listOf(
            Shortcut.METHOD_GET,
            Shortcut.METHOD_POST,
            Shortcut.METHOD_PUT,
            Shortcut.METHOD_DELETE,
            Shortcut.METHOD_PATCH,
            Shortcut.METHOD_HEAD,
            Shortcut.METHOD_OPTIONS,
            Shortcut.METHOD_TRACE,
        )
    }
}
