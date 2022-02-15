package ch.rmy.android.http_shortcuts.activities.editor.shortcuts

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.databinding.ActivityTriggerShortcutsBinding
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.extensions.applyTheme
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.mapFor
import ch.rmy.android.http_shortcuts.scripting.shortcuts.ShortcutPlaceholder
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.utils.DragOrderingHelper

class TriggerShortcutsActivity : BaseActivity() {

    private val currentShortcutId: String? by lazy {
        intent.getStringExtra(EXTRA_SHORTCUT_ID)
    }

    private val viewModel: TriggerShortcutsViewModel by bindViewModel()

    private lateinit var binding: ActivityTriggerShortcutsBinding
    private lateinit var adapter: ShortcutsAdapter

    private var isDraggingEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = applyBinding(ActivityTriggerShortcutsBinding.inflate(layoutInflater))
        setTitle(R.string.label_trigger_shortcuts)

        initViews()
        initUserInputBindings()
        initViewModelBindings()
        viewModel.initialize(currentShortcutId)
    }

    private fun initViews() {
        binding.buttonAddTrigger.applyTheme(themeHelper)

        val manager = LinearLayoutManager(context)
        binding.triggerShortcutsList.layoutManager = manager
        binding.triggerShortcutsList.setHasFixedSize(true)

        adapter = ShortcutsAdapter()
        binding.triggerShortcutsList.adapter = adapter
    }

    private fun initUserInputBindings() {
        adapter.userEvents
            .subscribe { event ->
                when (event) {
                    is ShortcutsAdapter.UserEvent.ShortcutClicked -> {
                        viewModel.onShortcutClicked(event.id)
                    }
                }
            }
            .attachTo(destroyer)

        binding.buttonAddTrigger.setOnClickListener {
            viewModel.onAddButtonClicked()
        }
        initDragOrdering()
    }

    private fun initDragOrdering() {
        val dragOrderingHelper = DragOrderingHelper { isDraggingEnabled }
        dragOrderingHelper.attachTo(binding.triggerShortcutsList)
        dragOrderingHelper.positionChangeSource
            .subscribe { (oldPosition, newPosition) ->
                viewModel.onShortcutMoved(oldPosition, newPosition)
            }
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState
            .subscribe { viewState ->
                adapter.items = viewState.triggerShortcuts
                isDraggingEnabled = viewState.isDraggingEnabled
            }
            .attachTo(destroyer)
        viewModel.events
            .subscribe(::handleEvent)
            .attachTo(destroyer)
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is TriggerShortcutsEvent.ShowShortcutPickerForAdding -> {
                showShortcutPickerForAdding(event.placeholders)
            }
            is TriggerShortcutsEvent.ShowRemoveShortcutDialog -> {
                showRemoveShortcutDialog(event.shortcutId, event.shortcutName)
            }
            else -> super.handleEvent(event)
        }
    }

    private fun showShortcutPickerForAdding(placeholders: List<ShortcutPlaceholder>) {
        DialogBuilder(context)
            .title(R.string.title_add_trigger_shortcut)
            .mapFor(placeholders) { shortcut ->
                item(name = shortcut.name, shortcutIcon = shortcut.icon) {
                    viewModel.onAddShortcutDialogConfirmed(shortcut.id)
                }
            }
            .showIfPossible()
    }

    private fun showRemoveShortcutDialog(shortcutId: String, shortcutName: String) {
        DialogBuilder(context)
            .title(R.string.title_remove_trigger_shortcut)
            .message(getString(R.string.message_remove_trigger_shortcut, shortcutName))
            .positive(R.string.dialog_remove) {
                viewModel.onRemoveShortcutDialogConfirmed(shortcutId)
            }
            .negative(R.string.dialog_cancel)
            .showIfPossible()
    }

    class IntentBuilder(context: Context) : BaseIntentBuilder(context, TriggerShortcutsActivity::class.java) {

        fun shortcutId(shortcutId: String?) = also {
            intent.putExtra(EXTRA_SHORTCUT_ID, shortcutId)
        }
    }

    companion object {
        private const val EXTRA_SHORTCUT_ID = "shortcutId"
    }
}
