package ch.rmy.android.http_shortcuts.scripting.actions.types

import ch.rmy.android.framework.extensions.toHexString
import ch.rmy.android.http_shortcuts.scripting.ExecutionContext
import javax.inject.Inject

class ToHexStringAction
@Inject
constructor() : Action<ToHexStringAction.Params> {
    override suspend fun Params.execute(executionContext: ExecutionContext): String =
        data.toHexString()

    data class Params(
        val data: ByteArray,
    )
}
