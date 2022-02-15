package ch.rmy.android.http_shortcuts.activities.misc.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.Entrypoint
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.finishWithoutAnimation
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.showToast
import ch.rmy.android.http_shortcuts.utils.FileUtil
import ch.rmy.android.http_shortcuts.utils.UUIDUtils
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ShareActivity : BaseActivity(), Entrypoint {

    override val initializeWithTheme: Boolean
        get() = false

    private val viewModel: ShareViewModel by bindViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isRealmAvailable) {
            finish()
            return
        }

        initViewModelBindings()
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        cacheFiles(getFileUris()) { cachedFiles ->
            viewModel.initialize(text, cachedFiles)
        }
    }

    private fun initViewModelBindings() {
        viewModel.events.observe(this, ::handleEvent)
    }

    private fun getFileUris(): List<Uri> =
        if (intent.action == Intent.ACTION_SEND) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { listOf(it) }
        } else {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
            ?: emptyList()

    private fun cacheFiles(fileUris: List<Uri>, action: (List<Uri>) -> Unit) {
        if (fileUris.isEmpty()) {
            action(emptyList())
            return
        }
        val context = applicationContext
        Single.fromCallable {
            cacheSharedFiles(context, fileUris)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                action,
                { e ->
                    showToast(R.string.error_generic)
                    logException(e)
                    finishWithoutAnimation()
                }
            )
            .attachTo(destroyer)
    }

    companion object {
        private fun cacheSharedFiles(context: Context, fileUris: List<Uri>) =
            fileUris
                .map { fileUri ->
                    context.contentResolver.openInputStream(fileUri)!!
                        .use { stream ->
                            FileUtil.createCacheFile(context, createCacheFileName())
                                .also { file ->
                                    FileUtil.getFileName(context.contentResolver, fileUri)
                                        ?.let { fileName ->
                                            FileUtil.putCacheFileOriginalName(file, fileName)
                                        }
                                    stream.copyTo(context.contentResolver.openOutputStream(file)!!)
                                }
                        }
                }

        private fun createCacheFileName() = "shared_${UUIDUtils.newUUID()}"
    }
}
