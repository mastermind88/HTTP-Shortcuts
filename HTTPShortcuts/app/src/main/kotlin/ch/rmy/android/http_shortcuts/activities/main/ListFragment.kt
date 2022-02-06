package ch.rmy.android.http_shortcuts.activities.main

import android.app.Activity.RESULT_OK
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseFragment
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.data.RealmFactory
import ch.rmy.android.http_shortcuts.databinding.FragmentListBinding
import ch.rmy.android.http_shortcuts.dialogs.CurlExportDialog
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.dialogs.ShortcutInfoDialog
import ch.rmy.android.http_shortcuts.exceptions.CanceledByUserException
import ch.rmy.android.http_shortcuts.extensions.addArguments
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.color
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.extensions.mapFor
import ch.rmy.android.http_shortcuts.extensions.mapIf
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.showSnackbar
import ch.rmy.android.http_shortcuts.extensions.showToast
import ch.rmy.android.http_shortcuts.extensions.startActivity
import ch.rmy.android.http_shortcuts.extensions.type
import ch.rmy.android.http_shortcuts.import_export.CurlExporter
import ch.rmy.android.http_shortcuts.import_export.ExportFormat
import ch.rmy.android.http_shortcuts.import_export.ExportUI
import ch.rmy.android.http_shortcuts.scheduling.ExecutionScheduler
import ch.rmy.android.http_shortcuts.utils.CategoryBackgroundType
import ch.rmy.android.http_shortcuts.utils.CategoryLayoutType
import ch.rmy.android.http_shortcuts.utils.DragOrderingHelper
import ch.rmy.android.http_shortcuts.utils.GridLayoutManager
import ch.rmy.android.http_shortcuts.utils.SelectionMode
import ch.rmy.android.http_shortcuts.variables.VariableManager
import ch.rmy.android.http_shortcuts.variables.VariableResolver
import ch.rmy.curlcommand.CurlConstructor

class ListFragment : BaseFragment<FragmentListBinding>() {

    val categoryId by lazy {
        args.getString(ARG_CATEGORY_ID) ?: ""
    }

    val layoutType: CategoryLayoutType by lazy {
        CategoryLayoutType.parse(args.getString(ARG_CATEGORY_LAYOUT_TYPE))
    }

    val selectionMode by lazy {
        args.getSerializable(ARG_SELECTION_MODE) as SelectionMode
    }

    private var isDraggingEnabled = false

    private var previousBackground: CategoryBackgroundType? = null

    private val exportUI by lazy {
        destroyer.own(ExportUI(requireActivity()))
    }

    private val viewModel: ShortcutListViewModel by bindViewModel()

    private lateinit var adapter: BaseShortcutAdapter

    private val wallpaper: Drawable? by lazy {
        try {
            WallpaperManager.getInstance(context).drawable
        } catch (e: SecurityException) {
            null
        }
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentListBinding.inflate(inflater, container, false)

    override fun setupViews() {
        initViews()
        initUserInputBindings()
        initViewModelBindings()

        viewModel.initialize(categoryId, selectionMode)
    }

    private fun initViews() {
        adapter = when (layoutType) {
            CategoryLayoutType.LINEAR_LIST -> ShortcutListAdapter()
            CategoryLayoutType.GRID -> ShortcutGridAdapter()
        }

        binding.shortcutList.layoutManager = when (layoutType) {
            CategoryLayoutType.GRID -> GridLayoutManager(requireContext())
            CategoryLayoutType.LINEAR_LIST -> LinearLayoutManager(context)
        }
        binding.shortcutList.adapter = adapter
        binding.shortcutList.setHasFixedSize(true)
    }

    private fun initUserInputBindings() {
        initDragOrdering()

        adapter.userEvents.observe(this) { event ->
            when (event) {
                is BaseShortcutAdapter.UserEvent.ShortcutClicked -> viewModel.onShortcutClicked(event.id)
                is BaseShortcutAdapter.UserEvent.ShortcutLongClicked -> viewModel.onShortcutLongClicked(event.id)
            }
        }
    }

    private fun initDragOrdering() {
        val dragOrderingHelper = DragOrderingHelper(allowHorizontalDragging = layoutType == CategoryLayoutType.GRID) { isDraggingEnabled }
        dragOrderingHelper.attachTo(binding.shortcutList)
        dragOrderingHelper.positionChangeSource
            .subscribe { (oldPosition, newPosition) ->
                viewModel.onShortcutMoved(oldPosition, newPosition)
            }
            .attachTo(destroyer)
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            binding.shortcutList.alpha = if (viewState.isInMovingMode) 0.7f else 1f
            isDraggingEnabled = viewState.isDraggingEnabled
            adapter.isLongClickingEnabled = viewState.isLongClickingEnabled
            updateBackground(viewState.background)
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    private fun updateBackground(background: CategoryBackgroundType) {
        if (background == previousBackground) {
            return
        }
        previousBackground = background
        binding.background.apply {
            when (background) {
                CategoryBackgroundType.WHITE -> {
                    setImageDrawable(null)
                    setBackgroundColor(color(requireContext(), R.color.activity_background))
                }
                CategoryBackgroundType.BLACK -> {
                    setImageDrawable(null)
                    setBackgroundColor(color(requireContext(), R.color.activity_background_dark))
                }
                CategoryBackgroundType.WALLPAPER -> {
                    wallpaper
                        ?.also {
                            setImageDrawable(it)
                        }
                        ?: run {
                            setImageDrawable(null)
                            setBackgroundColor(color(requireContext(), R.color.activity_background))
                        }
                }
            }
        }
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is ShortcutListEvent.ShowContextMenu -> showContextMenu(
                event.shortcutId,
                event.title,
                event.isPending,
                event.isMovable,
            )
            is ShortcutListEvent.ShowMoveOptionsDialog -> showMoveDialog(event.shortcutId)
            is ShortcutListEvent.ShowMoveToCategoryDialog -> showMoveToCategoryDialog(event.shortcutId, event.categoryOptions)
            is ShortcutListEvent.ShowShortcutInfoDialog -> showShortcutInfoDialog(event.shortcutId, event.shortcutName)
            else -> super.handleEvent(event)
        }
    }

    private fun showContextMenu(shortcutId: String, title: String, isPending: Boolean, isMovable: Boolean) {
        DialogBuilder(requireContext())
            .title(title)
            .item(R.string.action_place) {
                viewModel.onPlaceOnHomeScreenOptionSelected(shortcutId)
            }
            .item(R.string.action_run) {
                viewModel.onExecuteOptionSelected(shortcutId)
            }
            .mapIf(isPending) {
                item(R.string.action_cancel_pending) {
                    viewModel.onCancelPendingExecutionOptionSelected(shortcutId)
                }
            }
            .separator()
            .item(R.string.action_edit) {
                viewModel.onEditOptionSelected(shortcutId)
            }
            .mapIf(isMovable) {
                item(R.string.action_move) {
                    viewModel.onMoveOptionSelected(shortcutId)
                }
            }
            .item(R.string.action_duplicate) {
                viewModel.onDuplicateOptionSelected(shortcutId)
            }
            .item(R.string.action_delete) {
                viewModel.onDeleteOptionSelected(shortcutId)
            }
            .separator()
            .item(R.string.action_shortcut_information) {
                viewModel.onShowInfoOptionSelected(shortcutId)
            }
            .item(R.string.action_export) {
                viewModel.onExportOptionSelected(shortcutId)
            }
            .showIfPossible()
    }

    private fun showMoveDialog(shortcutId: String) {
        DialogBuilder(requireContext())
            .item(R.string.action_enable_moving) {
                viewModel.onMoveModeOptionSelected()
            }
            .item(R.string.action_move_to_category) {
                viewModel.onMoveToCategoryOptionSelected(shortcutId)
            }
            .showIfPossible()
    }

    private fun showMoveToCategoryDialog(shortcutId: String, categoryOptions: List<ShortcutListEvent.ShowMoveToCategoryDialog.CategoryOption>) {
        DialogBuilder(requireContext())
            .title(R.string.title_move_to_category)
            .mapFor(categoryOptions) { categoryOption ->
                item(name = categoryOption.name) {
                    viewModel.onMoveTargetCategorySelected(shortcutId, categoryOption.categoryId)
                }
            }
            .showIfPossible()
    }

    private fun showShortcutInfoDialog(shortcutId: String, shortcutName: String) {
        ShortcutInfoDialog(requireContext(), shortcutId, shortcutName)
            .show()
    }

    /*
    private fun duplicateShortcut(shortcut: Shortcut) {
        val name = shortcut.name
        val newName = String.format(getString(R.string.template_shortcut_name_copy), shortcut.name)
        val categoryId = categoryData.value?.id ?: return
        val newPosition = categoryData.value
            ?.shortcuts
            ?.indexOfFirst { it.id == shortcut.id }
            .takeIf { it != -1 }
            ?.let { it + 1 }
        viewModel.duplicateShortcut(shortcut.id, newName, newPosition, categoryId)
            .subscribe {
                showSnackbar(String.format(getString(R.string.shortcut_duplicated), name))
            }
            .attachTo(destroyer)
    }

    private fun showExportChoiceDialog(shortcutData: LiveData<Shortcut?>) {
        val shortcut = shortcutData.value ?: return
        if (shortcut.type.usesUrl) {
            DialogBuilder(requireContext())
                .title(R.string.title_export_shortcut_as)
                .item(R.string.action_export_as_curl) {
                    showCurlExportDialog(shortcutData)
                }
                .item(R.string.action_export_as_file) {
                    showFileExportDialog(shortcutData)
                }
                .showIfPossible()
        } else {
            showFileExportDialog(shortcutData)
        }
    }

    private fun showCurlExportDialog(shortcutData: LiveData<Shortcut?>) {
        CurlExporter.generateCommand(requireContext(), shortcutData.value ?: return)
            .subscribe(
                { command ->
                    CurlExportDialog(
                        requireContext(),
                        shortcutData.value?.name ?: return@subscribe,
                        CurlConstructor.toCurlCommandString(command)
                    )
                        .show()
                },
                { e ->
                    if (e !is CanceledByUserException) {
                        activity?.showToast(R.string.error_generic)
                        logException(e)
                    }
                }
            )
            .attachTo(destroyer)
    }

    private fun showFileExportDialog(shortcutData: LiveData<Shortcut?>) {
        val shortcut = shortcutData.value ?: return
        val shortcutId = shortcut.id
        val variableIds = getVariableIdsRequiredForExport(shortcut)
        exportUI.showExportOptions(format = ExportFormat.getPreferredFormat(requireContext()), shortcutId, variableIds) { intent ->
            viewModel.exportedShortcutId = shortcutId
            try {
                intent.startActivity(this, REQUEST_EXPORT)
            } catch (e: ActivityNotFoundException) {
                context?.showToast(R.string.error_not_supported)
            }
        }
    }

    private fun showDeleteDialog(shortcutData: LiveData<Shortcut?>) {
        DialogBuilder(requireContext())
            .message(R.string.confirm_delete_shortcut_message)
            .positive(R.string.dialog_delete) {
                deleteShortcut(shortcutData.value ?: return@positive)
            }
            .negative(R.string.dialog_cancel)
            .showIfPossible()
    }

    private fun deleteShortcut(shortcut: Shortcut) {
        showSnackbar(String.format(getString(R.string.shortcut_deleted), shortcut.name))
        tabHost?.removeShortcutFromHomeScreen(shortcut)
        viewModel.deleteShortcut(shortcut.id)
            .subscribe {
                ExecutionScheduler.schedule(requireContext())
            }
            .attachTo(destroyer)
    }
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        when (requestCode) {
            REQUEST_EDIT_SHORTCUT -> {
                viewModel.onShortcutEdited()
            }
            REQUEST_EXPORT -> {
                if (resultCode == RESULT_OK) {
                    viewModel.onExportDestinationSelected(intent?.data ?: return)
                }
            }
        }
    }

    /*
    private fun startExport(uri: Uri, shortcutId: String) {
        val shortcut = shortcuts.value?.find { it.id == shortcutId } ?: return
        val variableIds = getVariableIdsRequiredForExport(shortcut)

        exportUI.startExport(
            uri,
            format = ExportFormat.getPreferredFormat(requireContext()),
            shortcutId = shortcutId,
            variableIds = variableIds,
        )
    }

    private fun getVariableIdsRequiredForExport(shortcut: Shortcut) =
        // TODO: Recursively collect variables referenced by other variables
        RealmFactory.withRealmContext {
            VariableResolver.extractVariableIds(
                shortcut,
                variableLookup = VariableManager(getBase().findFirst()!!.variables),
            )
        }
     */

    override fun onPause() {
        super.onPause()
        viewModel.onPaused()
    }

    override fun onBackPressed() = consume {
        viewModel.onBackPressed()
    }

    companion object {

        fun create(categoryId: String, layoutType: CategoryLayoutType, selectionMode: SelectionMode): ListFragment =
            ListFragment().addArguments {
                putString(ARG_CATEGORY_ID, categoryId)
                putString(ARG_CATEGORY_LAYOUT_TYPE, layoutType.toString())
                putSerializable(ARG_SELECTION_MODE, selectionMode)
            }

        const val REQUEST_EDIT_SHORTCUT = 2
        private const val REQUEST_EXPORT = 3

        private const val ARG_CATEGORY_ID = "categoryId"
        private const val ARG_CATEGORY_LAYOUT_TYPE = "categoryLayoutType"
        private const val ARG_SELECTION_MODE = "selectionMode"
    }
}
