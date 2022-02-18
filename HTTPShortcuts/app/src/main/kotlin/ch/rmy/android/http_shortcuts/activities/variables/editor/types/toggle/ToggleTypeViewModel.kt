package ch.rmy.android.http_shortcuts.activities.variables.editor.types.toggle

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.BaseVariableTypeViewModel
import ch.rmy.android.http_shortcuts.data.domains.variables.TemporaryVariableRepository

class ToggleTypeViewModel(application: Application) : BaseVariableTypeViewModel<Unit, ToggleTypeViewState>(application) {

    override fun initViewState() = ToggleTypeViewState(
        rememberValue = false,
    )
}