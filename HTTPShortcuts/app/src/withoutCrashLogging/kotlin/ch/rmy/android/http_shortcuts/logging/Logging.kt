package ch.rmy.android.http_shortcuts.logging

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ch.rmy.android.framework.extensions.showToast
import ch.rmy.android.http_shortcuts.BuildConfig

object Logging {

    private var context: Context? = null

    fun initCrashReporting(context: Context) {
        if (BuildConfig.DEBUG) {
            this.context = context
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun disableCrashReporting(context: Context) {
    }

    val supportsCrashReporting: Boolean = false

    fun logException(origin: String, e: Throwable) {
        if (BuildConfig.DEBUG) {
            Log.e(origin, "An error occurred", e)
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                context?.showToast("Error: $e", long = true)
            }
        }
    }

    fun logInfo(origin: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(origin, message)
        }
    }
}
