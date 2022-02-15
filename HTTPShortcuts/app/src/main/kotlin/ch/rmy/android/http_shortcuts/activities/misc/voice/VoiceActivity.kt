package ch.rmy.android.http_shortcuts.activities.misc.voice

import android.app.SearchManager
import android.os.Bundle
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.Entrypoint
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.observe

class VoiceActivity : BaseActivity(), Entrypoint {

    private val viewModel: VoiceViewModel by bindViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isRealmAvailable) {
            return
        }

        initViewModelBindings()
        viewModel.initialize(intent.getStringExtra(SearchManager.QUERY))
    }

    private fun initViewModelBindings() {
        viewModel.events.observe(this, ::handleEvent)
    }
}
