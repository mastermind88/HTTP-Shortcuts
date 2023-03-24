package ch.rmy.android.http_shortcuts.activities.curl_import

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.components.BackButton
import ch.rmy.android.http_shortcuts.components.ScreenScope
import ch.rmy.android.http_shortcuts.components.SimpleScaffold
import ch.rmy.android.http_shortcuts.components.bindViewModel

@Composable
fun ScreenScope.CurlImportScreen() {
    val (viewModel, state) = bindViewModel<Unit, CurlImportViewState, CurlImportViewModel>(Unit)

    val inputText by viewModel.inputText.collectAsState()

    SimpleScaffold(
        viewState = state,
        title = stringResource(R.string.title_curl_import),
        backButton = BackButton.CROSS,
        actions = { viewState ->
            if (viewState.submitButtonVisible) {
                IconButton(
                    onClick = {
                        viewModel.onSubmitButtonClicked()
                    },
                ) {
                    Icon(Icons.Filled.Check, stringResource(R.string.curl_import_button))
                }
            }
        },
    ) {
        CurlImportContent(
            inputText = inputText,
            onInputTextChanged = viewModel::onInputTextChanged,
            onSubmit = viewModel::onSubmitButtonClicked,
        )
    }
}
