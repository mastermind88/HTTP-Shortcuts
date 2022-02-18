package ch.rmy.android.framework.viewmodel

import android.app.Application
import android.content.Intent
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.lifecycle.AndroidViewModel
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.ui.BaseIntentBuilder
import ch.rmy.android.framework.utils.Destroyer
import ch.rmy.android.framework.utils.ProgressMonitor
import ch.rmy.android.framework.utils.localization.Localizable
import ch.rmy.android.http_shortcuts.R
import com.victorrendina.rxqueue2.QueueSubject
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject

abstract class BaseViewModel<InitData : Any, ViewState : Any>(application: Application) : AndroidViewModel(application) {

    protected lateinit var initData: InitData
        private set

    protected val progressMonitor = ProgressMonitor()

    private val eventSubject = QueueSubject.create<ViewModelEvent>()

    val events: Observable<ViewModelEvent>
        get() = eventSubject.observeOn(AndroidSchedulers.mainThread())

    private val viewStateSubject = BehaviorSubject.create<ViewState>()

    val viewState: Observable<ViewState>
        get() = viewStateSubject.observeOn(AndroidSchedulers.mainThread())

    protected lateinit var currentViewState: ViewState

    protected fun emitEvent(event: ViewModelEvent) {
        eventSubject.onNext(event)
    }

    private var suppressViewStatePublishing = false

    @UiThread
    protected fun updateViewState(mutation: ViewState.() -> ViewState) {
        currentViewState = mutation(currentViewState)
        if (!suppressViewStatePublishing) {
            viewStateSubject.onNext(currentViewState)
        }
    }

    @UiThread
    protected fun atomicallyUpdateViewState(action: () -> Unit) {
        if (suppressViewStatePublishing) {
            action()
            return
        }
        suppressViewStatePublishing = true
        action()
        suppressViewStatePublishing = false
        viewStateSubject.onNext(currentViewState)
    }

    protected val destroyer = Destroyer()

    private var isInitializationStarted = false

    protected var isInitialized: Boolean = false
        private set

    fun initialize(data: InitData) {
        if (isInitializationStarted) {
            if (data != initData) {
                logException(IllegalStateException("cannot re-initialize view model with different data"))
            }
            return
        }
        this.initData = data
        isInitializationStarted = true
        onInitializationStarted(data)
    }

    /**
     * Must eventually call finalizeInitialization or terminate the view
     */
    protected open fun onInitializationStarted(data: InitData) {
        finalizeInitialization()
    }

    protected fun finalizeInitialization(silent: Boolean = false) {
        if (isInitialized) {
            throw IllegalStateException("view model already initialized")
        }
        currentViewState = initViewState()
        if (!silent) {
            viewStateSubject.onNext(currentViewState)
        }
        isInitialized = true
        onInitialized()
    }

    protected open fun onInitialized() {
    }

    protected abstract fun initViewState(): ViewState

    protected fun performOperation(operation: Completable, onComplete: (() -> Unit) = {}) {
        operation
            .compose(progressMonitor.transformer())
            .subscribe(
                onComplete,
                ::handleUnexpectedError,
            )
            .attachTo(destroyer)
    }

    protected fun handleUnexpectedError(error: Throwable) {
        logException(error)
        showSnackbar(R.string.error_generic, long = true)
    }

    protected fun waitForOperationsToFinish(action: () -> Unit) {
        progressMonitor.anyInProgress
            .takeWhile { it }
            .ignoreElements()
            .subscribe(action)
            .attachTo(destroyer)
    }

    protected fun showSnackbar(@StringRes stringRes: Int, long: Boolean = false) {
        emitEvent(ViewModelEvent.ShowSnackbar(stringRes, long = long))
    }

    protected fun showSnackbar(message: Localizable, long: Boolean = false) {
        emitEvent(ViewModelEvent.ShowSnackbar(message, long = long))
    }

    protected fun showToast(@StringRes stringRes: Int, long: Boolean = false) {
        emitEvent(ViewModelEvent.ShowToast(stringRes, long = long))
    }

    protected fun finish(result: Int? = null, intent: Intent? = null, skipAnimation: Boolean = false) {
        emitEvent(ViewModelEvent.Finish(result, intent, skipAnimation))
    }

    protected fun openURL(url: String) {
        emitEvent(ViewModelEvent.OpenURL(url))
    }

    protected fun openActivity(intentBuilder: BaseIntentBuilder, requestCode: Int? = null) {
        emitEvent(ViewModelEvent.OpenActivity(intentBuilder, requestCode))
    }

    protected fun sendBroadcast(intent: Intent) {
        emitEvent(ViewModelEvent.SendBroadcast(intent))
    }

    final override fun onCleared() {
        destroyer.destroy()
    }
}
