package ch.rmy.android.http_shortcuts.activities.variables.editor.types.select

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.BaseVariableTypeViewModel
import ch.rmy.android.http_shortcuts.data.domains.variables.TemporaryVariableRepository

class SelectTypeViewModel(application: Application) : BaseVariableTypeViewModel<Unit, SelectTypeViewState>(application) {

    override fun initViewState() = SelectTypeViewState(
        rememberValue = false,
    )
}