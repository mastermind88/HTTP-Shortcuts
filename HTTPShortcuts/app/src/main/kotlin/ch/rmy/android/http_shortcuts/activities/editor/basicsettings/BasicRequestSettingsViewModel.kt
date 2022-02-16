package ch.rmy.android.http_shortcuts.activities.editor.basicsettings

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.TemporaryShortcutRepository
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.type
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class BasicRequestSettingsViewModel(application: Application) : BaseViewModel<Unit, BasicRequestSettingsViewState>(application) {

    private val temporaryShortcutRepository = TemporaryShortcutRepository()
    private val variableRepository = VariableRepository()

    private val urlSubject = PublishSubject.create<String>()

    override fun initViewState() = BasicRequestSettingsViewState()

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

        urlSubject
            .throttleLatest(200, TimeUnit.MILLISECONDS, true)
            .concatMapCompletable { url ->
                temporaryShortcutRepository.setUrl(url)
                    .compose(progressMonitor.transformer())
            }
            .subscribe()
            .attachTo(destroyer)
    }

    private fun initViewStateFromShortcut(shortcut: Shortcut) {
        updateViewState {
            copy(
                methodVisible = shortcut.type == ShortcutExecutionType.APP,
                method = shortcut.method,
                url = shortcut.url,
            )
        }
    }

    private fun onInitializationError(error: Throwable) {
        // TODO: Handle error better
        logException(error)
        finish()
    }

    fun onBackPressed() {
        waitForOperationsToFinish {
            finish()
        }
    }

    fun onUrlChanged(url: String) {
        updateViewState {
            copy(url = url)
        }
        urlSubject.onNext(url)
    }

    fun onMethodChanged(method: String) {
        updateViewState {
            copy(method = method)
        }
        performOperation(
            temporaryShortcutRepository.setMethod(method)
        )
    }
}
