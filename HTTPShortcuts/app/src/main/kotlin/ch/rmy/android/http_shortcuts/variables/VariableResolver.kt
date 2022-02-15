package ch.rmy.android.http_shortcuts.variables

import android.content.Context
import ch.rmy.android.http_shortcuts.data.models.ResponseHandling
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.variables.types.VariableTypeFactory
import io.reactivex.Completable
import io.reactivex.Single

class VariableResolver(private val context: Context) {

    fun resolve(
        variables: List<Variable>,
        shortcut: Shortcut,
        globalCode: String = "",
        preResolvedValues: Map<String, String> = emptyMap(),
    ): Single<VariableManager> {
        val variableManager = VariableManager(variables)
        val requiredVariableIds = extractVariableIds(shortcut, variableManager)
            .plus(extractVariableIdsFromJS(globalCode, variableManager))
            .toMutableSet()

        val preResolvedVariables = mutableMapOf<Variable, String>()
        preResolvedValues
            .forEach { (variableKey, value) ->
                variableManager.getVariableByKeyOrId(variableKey)?.let { variable ->
                    preResolvedVariables[variable] = value
                }
            }

        val variablesToResolve = requiredVariableIds
            .mapNotNull { variableId ->
                variableManager.getVariableById(variableId)
            }

        return resolveVariables(variablesToResolve, preResolvedVariables)
            .flatMap { resolvedVariables ->
                resolveRecursiveVariables(variableManager, resolvedVariables)
            }
            .map { resolvedValues ->
                resolvedValues
                    .forEach { (variable, value) ->
                        variableManager.setVariableValue(variable, value)
                    }
                variableManager
            }
    }

    private fun resolveRecursiveVariables(
        variableLookup: VariableLookup,
        preResolvedValues: Map<Variable, String>,
        recursionDepth: Int = 0,
    ): Single<Map<Variable, String>> {
        val requiredVariableIds = mutableSetOf<String>()
        preResolvedValues.values.forEach { value ->
            requiredVariableIds.addAll(Variables.extractVariableIds(value))
        }
        if (recursionDepth >= MAX_RECURSION_DEPTH || requiredVariableIds.isEmpty()) {
            return Single.just(preResolvedValues)
        }

        val variablesToResolve = requiredVariableIds
            .mapNotNull { variableId ->
                variableLookup.getVariableById(variableId)
            }
        return resolveVariables(variablesToResolve, preResolvedValues)
            .map {
                it.toMutableMap().also { resolvedVariables ->
                    resolvedVariables.forEach { resolvedVariable ->
                        resolvedVariables[resolvedVariable.key] =
                            Variables.rawPlaceholdersToResolvedValues(
                                resolvedVariable.value,
                                resolvedVariables.mapKeys { it.key.id },
                            )
                    }
                }
            }
            .flatMap { resolvedVariables ->
                resolveRecursiveVariables(variableLookup, resolvedVariables, recursionDepth + 1)
            }
    }

    fun resolveVariables(
        variablesToResolve: List<Variable>,
        preResolvedValues: Map<Variable, String> = emptyMap(),
    ): Single<Map<Variable, String>> {
        var completable = Completable.complete()
        val resolvedVariables = preResolvedValues.toMutableMap()

        for (variable in variablesToResolve) {
            if (resolvedVariables.keys.any { it.id == variable.id }) {
                // Variable value is already resolved
                continue
            }
            val preResolvedValue = preResolvedValues.entries
                .firstOrNull { it.key.id == variable.id }
                ?.value
            if (preResolvedValue != null) {
                // Variable value was pre-resolved
                resolvedVariables[variable] = preResolvedValue
                continue
            }

            val variableType = VariableTypeFactory.getType(variable.variableType)
            completable = completable.concatWith(
                variableType.resolveValue(context, variable)
                    .doOnSuccess { resolvedValue ->
                        resolvedVariables[variable] = resolvedValue
                    }
                    .ignoreElement()
            )
        }

        return completable.toSingle { resolvedVariables }
    }

    companion object {

        private const val MAX_RECURSION_DEPTH = 3

        fun extractVariableIds(shortcut: Shortcut, variableLookup: VariableLookup): Set<String> =
            mutableSetOf<String>().apply {
                addAll(Variables.extractVariableIds(shortcut.url))
                if (shortcut.usesBasicAuthentication() || shortcut.usesDigestAuthentication()) {
                    addAll(Variables.extractVariableIds(shortcut.username))
                    addAll(Variables.extractVariableIds(shortcut.password))
                }
                if (shortcut.usesBearerAuthentication()) {
                    addAll(Variables.extractVariableIds(shortcut.authToken))
                }
                if (shortcut.usesCustomBody()) {
                    addAll(Variables.extractVariableIds(shortcut.bodyContent))
                }
                if (shortcut.usesRequestParameters()) {
                    for (parameter in shortcut.parameters) {
                        addAll(Variables.extractVariableIds(parameter.key))
                        addAll(Variables.extractVariableIds(parameter.value))
                    }
                }
                for (header in shortcut.headers) {
                    addAll(Variables.extractVariableIds(header.key))
                    addAll(Variables.extractVariableIds(header.value))
                }
                addAll(extractVariableIdsFromJS(shortcut.codeOnPrepare, variableLookup))
                addAll(extractVariableIdsFromJS(shortcut.codeOnSuccess, variableLookup))
                addAll(extractVariableIdsFromJS(shortcut.codeOnFailure, variableLookup))

                if (shortcut.proxyHost != null) {
                    addAll(Variables.extractVariableIds(shortcut.proxyHost!!))
                }

                addAll(Variables.extractVariableIds(shortcut.codeOnPrepare))
                addAll(Variables.extractVariableIds(shortcut.codeOnSuccess))
                addAll(Variables.extractVariableIds(shortcut.codeOnFailure))

                if (shortcut.responseHandling != null && shortcut.responseHandling!!.successOutput == ResponseHandling.SUCCESS_OUTPUT_MESSAGE) {
                    addAll(Variables.extractVariableIds(shortcut.responseHandling!!.successMessage))
                }
            }

        private fun extractVariableIdsFromJS(
            code: String,
            variableLookup: VariableLookup,
        ): Set<String> =
            Variables.extractVariableIdsFromJS(code)
                .plus(
                    Variables.extractVariableKeysFromJS(code)
                        .map { variableKey ->
                            variableLookup.getVariableByKey(variableKey)?.id ?: variableKey
                        }
                )
    }
}
