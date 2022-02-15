package ch.rmy.android.http_shortcuts.activities.misc.deeplink

import android.os.Bundle
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.Entrypoint
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.observe

class DeepLinkActivity : BaseActivity(), Entrypoint {

    override val initializeWithTheme: Boolean
        get() = false

    private val viewModel: DeepLinkViewModel by bindViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isRealmAvailable) {
            return
        }

        initViewModelBindings()
        viewModel.initialize(intent.data)
    }

    private fun initViewModelBindings() {
        viewModel.events.observe(this, ::handleEvent)
    }
}
