package ch.rmy.android.http_shortcuts.variables.types

import android.content.Context
import android.text.InputType
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Variable
import io.reactivex.Single

internal class NumberType : TextType() {

    private val variablesRepository = VariableRepository()

    override fun resolveValue(context: Context, variable: Variable): Single<String> =
        Single.create<String> { emitter ->
            createDialogBuilder(context, variable, emitter)
                .textInput(
                    prefill = variable.value?.takeIf { variable.rememberValue }?.toIntOrNull()?.toString() ?: "",
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED,
                    allowEmpty = false,
                    callback = emitter::onSuccess,
                )
                .showIfPossible()
        }
            .map(::sanitize)
            .storeValueIfNeeded(variable, variablesRepository)

    private fun sanitize(input: String) =
        input.trimEnd('.')
            .let {
                when {
                    it.startsWith("-.") -> "-0.${it.drop(2)}"
                    it.startsWith(".") -> "0$it"
                    it.isEmpty() || it == "-" -> "0"
                    else -> it
                }
            }
}
