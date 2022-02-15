package ch.rmy.android.http_shortcuts.activities.editor.authentication

import android.app.Application
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.logException

class AuthenticationViewModel(application: Application) : BaseViewModel<AuthenticationViewState>(application) {

    private val temporaryShortcutRepository = TemporaryShortcutRepository()
    private val variableRepository = VariableRepository()

    override fun initViewState() = AuthenticationViewState()

    override fun onInitialized() {
        temporaryShortcutRepository.getTemporaryShortcut()
            .subscribe(
                ::initViewStateFromShortcut,
                ::onInitializationError,
            )
            .attachTo(destroyer)

        variableRepository.getObservableVariables()
            .subscribe { variables ->
                updateViewState {
                    copy(variables = variables)
                }
            }
            .attachTo(destroyer)
    }

    private fun initViewStateFromShortcut(shortcut: Shortcut) {
        updateViewState {
            val authenticationMethod = shortcut.authentication ?: Shortcut.AUTHENTICATION_NONE
            copy(
                authenticationMethod = authenticationMethod,
                isUsernameAndPasswordVisible = isUsernameAndPasswordVisible(authenticationMethod),
                isTokenVisible = isTokenVisible(authenticationMethod),
                username = shortcut.username,
                password = shortcut.password,
                token = shortcut.authToken,
            )
        }
    }

    private fun onInitializationError(error: Throwable) {
        // TODO: Handle error better
        logException(error)
        finish()
    }

    fun onAuthenticationMethodChanged(authenticationMethod: String) {
        updateViewState {
            copy(
                authenticationMethod = authenticationMethod,
                isUsernameAndPasswordVisible = isUsernameAndPasswordVisible(authenticationMethod),
                isTokenVisible = isTokenVisible(authenticationMethod),
            )
        }
        performOperation(
            temporaryShortcutRepository.setAuthenticationMethod(authenticationMethod)
        )
    }

    fun onUsernameChanged(username: String) {
        updateViewState {
            copy(username = username)
        }

        // TODO: Debounce
        performOperation(
            temporaryShortcutRepository.setUsername(username)
        )
    }

    fun onPasswordChanged(password: String) {
        updateViewState {
            copy(username = username)
        }

        // TODO: Debounce
        performOperation(
            temporaryShortcutRepository.setPassword(password)
        )
    }

    fun onTokenChanged(token: String) {
        updateViewState {
            copy(username = username)
        }

        // TODO: Debounce
        performOperation(
            temporaryShortcutRepository.setToken(token)
        )
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish()
        }
    }

    companion object {
        private fun isUsernameAndPasswordVisible(authenticationMethod: String) =
            when (authenticationMethod) {
                Shortcut.AUTHENTICATION_BASIC,
                Shortcut.AUTHENTICATION_DIGEST,
                -> true
                else -> false
            }

        private fun isTokenVisible(authenticationMethod: String) =
            authenticationMethod == Shortcut.AUTHENTICATION_BEARER
    }
}
