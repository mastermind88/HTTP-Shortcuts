package ch.rmy.android.http_shortcuts.activities.variables.editor.types.time

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.BaseVariableTypeViewModel
import ch.rmy.android.http_shortcuts.data.domains.variables.TemporaryVariableRepository

class TimeTypeViewModel(application: Application) : BaseVariableTypeViewModel<Unit, TimeTypeViewState>(application) {

    override fun initViewState() = TimeTypeViewState(
        rememberValue = false,
    )
}