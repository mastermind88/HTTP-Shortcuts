package ch.rmy.android.http_shortcuts.activities.main

import android.app.Activity
import android.app.Application
import android.content.Intent
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.ViewModelEvent
import ch.rmy.android.http_shortcuts.activities.categories.CategoriesActivity
import ch.rmy.android.http_shortcuts.activities.editor.ShortcutEditorActivity
import ch.rmy.android.http_shortcuts.activities.misc.CurlImportActivity
import ch.rmy.android.http_shortcuts.activities.settings.AboutActivity
import ch.rmy.android.http_shortcuts.activities.settings.ImportExportActivity
import ch.rmy.android.http_shortcuts.activities.settings.SettingsActivity
import ch.rmy.android.http_shortcuts.activities.variables.VariablesActivity
import ch.rmy.android.http_shortcuts.activities.widget.WidgetSettingsActivity
import ch.rmy.android.http_shortcuts.data.domains.app.AppRepository
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryRepository
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.context
import ch.rmy.android.http_shortcuts.extensions.mapIf
import ch.rmy.android.http_shortcuts.extensions.toLauncherShortcut
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import ch.rmy.android.http_shortcuts.utils.IntentUtil
import ch.rmy.android.http_shortcuts.utils.LauncherShortcutManager
import ch.rmy.android.http_shortcuts.utils.SelectionMode
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable
import ch.rmy.android.http_shortcuts.widget.WidgetManager
import ch.rmy.curlcommand.CurlCommand
import io.reactivex.Single
import org.mindrot.jbcrypt.BCrypt

class MainViewModel(application: Application) : BaseViewModel<MainViewState>(application) {

    private val categoryRepository: CategoryRepository = CategoryRepository()
    private val appRepository: AppRepository = AppRepository()
    private val launcherShortcutMapper: LauncherShortcutMapper = LauncherShortcutMapper()

    private var initialized = false
    private var initialCategoryId: String? = null
    private var widgetId: Int? = null
    private lateinit var categories: List<Category>
    private lateinit var activeCategoryId: String

    private lateinit var selectionMode: SelectionMode

    fun initialize(selectionMode: SelectionMode, initialCategoryId: String?, widgetId: Int?) {
        if (initialized) {
            return
        }
        this.selectionMode = selectionMode
        this.initialCategoryId = initialCategoryId
        this.widgetId = widgetId
        initialized = true

        categoryRepository.getCategories()
            .subscribe { categories ->
                this.categories = categories
                initialize()
            }
            .attachTo(destroyer)
    }

    override fun initViewState() = MainViewState(
        selectionMode = selectionMode,
        categoryTabItems = getCategoryTabItems(),
        activeCategoryId = initialCategoryId ?: "",
    )

    private fun getCategoryTabItems() =
        categories
            .mapIf(selectionMode == SelectionMode.NORMAL) {
                filter { !it.hidden }
            }
            .map { category ->
                CategoryTabItem(
                    categoryId = category.id,
                    name = category.name,
                    layoutType = category.categoryLayoutType,
                )
            }

    override fun onInitialized() {
        observeToolbarTitle()
        observeAppLock()

        emitEvent(MainEvent.ScheduleExecutions)
        emitEvent(MainEvent.UpdateLauncherShortcuts(launcherShortcutMapper(categories)))

        if (selectionMode === SelectionMode.NORMAL) {
            emitEvent(MainEvent.ShowChangeLogDialogIfNeeded)
        } else {
            if (selectionMode == SelectionMode.HOME_SCREEN_WIDGET_PLACEMENT && widgetId != null) {
                emitEvent(ViewModelEvent.SetResult(Activity.RESULT_CANCELED, WidgetManager.getIntent(widgetId!!)))
            }
            if (
                selectionMode == SelectionMode.HOME_SCREEN_WIDGET_PLACEMENT ||
                selectionMode == SelectionMode.HOME_SCREEN_SHORTCUT_PLACEMENT

            ) {
                showToast(R.string.instructions_select_shortcut_for_home_screen, long = true)
            }
        }
    }

    private fun observeToolbarTitle() {
        appRepository.getObservableToolbarTitle()
            .subscribe { toolbarTitle ->
                updateViewState {
                    copy(toolbarTitle = toolbarTitle)
                }
            }
            .attachTo(destroyer)
    }

    private fun observeAppLock() {
        appRepository.getObservableLock()
            .subscribe { optionalLock ->
                updateViewState {
                    copy(isLocked = optionalLock.value != null)
                }
            }
            .attachTo(destroyer)
    }
    /*

    fun moveShortcut(shortcutId: String, targetPosition: Int? = null, targetCategoryId: String? = null) =
        shortcutRepository.moveShortcut(shortcutId, targetPosition, targetCategoryId)
            .observeOn(AndroidSchedulers.mainThread())
    */

    fun onSettingsButtonClicked() {
        openActivity(SettingsActivity.IntentBuilder(), MainActivity.REQUEST_SETTINGS)
    }

    fun onImportExportButtonClicked() {
        openActivity(ImportExportActivity.IntentBuilder(), MainActivity.REQUEST_IMPORT_EXPORT)
    }

    fun onAboutButtonClicked() {
        openActivity(AboutActivity.IntentBuilder())
    }

    fun onCategoriesButtonClicked() {
        openCategoriesEditor()
    }

    private fun openCategoriesEditor() {
        openActivity(CategoriesActivity.IntentBuilder(), MainActivity.REQUEST_CATEGORIES)
    }

    fun onVariablesButtonClicked() {
        openActivity(VariablesActivity.IntentBuilder())
    }

    fun onTabLongClicked() {
        if (selectionMode == SelectionMode.NORMAL && !currentViewState.isLocked) {
            openCategoriesEditor()
        }
    }

    fun onToolbarTitleClicked() {
        if (selectionMode == SelectionMode.NORMAL && !currentViewState.isLocked) {
            emitEvent(MainEvent.ShowToolbarTitleChangeDialog(currentViewState.toolbarTitle))
            // TODO
            // showToolbarTitleChangeDialog()
        }
    }

    fun onCreationDialogOptionSelected(executionType: ShortcutExecutionType) {
        openActivity(
            ShortcutEditorActivity.IntentBuilder()
                .categoryId(activeCategoryId)
                .executionType(executionType),
            requestCode = MainActivity.REQUEST_CREATE_SHORTCUT,
        )
    }

    fun onCreationDialogHelpButtonClicked() {
        openURL(ExternalURLs.SHORTCUTS_DOCUMENTATION)
    }

    fun onCreateShortcutButtonClicked() {
        emitEvent(MainEvent.ShowCreationDialog)
    }

    fun onToolbarTitleChangeSubmitted(newTitle: String) {
        performOperation(appRepository.setToolbarTitle(newTitle)) {
            showSnackbar(R.string.message_title_changed)
        }
    }

    fun onSwitchedToCategory(categoryId: String) {
        activeCategoryId = categoryId
    }

    fun onUnlockButtonClicked() {
        emitEvent(MainEvent.ShowUnlockDialog(StringResLocalizable(R.string.dialog_text_unlock_app)))
    }

    fun onAppLocked() {
        showSnackbar(R.string.message_app_locked)
    }

    fun onUnlockDialogSubmitted(password: String) {
        performOperation(
            appRepository.getLock()
                .flatMap { optionalLock ->
                    val passwordHash = optionalLock.value?.passwordHash
                    if (passwordHash != null && BCrypt.checkpw(password, passwordHash)) {
                        appRepository.removeLock()
                            .toSingleDefault(true)
                    } else {
                        Single.just(passwordHash == null)
                    }
                }
                .doOnSuccess { unlocked ->
                    if (unlocked) {
                        showSnackbar(R.string.message_app_unlocked)
                    } else {
                        emitEvent(MainEvent.ShowUnlockDialog(StringResLocalizable(R.string.dialog_text_unlock_app_retry)))
                    }
                }
                .ignoreElement()
        )
    }

    fun onCurlImportOptionSelected() {
        openActivity(
            CurlImportActivity.IntentBuilder(),
            requestCode = MainActivity.REQUEST_CREATE_SHORTCUT_FROM_CURL,
        )
    }

    fun onShortcutCreated(shortcutId: String) {
        categoryRepository.getCategories()
            .subscribe { categories ->
                this.categories = categories
                emitEvent(MainEvent.UpdateLauncherShortcuts(launcherShortcutMapper(categories)))
                selectShortcut(shortcutId)
            }
            .attachTo(destroyer)
    }

    private fun selectShortcut(shortcutId: String) {
        when (selectionMode) {
            SelectionMode.HOME_SCREEN_SHORTCUT_PLACEMENT -> returnForHomeScreenShortcutPlacement(shortcutId)
            SelectionMode.HOME_SCREEN_WIDGET_PLACEMENT -> openWidgetSettings(shortcutId)
            SelectionMode.PLUGIN -> returnForPlugin(shortcutId)
            SelectionMode.NORMAL -> Unit
        }
    }

    private fun openWidgetSettings(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        openActivity(
            WidgetSettingsActivity.IntentBuilder()
                .shortcut(shortcut.toLauncherShortcut()),
            requestCode = MainActivity.REQUEST_WIDGET_SETTINGS,
        )
    }

    private fun returnForHomeScreenShortcutPlacement(shortcutId: String) {
        if (LauncherShortcutManager.supportsPinning(context)) {
            emitEvent(MainEvent.ShowShortcutPlacementDialog(shortcutId))
        } else {
            placeShortcutOnHomeScreenAndFinish(shortcutId)
        }
    }

    private fun placeShortcutOnHomeScreenAndFinish(shortcutId: String) {
        placeOnHomeScreenWithLegacyAndFinish(shortcutId)
    }

    private fun returnForHomeScreenWidgetPlacement(shortcutId: String, showLabel: Boolean, labelColor: String?) {
        val widgetId = widgetId ?: return
        val widgetManager = WidgetManager()
        widgetManager
            .createWidget(widgetId, shortcutId, showLabel, labelColor)
            .andThen(widgetManager.updateWidgets(context, shortcutId))
            .subscribe {
                finishWithOkResult(
                    WidgetManager.getIntent(widgetId)
                )
            }
            .attachTo(destroyer)
    }

    fun onShortcutPlacementConfirmed(shortcutId: String, useLegacyMethod: Boolean) {
        if (useLegacyMethod) {
            placeOnHomeScreenWithLegacyAndFinish(shortcutId)
        } else {
            placeOnHomeScreenAndFinish(shortcutId)
        }
    }

    private fun placeOnHomeScreenAndFinish(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        finishWithOkResult(LauncherShortcutManager.createShortcutPinIntent(context, shortcut.toLauncherShortcut()))
    }

    private fun placeOnHomeScreenWithLegacyAndFinish(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        finishWithOkResult(IntentUtil.getLegacyShortcutPlacementIntent(context, shortcut.toLauncherShortcut(), install = true))
    }

    private fun returnForPlugin(shortcutId: String) {
        val shortcut = getShortcutById(shortcutId) ?: return
        finishWithOkResult(
            Intent()
                .putExtra(MainActivity.EXTRA_SELECTION_ID, shortcut.id)
                .putExtra(MainActivity.EXTRA_SELECTION_NAME, shortcut.name)
        )
    }

    fun onCurlCommandSubmitted(curlCommand: CurlCommand) {
        openActivity(
            ShortcutEditorActivity.IntentBuilder()
                .categoryId(currentViewState.activeCategoryId)
                .curlCommand(curlCommand),
            requestCode = MainActivity.REQUEST_CREATE_SHORTCUT,
        )
    }

    private fun getShortcutById(shortcutId: String): Shortcut? {
        for (category in categories) {
            for (shortcut in category.shortcuts) {
                if (shortcut.id == shortcutId) {
                    return shortcut
                }
            }
        }
        return null
    }

    fun onWidgetSettingsSubmitted(shortcutId: String, showLabel: Boolean, labelColor: String?) {
        returnForHomeScreenWidgetPlacement(shortcutId, showLabel, labelColor)
    }

    private fun finishWithOkResult(intent: Intent) {
        finish(
            result = Activity.RESULT_OK,
            intent = intent,
        )
    }

}
