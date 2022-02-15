package ch.rmy.android.http_shortcuts.activities.editor.headers

import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

sealed interface HeaderListItem {
    data class Header(
        val id: String,
        val key: String,
        val value: String,
    ) : HeaderListItem

    object EmptyState : HeaderListItem {
        val title: Localizable
            get() = StringResLocalizable(R.string.empty_state_request_headers)

        val instructions: Localizable
            get() = StringResLocalizable(R.string.empty_state_request_headers_instructions)
    }
}
