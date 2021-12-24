package ch.rmy.android.http_shortcuts.activities.editor.body

import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable

data class RequestBodyViewState(
    val requestBodyType: String = "",
    val parameters: List<ParameterListItem> = emptyList(),
    val variables: List<Variable> = emptyList(),
    val contentType: String = "",
    val bodyContent: String = "",
) {
    val isDraggingEnabled: Boolean
        get() = parameters.size > 1

    val parameterListVisible: Boolean
        get() = requestBodyType == Shortcut.REQUEST_BODY_TYPE_FORM_DATA
            || requestBodyType == Shortcut.REQUEST_BODY_TYPE_X_WWW_FORM_URLENCODE

    val addParameterButtonVisible: Boolean
        get() = parameterListVisible

    val contentTypeVisible: Boolean
        get() = requestBodyType == Shortcut.REQUEST_BODY_TYPE_CUSTOM_TEXT

    val bodyContentVisible: Boolean
        get() = contentTypeVisible
}