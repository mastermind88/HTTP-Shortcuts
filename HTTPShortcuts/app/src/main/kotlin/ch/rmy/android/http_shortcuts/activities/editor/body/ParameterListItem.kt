package ch.rmy.android.http_shortcuts.activities.editor.body

import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

sealed interface ParameterListItem {
    data class Parameter(
        val id: String,
        val key: String,
        val value: String?,
        val label: Localizable?,
    ) : ParameterListItem

    object EmptyState : ParameterListItem {
        val title: Localizable
            get() = StringResLocalizable(R.string.empty_state_request_parameters)

        val instructions: Localizable
            get() = StringResLocalizable(R.string.empty_state_request_parameters_instructions)
    }
}