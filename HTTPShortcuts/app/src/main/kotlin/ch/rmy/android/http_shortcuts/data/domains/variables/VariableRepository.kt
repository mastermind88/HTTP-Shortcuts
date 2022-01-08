package ch.rmy.android.http_shortcuts.data.domains.variables

import ch.rmy.android.http_shortcuts.data.BaseRepository
import ch.rmy.android.http_shortcuts.data.domains.getBase
import ch.rmy.android.http_shortcuts.data.domains.getVariableById
import ch.rmy.android.http_shortcuts.data.domains.getVariableByKey
import ch.rmy.android.http_shortcuts.data.domains.getVariableByKeyOrId
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.extensions.detachFromRealm
import ch.rmy.android.http_shortcuts.utils.UUIDUtils.newUUID
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class VariableRepository : BaseRepository() {

    fun getVariableById(variableId: String): Single<List<Variable>> =
        query {
            getVariableById(variableId)
        }

    fun getVariableByKey(key: String): Single<List<Variable>> =
        query {
            getVariableByKey(key)
        }

    fun getVariableByKeyOrId(keyOrId: String): Single<Variable> =
        queryItem {
            getVariableByKeyOrId(keyOrId)
        }

    fun getObservableVariables(): Observable<List<Variable>> =
        observeItem {
            getBase()
        }
            .map { base ->
                base.variables
            }

    fun getVariables(): Single<List<Variable>> =
        queryItem {
            getBase()
        }
            .map { base ->
                base.variables
            }

    fun saveVariable(variable: Variable) =
        commitTransaction {
            val isNew = variable.isNew
            if (isNew) {
                variable.id = newUUID()
            }
            val base = getBase()
                .findFirst()
                ?: return@commitTransaction
            val newVariable = copyOrUpdate(variable)
            if (isNew) {
                base.variables.add(newVariable)
            }
        }

    fun setVariableValue(variableId: String, value: String): Completable =
        commitTransaction {
            getVariableById(variableId)
                .findFirst()
                ?.value = value
        }

    fun moveVariable(variableId: String, position: Int) =
        commitTransaction {
            val variable = getVariableById(variableId)
                .findFirst()
                ?: return@commitTransaction
            val variables = getBase()
                .findFirst()
                ?.variables ?: return@commitTransaction
            val oldPosition = variables.indexOf(variable)
            variables.move(oldPosition, position)
        }

    fun duplicateVariable(variableId: String, newKey: String) =
        commitTransaction {
            val oldVariable = getVariableById(variableId)
                .findFirst()
                ?: return@commitTransaction
            val newVariable = oldVariable.detachFromRealm()
            newVariable.id = newUUID()
            newVariable.key = newKey
            newVariable.options?.forEach {
                it.id = newUUID()
            }

            val base = getBase()
                .findFirst()
                ?: return@commitTransaction
            val oldPosition = base.variables.indexOf(oldVariable)
            val newPersistedVariable = copyOrUpdate(newVariable)
            base.variables.add(oldPosition + 1, newPersistedVariable)
        }

    fun deleteVariable(variableId: String) =
        commitTransaction {
            getVariableById(variableId)
                .findFirst()
                ?.apply {
                    options?.deleteAllFromRealm()
                    deleteFromRealm()
                }
        }

}