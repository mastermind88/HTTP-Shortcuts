package ch.rmy.android.http_shortcuts.activities

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewbinding.ViewBinding
import ch.rmy.android.http_shortcuts.Application
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.RealmFactory
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.color
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.drawable
import ch.rmy.android.http_shortcuts.extensions.finishWithoutAnimation
import ch.rmy.android.http_shortcuts.extensions.logInfo
import ch.rmy.android.http_shortcuts.extensions.openURL
import ch.rmy.android.http_shortcuts.extensions.setTintCompat
import ch.rmy.android.http_shortcuts.extensions.showSnackbar
import ch.rmy.android.http_shortcuts.extensions.showToast
import ch.rmy.android.http_shortcuts.utils.Destroyer
import ch.rmy.android.http_shortcuts.utils.LocaleHelper
import ch.rmy.android.http_shortcuts.utils.SnackbarManager
import ch.rmy.android.http_shortcuts.utils.ThemeHelper

abstract class BaseActivity : AppCompatActivity() {

    internal var toolbar: Toolbar? = null

    val destroyer = Destroyer()

    val themeHelper by lazy {
        ThemeHelper(context)
    }

    open val initializeWithTheme: Boolean
        get() = true

    val baseView: ViewGroup?
        get() = (findViewById<ViewGroup>(android.R.id.content))?.getChildAt(0) as ViewGroup?

    val isRealmAvailable: Boolean
        get() = (application as? Application)?.isRealmAvailable ?: false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (initializeWithTheme) {
            setTheme(themeHelper.theme)
        }
        super.onCreate(savedInstanceState)
        try {
            RealmFactory.init(applicationContext)
        } catch (e: RealmFactory.RealmNotFoundException) {
            if (this is Entrypoint) {
                showRealmError()
            } else {
                throw e
            }
        }
    }

    override fun onStart() {
        super.onStart()
        SnackbarManager.showEnqueuedSnackbars(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    private fun showRealmError() {
        DialogBuilder(context)
            .title(R.string.dialog_title_error)
            .message(R.string.error_realm_unavailable, isHtml = true)
            .positive(R.string.dialog_ok)
            .dismissListener {
                finish()
            }
            .showIfPossible()
    }

    fun <T : ViewBinding> applyBinding(binding: T): T =
        binding.also {
            setContentView(binding.root)
            setUpCommonViews()
        }

    private fun setUpCommonViews() {
        baseView?.setBackgroundColor(color(context, R.color.activity_background))
        toolbar = findViewById(R.id.toolbar) ?: return
        updateStatusBarColor()
        setSupportActionBar(toolbar)
        if (navigateUpIcon != 0) {
            enableNavigateUpButton(navigateUpIcon)
        }
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        setUpCommonViews()
    }

    fun setSubtitle(@StringRes subtitle: Int) {
        toolbar?.setSubtitle(subtitle)
    }

    val context: Context
        get() = this

    protected open val navigateUpIcon = R.drawable.up_arrow

    private fun enableNavigateUpButton(iconResource: Int) {
        val actionBar = supportActionBar ?: return
        actionBar.setDisplayHomeAsUpEnabled(true)
        val upArrow = drawable(context, iconResource) ?: return
        upArrow.setTintCompat(Color.WHITE)
        actionBar.setHomeAsUpIndicator(upArrow)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> consume { onBackPressed() }
        else -> super.onOptionsItemSelected(item)
    }

    private fun updateStatusBarColor() {
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            statusBarColor = themeHelper.statusBarColor
        }
    }

    open fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is ViewModelEvent.OpenActivity -> {
                try {
                    event.intentBuilder.startActivity(this, event.requestCode)
                } catch (e: ActivityNotFoundException) {
                    showToast(R.string.error_not_supported)
                }
            }
            is ViewModelEvent.OpenURL -> {
                openURL(event.url)
            }
            is ViewModelEvent.Finish -> {
                if (event.result != null) {
                    if (event.intent != null) {
                        setResult(event.result, event.intent)
                    } else {
                        setResult(event.result)
                    }
                }
                if (event.skipAnimation) {
                    finishWithoutAnimation()
                } else {
                    finish()
                }
            }
            is ViewModelEvent.ShowDialog -> {
                event.dialogBuilder(context)
            }
            is ViewModelEvent.ShowSnackbar -> {
                showSnackbar(event.message.localize(context), long = event.long)
            }
            else -> logInfo("Unhandled event: $event")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyer.destroy()
    }
}
