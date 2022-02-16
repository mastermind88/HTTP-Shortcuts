package ch.rmy.android.http_shortcuts.variables.types

import android.content.Context
import android.text.InputType
import ch.rmy.android.framework.extensions.mapIf
import ch.rmy.android.http_shortcuts.data.domains.variables.VariableRepository
import ch.rmy.android.http_shortcuts.data.models.Variable
import io.reactivex.Single

class PasswordType : TextType() {

    private val variablesRepository = VariableRepository()

    override fun resolveValue(context: Context, variable: Variable): Single<String> =
        Single.create<String> { emitter ->
            createDialogBuilder(context, variable, emitter)
                .textInput(
                    prefill = variable.value?.takeIf { variable.rememberValue } ?: "",
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                ) { input ->
                    emitter.onSuccess(input)
                }
                .showIfPossible()
        }
            .mapIf(variable.rememberValue) {
                flatMap { resolvedValue ->
                    variablesRepository.setVariableValue(variable.id, resolvedValue)
                        .toSingle { resolvedValue }
                }
            }
}
