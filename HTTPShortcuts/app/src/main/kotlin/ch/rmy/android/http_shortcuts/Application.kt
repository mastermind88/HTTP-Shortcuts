package ch.rmy.android.http_shortcuts

import android.content.Context
import androidx.multidex.MultiDex
import ch.rmy.android.framework.WithRealm
import ch.rmy.android.framework.extensions.logException
import ch.rmy.android.http_shortcuts.data.RealmFactory
import ch.rmy.android.http_shortcuts.logging.Logging
import ch.rmy.android.http_shortcuts.utils.DarkThemeHelper
import ch.rmy.android.http_shortcuts.utils.LocaleHelper
import ch.rmy.android.http_shortcuts.utils.Settings
import com.facebook.stetho.Stetho
import io.reactivex.plugins.RxJavaPlugins
import org.conscrypt.Conscrypt
import java.security.Security

class Application : android.app.Application(), WithRealm {

    override var isRealmAvailable: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        Security.insertProviderAt(Conscrypt.newProvider(), 1)

        Logging.initCrashReporting(context)

        Stetho.initializeWithDefaults(context)

        RxJavaPlugins.setErrorHandler(::logException)

        try {
            RealmFactory.init(applicationContext)
            isRealmAvailable = true
        } catch (e: RealmFactory.RealmNotFoundException) {
            // Nothing to do here...
        }

        DarkThemeHelper.applyDarkThemeSettings(Settings(context).darkThemeSetting)
    }

    public override fun attachBaseContext(base: Context) {
        MultiDex.install(base)
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }

    private val context: Context
        get() = this
}
