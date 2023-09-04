package ch.rmy.android.http_shortcuts.scripting.actions.types

import ch.rmy.android.http_shortcuts.scripting.ActionAlias
import ch.rmy.android.http_shortcuts.scripting.actions.ActionData
import ch.rmy.android.http_shortcuts.scripting.actions.ActionRunnable
import javax.inject.Inject

class UUIDActionType
@Inject
constructor(
    private val uuidAction: UUIDAction,
) : ActionType {
    override val type = TYPE

    override fun getActionRunnable(actionDTO: ActionData) =
        ActionRunnable(
            action = uuidAction,
            params = Unit,
        )

    override fun getAlias() = ActionAlias(
        functionName = FUNCTION_NAME,
        parameters = 0,
    )

    companion object {
        private const val TYPE = "uuidv4"
        private const val FUNCTION_NAME = "uuidv4"
    }
}
