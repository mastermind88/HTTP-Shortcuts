package ch.rmy.android.http_shortcuts.activities.variables.editor.types.text

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.BaseVariableTypeViewModel
import ch.rmy.android.http_shortcuts.data.domains.variables.TemporaryVariableRepository

class TextTypeViewModel(application: Application) : BaseVariableTypeViewModel<Unit, TextTypeViewState>(application) {

    override fun initViewState() = TextTypeViewState(
        rememberValue = false,
    )
}