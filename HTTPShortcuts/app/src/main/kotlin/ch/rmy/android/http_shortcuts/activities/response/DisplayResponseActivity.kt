package ch.rmy.android.http_shortcuts.activities.response

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.ExecuteActivity
import ch.rmy.android.http_shortcuts.databinding.ActivityDisplayResponseImageBinding
import ch.rmy.android.http_shortcuts.databinding.ActivityDisplayResponsePlainBinding
import ch.rmy.android.http_shortcuts.databinding.ActivityDisplayResponseSyntaxHighlightingBinding
import ch.rmy.android.http_shortcuts.databinding.ActivityDisplayResponseSyntaxHighlightingWithDetailsBinding
import ch.rmy.android.http_shortcuts.databinding.ActivityDisplayResponseWebviewBinding
import ch.rmy.android.http_shortcuts.databinding.ActivityDisplayResponseWebviewWithDetailsBinding
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.finishWithoutAnimation
import ch.rmy.android.http_shortcuts.extensions.loadImage
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.extensions.showIfPossible
import ch.rmy.android.http_shortcuts.extensions.showSnackbar
import ch.rmy.android.http_shortcuts.extensions.startActivity
import ch.rmy.android.http_shortcuts.extensions.truncate
import ch.rmy.android.http_shortcuts.http.HttpHeaders
import ch.rmy.android.http_shortcuts.http.HttpStatus
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.utils.FileTypeUtil.TYPE_HTML
import ch.rmy.android.http_shortcuts.utils.FileTypeUtil.TYPE_JSON
import ch.rmy.android.http_shortcuts.utils.FileTypeUtil.TYPE_XML
import ch.rmy.android.http_shortcuts.utils.FileTypeUtil.TYPE_YAML
import ch.rmy.android.http_shortcuts.utils.FileTypeUtil.TYPE_YAML_ALT
import ch.rmy.android.http_shortcuts.utils.FileTypeUtil.isImage
import ch.rmy.android.http_shortcuts.utils.ShareUtil
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class DisplayResponseActivity : BaseActivity() {

    private val shortcutId: String by lazy {
        intent?.extras?.getString(EXTRA_SHORTCUT_ID) ?: ""
    }
    private val shortcutName: String by lazy {
        intent?.extras?.getString(EXTRA_NAME) ?: ""
    }
    private val text: String by lazy {
        intent?.extras?.getString(EXTRA_TEXT) ?: ""
    }
    private val responseFileUri: Uri? by lazy {
        intent?.extras?.getParcelable(EXTRA_RESPONSE_FILE_URI)
    }
    private val type: String? by lazy {
        intent?.extras?.getString(EXTRA_TYPE)
    }
    private val url: String? by lazy {
        intent?.extras?.getString(EXTRA_URL)
    }
    private val statusCode: Int? by lazy {
        intent?.extras?.getInt(EXTRA_STATUS_CODE)?.takeUnless { it == 0 }
    }
    private val headers: Map<String, List<String>> by lazy {
        (intent?.extras?.getSerializable(EXTRA_HEADERS) as? Map<String, List<String>>) ?: emptyMap()
    }
    private val timing: Long? by lazy {
        intent?.extras?.getLong(EXTRA_TIMING)?.takeUnless { it == 0L }
    }
    private val showDetails: Boolean by lazy {
        intent?.extras?.getBoolean(EXTRA_DETAILS, false) ?: false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = shortcutName
        updateViews()
    }

    private fun updateViews() {
        displayBody()
        if (showDetails) {
            displayMetaInfo()
        }
    }

    private fun displayMetaInfo() {
        val processedGeneralData = mutableListOf<Pair<String, String>>()
            .apply {
                if (statusCode != null) {
                    add(context.getString(R.string.label_status_code) to "$statusCode (${HttpStatus.getMessage(statusCode!!)})")
                }
                if (url != null) {
                    add(context.getString(R.string.label_response_url) to url!!)
                }
                if (timing != null) {
                    val milliseconds = timing!!.toInt()
                    add(
                        context.getString(R.string.label_response_timing) to context.resources.getQuantityString(
                            R.plurals.milliseconds,
                            milliseconds,
                            milliseconds,
                        )
                    )
                }
            }

        val processedHeaders = headers.entries
            .flatMap { entry ->
                entry.value.map { value ->
                    entry.key to value
                }
            }

        if (processedGeneralData.isNotEmpty() || processedHeaders.isNotEmpty()) {
            val view = MetaInfoView(context)
            findViewById<ViewGroup>(R.id.meta_info_container).addView(view)
            view.showGeneralInfo(processedGeneralData)
            view.showHeaders(processedHeaders)
        }
    }

    private fun displayBody() {
        if (text.isBlank()) {
            displayAsPlainText(getString(R.string.message_blank_response), italic = true)
        } else {
            if (isImage(type)) {
                displayImage()
                return
            }
            when (type) {
                TYPE_HTML -> {
                    displayInWebView(text, url)
                }
                TYPE_JSON -> {
                    displayWithSyntaxHighlighting(text, SyntaxHighlightView.Language.JSON)
                }
                TYPE_XML -> {
                    displayWithSyntaxHighlighting(text, SyntaxHighlightView.Language.XML)
                }
                TYPE_YAML, TYPE_YAML_ALT -> {
                    displayWithSyntaxHighlighting(text, SyntaxHighlightView.Language.YAML)
                }
                else -> {
                    displayAsPlainText(text)
                }
            }
        }
    }

    private fun displayImage() {
        val binding = applyBinding(ActivityDisplayResponseImageBinding.inflate(layoutInflater))
        binding.responseImage.loadImage(responseFileUri!!)
    }

    private fun displayAsPlainText(text: String, italic: Boolean = false) {
        val binding = applyBinding(ActivityDisplayResponsePlainBinding.inflate(layoutInflater))
        if (italic) {
            binding.responseText.setTypeface(null, Typeface.ITALIC)
        }
        binding.responseText.text = text
    }

    private fun displayInWebView(text: String, url: String?) {
        try {
            if (showDetails) {
                val binding = applyBinding(ActivityDisplayResponseWebviewWithDetailsBinding.inflate(layoutInflater))
                binding.responseWebView.loadFromString(text, url)
            } else {
                val binding = applyBinding(ActivityDisplayResponseWebviewBinding.inflate(layoutInflater))
                binding.responseWebView.loadFromString(text, url)
            }
        } catch (e: Exception) {
            logException(e)
            displayAsPlainText(text)
        }
    }

    private fun displayWithSyntaxHighlighting(text: String, language: SyntaxHighlightView.Language) {
        try {
            if (showDetails) {
                val binding = applyBinding(ActivityDisplayResponseSyntaxHighlightingWithDetailsBinding.inflate(layoutInflater))
                binding.formattedResponseText.setCode(text, language)
            } else {
                val binding = applyBinding(ActivityDisplayResponseSyntaxHighlightingBinding.inflate(layoutInflater))
                binding.formattedResponseText.setCode(text, language)
            }
        } catch (e: Exception) {
            logException(e)
            displayAsPlainText(text)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.display_response_activity_menu, menu)
        menu.findItem(R.id.action_share_response).isVisible = canShare()
        menu.findItem(R.id.action_save_response_as_file).isVisible = canExport()
        return super.onCreateOptionsMenu(menu)
    }

    private fun canShare() =
        text.isNotEmpty() && responseFileUri != null

    private fun canExport() =
        text.isNotEmpty() && responseFileUri != null

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_rerun -> consume { rerunShortcut() }
        R.id.action_share_response -> consume { shareResponse() }
        R.id.action_save_response_as_file -> consume {
            openFilePicker()
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun rerunShortcut() {
        ExecuteActivity.IntentBuilder(shortcutId)
            .startActivity(context)
        finishWithoutAnimation()
    }

    private fun shareResponse() {
        if (shouldShareAsText()) {
            ShareUtil.shareText(context, text)
        } else {
            Intent(Intent.ACTION_SEND)
                .setType(type)
                .putExtra(Intent.EXTRA_STREAM, responseFileUri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .let {
                    Intent.createChooser(it, shortcutName)
                }
                .startActivity(this)
        }
    }

    private fun shouldShareAsText() =
        !isImage(type) && text.length < MAX_SHARE_LENGTH

    private fun openFilePicker() {
        try {
            Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(type)
                .putExtra(Intent.EXTRA_TITLE, shortcutName)
                .startActivity(this, REQUEST_SAVE_FILE)
        } catch (e: ActivityNotFoundException) {
            showSnackbar(R.string.error_not_supported)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode != RESULT_OK || intent == null) {
            return
        }
        when (requestCode) {
            REQUEST_SAVE_FILE -> {
                saveResponseToFile(intent.data ?: return)
            }
        }
    }

    private fun saveResponseToFile(uri: Uri) {
        val progressDialog = ProgressDialog(context).apply {
            setMessage(getString(R.string.saving_in_progress))
            setCanceledOnTouchOutside(false)
        }
        // TODO: Separate concerns better (this should not be in the activity)
        Completable
            .fromAction {
                context.contentResolver.openOutputStream(uri).use { output ->
                    context.contentResolver.openInputStream(responseFileUri!!).use { input ->
                        input!!.copyTo(output!!)
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe {
                progressDialog.showIfPossible()
            }
            .doOnEvent {
                progressDialog.hide()
            }
            .subscribe(
                {
                    showSnackbar(R.string.message_response_saved_to_file)
                },
                { e ->
                    showSnackbar(R.string.error_generic)
                    logException(e)
                },
            )
            .attachTo(destroyer)
    }

    override val navigateUpIcon = R.drawable.ic_clear

    class IntentBuilder(shortcutId: String) : BaseIntentBuilder(DisplayResponseActivity::class.java) {

        init {
            intent.putExtra(EXTRA_SHORTCUT_ID, shortcutId)
        }

        fun name(name: String) = also {
            intent.putExtra(EXTRA_NAME, name)
        }

        fun type(type: String?) = also {
            intent.putExtra(EXTRA_TYPE, type)
        }

        fun text(text: String) = also {
            intent.putExtra(EXTRA_TEXT, text.truncate(MAX_TEXT_LENGTH))
        }

        fun responseFileUri(uri: Uri?) = also {
            intent.putExtra(EXTRA_RESPONSE_FILE_URI, uri)
        }

        fun url(url: String?) = also {
            intent.putExtra(EXTRA_URL, url)
        }

        fun showDetails(showDetails: Boolean) = also {
            intent.putExtra(EXTRA_DETAILS, showDetails)
        }

        fun headers(headers: HttpHeaders?) = also {
            intent.putExtra(
                EXTRA_HEADERS,
                headers?.toMultiMap()?.let {
                    HashMap<String, ArrayList<String>>().apply {
                        it.forEach { (name, values) ->
                            put(name, ArrayList<String>().also { it.addAll(values) })
                        }
                    }
                }
            )
        }

        fun statusCode(statusCode: Int?) = also {
            intent.putExtra(EXTRA_STATUS_CODE, statusCode ?: return@also)
        }

        fun timing(timing: Long?) = also {
            intent.putExtra(EXTRA_TIMING, timing ?: return@also)
        }
    }

    companion object {
        private const val EXTRA_SHORTCUT_ID = "id"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_TYPE = "type"
        private const val EXTRA_TEXT = "text"
        private const val EXTRA_RESPONSE_FILE_URI = "response_file_uri"
        private const val EXTRA_URL = "url"
        private const val EXTRA_HEADERS = "headers"
        private const val EXTRA_STATUS_CODE = "status_code"
        private const val EXTRA_TIMING = "timing"
        private const val EXTRA_DETAILS = "details"

        private const val REQUEST_SAVE_FILE = 1

        private const val MAX_TEXT_LENGTH = 700000
        private const val MAX_SHARE_LENGTH = 300000
    }
}
