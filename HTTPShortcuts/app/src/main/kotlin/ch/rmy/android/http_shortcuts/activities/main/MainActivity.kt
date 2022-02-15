package ch.rmy.android.http_shortcuts.activities.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import androidx.viewpager.widget.ViewPager
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseActivity
import ch.rmy.android.http_shortcuts.activities.BaseFragment
import ch.rmy.android.http_shortcuts.activities.Entrypoint
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.activities.categories.CategoriesActivity
import ch.rmy.android.http_shortcuts.activities.editor.ShortcutEditorActivity
import ch.rmy.android.http_shortcuts.activities.misc.CurlImportActivity
import ch.rmy.android.http_shortcuts.activities.settings.ImportExportActivity
import ch.rmy.android.http_shortcuts.activities.settings.SettingsActivity
import ch.rmy.android.http_shortcuts.activities.widget.WidgetSettingsActivity
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.databinding.ActivityMainBinding
import ch.rmy.android.http_shortcuts.dialogs.ChangeLogDialog
import ch.rmy.android.http_shortcuts.dialogs.DialogBuilder
import ch.rmy.android.http_shortcuts.dialogs.NetworkRestrictionWarningDialog
import ch.rmy.android.http_shortcuts.extensions.applyTheme
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.bindViewModel
import ch.rmy.android.http_shortcuts.extensions.consume
import ch.rmy.android.http_shortcuts.extensions.observe
import ch.rmy.android.http_shortcuts.extensions.restartWithoutAnimation
import ch.rmy.android.http_shortcuts.extensions.titleView
import ch.rmy.android.http_shortcuts.extensions.visible
import ch.rmy.android.http_shortcuts.scheduling.ExecutionScheduler
import ch.rmy.android.http_shortcuts.utils.BaseIntentBuilder
import ch.rmy.android.http_shortcuts.utils.LauncherShortcut
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager
import ch.rmy.android.http_shortcuts.data.enums.SelectionMode
import ch.rmy.android.http_shortcuts.utils.text.Localizable
import ch.rmy.android.http_shortcuts.widget.WidgetManager
import ch.rmy.curlcommand.CurlCommand
import com.google.android.material.appbar.AppBarLayout

class MainActivity : BaseActivity(), Entrypoint {

    private val executionScheduler by lazy {
        ExecutionScheduler(applicationContext)
    }

    private val viewModel: MainViewModel by bindViewModel()

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: CategoryPagerAdapter

    private var menuItemSettings: MenuItem? = null
    private var menuItemImportExport: MenuItem? = null
    private var menuItemAbout: MenuItem? = null
    private var menuItemCategories: MenuItem? = null
    private var menuItemVariables: MenuItem? = null
    private var menuItemUnlock: MenuItem? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isRealmAvailable) {
            return
        }

        initViews()
        initUserInputBindings()
        initViewModelBindings()

        viewModel.initialize(
            selectionMode = SelectionMode.determineMode(intent.action),
            initialCategoryId = intent?.extras?.getString(EXTRA_CATEGORY_ID),
            widgetId = WidgetManager.getWidgetIdFromIntent(intent),
        )
    }

    private fun initViews() {
        binding = applyBinding(ActivityMainBinding.inflate(layoutInflater))

        setupViewPager()

        binding.tabs.applyTheme(themeHelper)
        binding.buttonCreateShortcut.applyTheme(themeHelper)
    }

    private fun setupViewPager() {
        adapter = CategoryPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = adapter
        binding.viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                viewModel.onSwitchedToCategory(position)
            }
        })
        binding.tabs.setupWithViewPager(binding.viewPager)
    }

    private fun initUserInputBindings() {
        binding.buttonCreateShortcut.setOnClickListener {
            viewModel.onCreateShortcutButtonClicked()
        }

        toolbar!!.titleView?.setOnClickListener {
            viewModel.onToolbarTitleClicked()
        }
    }

    private fun initViewModelBindings() {
        viewModel.viewState.observe(this) { viewState ->
            adapter.setCategories(viewState.categoryTabItems, viewState.selectionMode)
            binding.viewPager.currentItem = viewState.activeCategoryIndex
            toolbar?.title = viewState.toolbarTitleLocalizable.localize(context)
            binding.tabs.visible = viewState.isTabBarVisible
            binding.buttonCreateShortcut.visible = viewState.isCreateButtonVisible
            menuItemSettings?.isVisible = viewState.isRegularMenuButtonVisible
            menuItemImportExport?.isVisible = viewState.isRegularMenuButtonVisible
            menuItemAbout?.isVisible = viewState.isRegularMenuButtonVisible
            menuItemCategories?.isVisible = viewState.isRegularMenuButtonVisible
            menuItemVariables?.isVisible = viewState.isRegularMenuButtonVisible
            menuItemUnlock?.isVisible = viewState.isUnlockButtonVisible
            setToolbarScrolling(viewState.isToolbarScrollable)
        }
        viewModel.events.observe(this, ::handleEvent)
    }

    private fun setToolbarScrolling(isScrollable: Boolean) {
        (toolbar?.layoutParams as? AppBarLayout.LayoutParams?)?.scrollFlags =
            if (isScrollable) {
                AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS or
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
            } else 0
    }

    override fun handleEvent(event: ViewModelEvent) {
        when (event) {
            is MainEvent.ShowCreationDialog -> showCreationDialog()
            is MainEvent.ShowToolbarTitleChangeDialog -> showToolbarTitleChangeDialog(event.oldTitle)
            is MainEvent.ScheduleExecutions -> scheduleExecutions()
            is MainEvent.UpdateLauncherShortcuts -> updateLauncherShortcuts(event.shortcuts)
            is MainEvent.ShowChangeLogDialogIfNeeded -> showChangeLogDialogIfNeeded()
            is MainEvent.ShowUnlockDialog -> showUnlockDialog(event.message)
            is MainEvent.ShowShortcutPlacementDialog -> showShortcutPlacementDialog(event.shortcutId)
            else -> super.handleEvent(event)
        }
    }

    private fun scheduleExecutions() {
        executionScheduler.schedule()
            .subscribe()
            .attachTo(destroyer)
    }

    private fun showCreationDialog() {
        DialogBuilder(context)
            .title(R.string.title_create_new_shortcut_options_dialog)
            .item(R.string.button_create_new) {
                viewModel.onCreationDialogOptionSelected(ShortcutExecutionType.APP)
            }
            .item(R.string.button_curl_import) {
                viewModel.onCurlImportOptionSelected()
            }
            .separator()
            .item(
                nameRes = R.string.button_create_trigger_shortcut,
                descriptionRes = R.string.button_description_create_trigger_shortcut,
            ) {
                viewModel.onCreationDialogOptionSelected(ShortcutExecutionType.TRIGGER)
            }
            .item(
                nameRes = R.string.button_create_browser_shortcut,
                descriptionRes = R.string.button_description_create_browser_shortcut,
            ) {
                viewModel.onCreationDialogOptionSelected(ShortcutExecutionType.BROWSER)
            }
            .item(
                nameRes = R.string.button_create_scripting_shortcut,
                descriptionRes = R.string.button_description_create_scripting_shortcut,
            ) {
                viewModel.onCreationDialogOptionSelected(ShortcutExecutionType.SCRIPTING)
            }
            .positive(R.string.dialog_help) {
                viewModel.onCreationDialogHelpButtonClicked()
            }
            .showIfPossible()
    }

    private fun showToolbarTitleChangeDialog(oldTitle: String) {
        DialogBuilder(context)
            .title(R.string.title_set_title)
            .textInput(
                prefill = oldTitle,
                allowEmpty = true,
                maxLength = TITLE_MAX_LENGTH,
            ) { newTitle ->
                viewModel.onToolbarTitleChangeSubmitted(newTitle)
            }
            .showIfPossible()
    }

    private fun updateLauncherShortcuts(shortcuts: List<LauncherShortcut>) {
        LauncherShortcutManager.updateAppShortcuts(context, shortcuts)
    }

    private fun showChangeLogDialogIfNeeded() {
        ChangeLogDialog(context, whatsNew = true)
            .showIfNeeded()
            .ignoreElement()
            .andThen(
                NetworkRestrictionWarningDialog(context)
                    .showIfNeeded()
            )
            .subscribe({}, {})
            .attachTo(destroyer)
    }

    private fun showUnlockDialog(message: Localizable) {
        DialogBuilder(context)
            .title(R.string.dialog_title_unlock_app)
            .message(message)
            .positive(R.string.button_unlock_app)
            .textInput(inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) { input ->
                viewModel.onUnlockDialogSubmitted(input)
            }
            .negative(R.string.dialog_cancel)
            .showIfPossible()
    }

    private fun showShortcutPlacementDialog(shortcutId: String) {
        DialogBuilder(context)
            .title(R.string.title_select_placement_method)
            .message(R.string.description_select_placement_method)
            .positive(R.string.label_placement_method_default) {
                viewModel.onShortcutPlacementConfirmed(shortcutId, useLegacyMethod = false)
            }
            .negative(R.string.label_placement_method_legacy) {
                viewModel.onShortcutPlacementConfirmed(shortcutId, useLegacyMethod = true)
            }
            .showIfPossible()
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode != Activity.RESULT_OK || intent == null) {
            when (requestCode) {
                REQUEST_WIDGET_SETTINGS -> {
                    finish()
                }
            }
            return
        }
        when (requestCode) {
            REQUEST_CREATE_SHORTCUT_FROM_CURL -> {
                val curlCommand = intent.getSerializableExtra(CurlImportActivity.EXTRA_CURL_COMMAND) as CurlCommand
                viewModel.onCurlCommandSubmitted(curlCommand)
            }
            REQUEST_CREATE_SHORTCUT -> {
                viewModel.onShortcutCreated(intent.getStringExtra(ShortcutEditorActivity.RESULT_SHORTCUT_ID)!!)
            }
            REQUEST_SETTINGS -> {
                if (intent.getBooleanExtra(SettingsActivity.EXTRA_THEME_CHANGED, false)) {
                    recreate()
                    openSettings()
                    overridePendingTransition(0, 0)
                } else if (intent.getBooleanExtra(SettingsActivity.EXTRA_APP_LOCKED, false)) {
                    viewModel.onAppLocked()
                }
            }
            REQUEST_IMPORT_EXPORT -> {
                if (intent.getBooleanExtra(ImportExportActivity.EXTRA_CATEGORIES_CHANGED, false)) {
                    restartWithoutAnimation()
                }
            }
            REQUEST_CATEGORIES -> {
                if (intent.getBooleanExtra(CategoriesActivity.EXTRA_CATEGORIES_CHANGED, false)) {
                    restartWithoutAnimation()
                }
            }
            REQUEST_WIDGET_SETTINGS -> {
                viewModel.onWidgetSettingsSubmitted(
                    shortcutId = WidgetSettingsActivity.getShortcutId(intent) ?: return,
                    showLabel = WidgetSettingsActivity.shouldShowLabel(intent),
                    labelColor = WidgetSettingsActivity.getLabelColor(intent),
                )
            }
        }
    }

    private fun openSettings() {
        SettingsActivity.IntentBuilder()
            .startActivity(this, REQUEST_SETTINGS)
    }

    override val navigateUpIcon = 0

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuItemSettings = menu.findItem(R.id.action_settings)
        menuItemImportExport = menu.findItem(R.id.action_import_export)
        menuItemAbout = menu.findItem(R.id.action_about)
        menuItemCategories = menu.findItem(R.id.action_categories)
        menuItemVariables = menu.findItem(R.id.action_variables)
        menuItemUnlock = menu.findItem(R.id.action_unlock)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_settings -> consume { viewModel.onSettingsButtonClicked() }
        R.id.action_import_export -> consume { viewModel.onImportExportButtonClicked() }
        R.id.action_about -> consume { viewModel.onAboutButtonClicked() }
        R.id.action_categories -> consume { viewModel.onCategoriesButtonClicked() }
        R.id.action_variables -> consume { viewModel.onVariablesButtonClicked() }
        R.id.action_unlock -> consume { viewModel.onUnlockButtonClicked() }
        else -> super.onOptionsItemSelected(item)
    }

    /* TODO
    private fun placeShortcutOnHomeScreen(shortcut: Shortcut) {
        if (LauncherShortcutManager.supportsPinning(context)) {
            LauncherShortcutManager.pinShortcut(context, shortcut)
        } else {
            sendBroadcast(IntentUtil.getLegacyShortcutPlacementIntent(context, shortcut, true))
            showSnackbar(String.format(getString(R.string.shortcut_placed), shortcut.name))
        }
    }

    private fun removeShortcutFromHomeScreen(shortcut: Shortcut) {
        sendBroadcast(IntentUtil.getLegacyShortcutPlacementIntent(context, shortcut, false))
    }
     */

    override fun onBackPressed() {
        supportFragmentManager.fragments
            .filter { it.isResumed }
            .filterIsInstance(BaseFragment::class.java)
            .forEach { fragment ->
                if (fragment.onBackPressed()) {
                    return
                }
            }
        super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        setTabLongPressListener()
    }

    private fun setTabLongPressListener() {
        (0..binding.tabs.tabCount)
            .mapNotNull { binding.tabs.getTabAt(it)?.view }
            .forEach {
                it.setOnLongClickListener {
                    consume {
                        viewModel.onTabLongClicked()
                    }
                }
            }
    }

    class IntentBuilder : BaseIntentBuilder(MainActivity::class.java) {
        init {
            intent.action = Intent.ACTION_VIEW
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        fun categoryId(categoryId: String) = also {
            intent.putExtra(EXTRA_CATEGORY_ID, categoryId)
        }
    }

    companion object {

        const val EXTRA_SELECTION_ID = "ch.rmy.android.http_shortcuts.shortcut_id"
        const val EXTRA_SELECTION_NAME = "ch.rmy.android.http_shortcuts.shortcut_name"
        private const val EXTRA_CATEGORY_ID = "ch.rmy.android.http_shortcuts.category_id"

        const val REQUEST_CREATE_SHORTCUT = 1
        const val REQUEST_CREATE_SHORTCUT_FROM_CURL = 2
        const val REQUEST_SETTINGS = 3
        const val REQUEST_CATEGORIES = 4
        const val REQUEST_WIDGET_SETTINGS = 5
        const val REQUEST_IMPORT_EXPORT = 6

        private const val TITLE_MAX_LENGTH = 50
    }
}
