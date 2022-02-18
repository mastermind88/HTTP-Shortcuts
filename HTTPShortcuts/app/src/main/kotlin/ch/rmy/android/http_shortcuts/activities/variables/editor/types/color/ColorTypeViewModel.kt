package ch.rmy.android.http_shortcuts.activities.variables.editor.types.color

import android.app.Application
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.viewmodel.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.variables.editor.VariableEditorToVariableTypeEvent
import ch.rmy.android.http_shortcuts.activities.variables.editor.VariableTypeToVariableEditorEvent
import ch.rmy.android.http_shortcuts.activities.variables.editor.types.BaseVariableTypeViewModel
import ch.rmy.android.http_shortcuts.data.domains.variables.TemporaryVariableRepository

class ColorTypeViewModel(application: Application) : BaseVariableTypeViewModel<Unit, ColorTypeViewState>(application) {

    override fun initViewState() = ColorTypeViewState(
        rememberValue = variable.rememberValue,
    )

    fun onRememberValueChanged(enabled: Boolean) {
        performOperation(
            temporaryVariableRepository.setRememberValue(enabled)
        )
    }
}