package ch.rmy.android.http_shortcuts.activities.editor.advancedsettings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.widget.SeekBar
import android.widget.TextView
import ch.rmy.android.framework.extensions.attachTo
import ch.rmy.android.framework.extensions.bindViewModel
import ch.rmy.android.framework.extensions.initialize
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.framework.extensions.observe
import ch.rmy.android.framework.extensions.observeChecked
import ch.rmy.android.framework.extensions.observeTextChanges
import ch.rmy.android.framework.extensions.setSubtitle
import ch.rmy.android.framework.extensions.setText
import ch.rmy.android.framework.extensions.setTextSafely
import ch.rmy.android.framework.extensions.showSnackbar
import ch.rmy.android.framework.extensions.showToast
import ch.rmy.android.framework.extensions.startActivity
import ch.rmy.android.framework.ui.BaseIntentBuilder
import ch.rmy.android.framework.utils.FilePickerUtil
import ch.rmy.android.framework.utils.RxUtils
import ch.rmy.android.framework.utils.SimpleOnSeekBarChangeListener
import ch.rmy.android.framework.utils.UUIDUtils.newUUID
import ch.rmy.android.framework.utils.localization.Localizable
import ch.rmy.android.framework.viewmodel.ViewModelEvent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.data.models.ClientCertParams
import ch.rmy.android.http_shortcuts.databinding.ActivityAdvancedSettingsBinding
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.cancel
import ch.rmy.android.http_shortcuts.utils.ClientCertUtil
import ch.rmy.android.http_shortcuts.variables.VariablePlaceholderProvider
import ch.rmy.android.http_shortcuts.variables.VariableViewUtils
import io.reactivex.Single
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AdvancedSettingsActivity : BaseActivity() {

    private val viewModel: AdvancedSettingsViewModel by bindViewModel()
    private val variablePlaceholderProvider = VariablePlaceholderProvider()

    private lateinit var binding: ActivityAdvancedSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initialize()
        initViews()
        initUserInputBindings()
        initViewModelBindings()
    }

    private fun initViews() {
        binding = applyBinding(ActivityAdvancedSettingsBinding.inflate(layoutInflater))
        setTitle(R.string.label_advanced_technical_settings)
    }

    private fun initUserInputBindings() {
        binding.inputFollowRedirects
            .observeChecked()
            .subscribe { isChecked ->
                viewModel.onFollowRedirectsChanged(isChecked)
            }
            .attachTo(destroyer)
        binding.inputAcceptCertificates
            .observeChecked()
            .subscribe { isChecked ->
                viewModel.onAcceptAllCertificatesChanged(isChecked)
            }
            .attachTo(destroyer)
        binding.inputAcceptCookies
            .observeChecked()
            .subscribe { isChecked ->
                viewModel.onAcceptCookiesChanged(isChecked)
            }
            .attachTo(destroyer)

        binding.inputProxyHost.observeTextChanges()
            .subscribe {
                viewModel.onProxyHostChanged(binding.inputProxyHost.rawString)
            }
            .attachTo(destroyer)
        binding.inputProxyPort.observeTextChanges()
            .subscribe {
                viewModel.onProxyPortChanged(binding.inputProxyPort.text.toString().toIntOrNull())
            }
            .attachTo(destroyer)
        binding.inputSsid.observeTextChanges()
            .subscribe {
                viewModel.onWifiSsidChanged(binding.inputSsid.text.toString())
            }
            .attachTo(destroyer)

        binding.buttonClientCert.setOnClickListener {
            viewModel.onClientCertButtonClicked()
        }

        binding.inputTimeout.setOnClickListener {
            viewModel.onTimeoutButtonClicked()
        }

        VariableViewUtils.bindVariableViews(binding.inputProxyHost, binding.variableButtonProxyHost, variablePlaceholderProvider)
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.inputFollowRedirects.isChecked = viewState.followRedirects
            binding.inputAcceptCertificates.isChecked = viewState.acceptAllCertificates
            binding.buttonClientCert.isEnabled = viewState.isClientCertButtonEnabled
            binding.buttonClientCert.setSubtitle(viewState.clientCertSubtitle)
            binding.inputAcceptCookies.isChecked = viewState.acceptCookies
            binding.inputTimeout.setSubtitle(viewState.timeoutSubtitle)
            binding.inputProxyHost.rawString = viewState.proxyHost
            binding.inputProxyPort.setTextSafely(viewState.proxyPort)
            binding.inputSsid.setTextSafely(viewState.wifiSsid)
            viewState.variables?.let(variablePlaceholderProvider::applyVariables)
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is AdvancedSettingsEvent.ShowClientCertDialog -> {
                openClientCertDialog()
            }
            is AdvancedSettingsEvent.ShowTimeoutDialog -> {
                showTimeoutDialog(event.timeout, event.getLabel)
            }
            else -> super.handleEvent(event)
        }
    }

    private fun openClientCertDialog() {
        DialogBuilder(context)
            .title(R.string.title_client_cert)
            .item(R.string.label_client_cert_from_os, descriptionRes = R.string.label_client_cert_from_os_subtitle) {
                promptForClientCertAlias()
            }
            .item(R.string.label_client_cert_from_file, descriptionRes = R.string.label_client_cert_from_file_subtitle) {
                openCertificateFilePicker()
            }
            .showIfPossible()
    }

    private fun promptForClientCertAlias() {
        try {
            ClientCertUtil.promptForAlias(this) { alias ->
                viewModel.onClientCertParamsChanged(
                    ClientCertParams.Alias(alias)
                )
            }
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.error_not_supported)
        }
    }

    private fun openCertificateFilePicker() {
        try {
            FilePickerUtil.createIntent(type = "application/x-pkcs12")
                .startActivity(this, REQUEST_SELECT_CERTIFICATE_FILE)
        } catch (e: ActivityNotFoundException) {
            showToast(R.string.error_not_supported)
        }
    }

    // TODO: Move this out into its own class
    private fun showTimeoutDialog(timeout: Duration, getLabel: (Duration) -> Localizable) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker, null)

        val slider = view.findViewById<SeekBar>(R.id.slider)
        val label = view.findViewById<TextView>(R.id.slider_value)

        slider.max = TIMEOUT_OPTIONS.lastIndex

        slider.setOnSeekBarChangeListener(object : SimpleOnSeekBarChangeListener() {
            override fun onProgressChanged(slider: SeekBar, progress: Int, fromUser: Boolean) {
                label.text = getLabel(progressToTimeout(progress)).localize(context)
            }
        })
        label.setText(getLabel(timeout))
        slider.progress = timeoutToProgress(timeout)

        DialogBuilder(context)
            .title(R.string.label_timeout)
            .view(view)
            .positive(R.string.dialog_ok) {
                viewModel.onTimeoutChanged(progressToTimeout(slider.progress))
            }
            .showIfPossible()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode != RESULT_OK || intent == null) {
            return
        }
        when (requestCode) {
            REQUEST_SELECT_CERTIFICATE_FILE -> {
                onCertificateFileSelected(intent.data ?: return)
            }
        }
    }

    private fun onCertificateFileSelected(file: Uri) {
        copyCertificateFile(file)
            .flatMap { fileName ->
                promptForPassword()
                    .map { password ->
                        ClientCertParams.File(fileName, password)
                    }
            }
            .subscribe(
                viewModel::onClientCertParamsChanged,
            ) { e ->
                logException(e)
                showSnackbar(R.string.error_generic)
            }
            .attachTo(destroyer)
    }

    private fun copyCertificateFile(file: Uri): Single<String> =
        RxUtils.single {
            val fileName = "${newUUID()}.p12"
            contentResolver.openInputStream(file)!!.use { inputStream ->
                context.openFileOutput(fileName, MODE_PRIVATE).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            fileName
        }

    private fun promptForPassword(): Single<String> =
        Single.create { emitter ->
            DialogBuilder(context)
                .title(R.string.title_client_cert_file_password)
                .textInput(
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                ) { input ->
                    emitter.onSuccess(input)
                }
                .dismissListener {
                    emitter.cancel()
                }
                .showIfPossible()
        }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    class IntentBuilder : BaseIntentBuilder(AdvancedSettingsActivity::class.java)

    companion object {

        private const val REQUEST_SELECT_CERTIFICATE_FILE = 1

        private val TIMEOUT_OPTIONS = arrayOf(
            500.milliseconds,
            1.seconds,
            2.seconds,
            3.seconds,
            5.seconds,
            8.seconds,
            10.seconds,
            15.seconds,
            20.seconds,
            25.seconds,
            30.seconds,
            45.seconds,
            1.minutes,
            90.seconds,
            2.minutes,
            3.minutes,
            5.minutes,
            450.seconds,
            10.minutes,
        )

        private fun timeoutToProgress(timeout: Duration) = TIMEOUT_OPTIONS.indexOfFirst {
            it >= timeout
        }
            .takeUnless { it == -1 }
            ?: TIMEOUT_OPTIONS.lastIndex

        private fun progressToTimeout(progress: Int) = TIMEOUT_OPTIONS[progress]
    }
}
