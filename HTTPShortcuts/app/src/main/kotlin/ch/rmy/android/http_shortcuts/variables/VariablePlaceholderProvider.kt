package ch.rmy.android.http_shortcuts.variables

import ch.rmy.android.http_shortcuts.data.models.Variable

class VariablePlaceholderProvider(var variables: Collection<Variable> = emptyList()) {

    val placeholders
        get() = variables.map(::toPlaceholder)

    val hasVariables
        get() = variables.isNotEmpty()

    val hasConstants
        get() = variables.any { it.isConstant }

    fun findPlaceholderById(variableId: String): VariablePlaceholder? =
        variables
            .firstOrNull { it.id == variableId }
            ?.let(::toPlaceholder)

    companion object {

        private fun toPlaceholder(variable: Variable) =
            VariablePlaceholder(
                variableId = variable.id,
                variableKey = variable.key,
            )
    }
}
