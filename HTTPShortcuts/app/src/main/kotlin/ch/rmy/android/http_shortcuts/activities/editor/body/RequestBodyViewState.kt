package ch.rmy.android.http_shortcuts.activities.editor.body

import ch.rmy.android.http_shortcuts.data.enums.RequestBodyType
import ch.rmy.android.http_shortcuts.data.models.Variable

data class RequestBodyViewState(
    val requestBodyType: RequestBodyType = RequestBodyType.CUSTOM_TEXT,
    val parameters: List<ParameterListItem> = emptyList(),
    val variables: List<Variable>? = null,
    val contentType: String = "",
    val bodyContent: String = "",
) {
    val isDraggingEnabled: Boolean
        get() = parameters.size > 1

    val parameterListVisible: Boolean
        get() = requestBodyType == RequestBodyType.FORM_DATA ||
            requestBodyType == RequestBodyType.X_WWW_FORM_URLENCODE

    val addParameterButtonVisible: Boolean
        get() = parameterListVisible

    val contentTypeVisible: Boolean
        get() = requestBodyType == RequestBodyType.CUSTOM_TEXT

    val bodyContentVisible: Boolean
        get() = contentTypeVisible
}
