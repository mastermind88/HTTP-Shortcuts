package ch.rmy.android.http_shortcuts.activities.variables.editor

import android.app.Application
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Variable
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers

class VariableEditorViewModel(application: Application) : BaseViewModel(application) {

    private val variableRepository = VariableRepository()

    var variableType: String = Variable.TYPE_CONSTANT

    var variableId: String? = null
        set(value) {
            field = value
            variable = getDetachedVariable(value)
        }

    private lateinit var variable: Variable

    private fun getDetachedVariable(variableId: String?): Variable =
        if (variableId != null) {
            variableRepository.getVariableById(variableId)
                .blockingGet()
                .first() // FIXME
        } else {
            Variable(type = variableType)
        }

    fun getVariable(): Variable = variable

    fun hasChanges(): Boolean = !variable.isSameAs(getDetachedVariable(variableId))

    fun isKeyAlreadyInUsed(): Boolean {
        val otherVariable = variableRepository.getVariableByKey(variable.key)
            .blockingGet()
            .firstOrNull() // FIXME
        return otherVariable != null && otherVariable.id != variable.id
    }

    fun save(): Completable =
        variableRepository.saveVariable(variable)
            .observeOn(AndroidSchedulers.mainThread())
}
