package ch.rmy.android.http_shortcuts.activities.editor.authentication

import android.os.Bundle
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeTextChanges
import ch.rmy.android.framework.extensions.visible
import ch.rmy.android.framework.ui.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.databinding.ActivityAuthenticationBinding
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.VariableViewUtils

class AuthenticationActivity : BaseActivity() {

    private val viewModel: AuthenticationViewModel by bindViewModel()
    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initialize()
        initViews()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initViews() {
        binding = applyBinding(ActivityAuthenticationBinding.inflate(layoutInflater))
        setTitle(R.string.section_authentication)
        binding.inputAuthenticationMethod.setItemsFromPairs(
            AUTHENTICATION_METHODS.map {
                it.first to getString(it.second)
            }
        )
    }

    private fun initUserInputBindings() {
        VariableViewUtils.bindVariableViews(binding.inputUsername, binding.variableButtonUsername, variablePlaceholderProvider)
            .attachTo(destroyer)
        VariableViewUtils.bindVariableViews(binding.inputPassword, binding.variableButtonPassword, variablePlaceholderProvider)
            .attachTo(destroyer)
        VariableViewUtils.bindVariableViews(binding.inputToken, binding.variableButtonToken, variablePlaceholderProvider)
            .attachTo(destroyer)

        binding.inputAuthenticationMethod.selectionChanges
            .subscribe { method ->
                viewModel.onAuthenticationMethodChanged(method)
            }
            .attachTo(destroyer)

        binding.inputUsername.observeTextChanges()
            .subscribe {
                viewModel.onUsernameChanged(binding.inputUsername.rawString)
            }
            .attachTo(destroyer)

        binding.inputPassword.observeTextChanges()
            .subscribe {
                viewModel.onPasswordChanged(binding.inputPassword.rawString)
            }
            .attachTo(destroyer)

        binding.inputToken.observeTextChanges()
            .subscribe {
                viewModel.onTokenChanged(binding.inputToken.rawString)
            }
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.containerUsername.visible = viewState.isUsernameAndPasswordVisible
            binding.containerPassword.visible = viewState.isUsernameAndPasswordVisible
            binding.containerToken.visible = viewState.isTokenVisible

            binding.inputAuthenticationMethod.selectedItem = viewState.authenticationMethod
            binding.inputUsername.rawString = viewState.username
            binding.inputPassword.rawString = viewState.password
            binding.inputToken.rawString = viewState.token

            viewState.variables?.let(variablePlaceholderProvider::applyVariables)
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder : BaseIntentBuilder(AuthenticationActivity::class.java)

    companion object {

        private val AUTHENTICATION_METHODS = listOf(
            Shortcut.AUTHENTICATION_NONE to R.string.authentication_none,
            Shortcut.AUTHENTICATION_BASIC to R.string.authentication_basic,
            Shortcut.AUTHENTICATION_DIGEST to R.string.authentication_digest,
            Shortcut.AUTHENTICATION_BEARER to R.string.authentication_bearer,
        )
    }
}
