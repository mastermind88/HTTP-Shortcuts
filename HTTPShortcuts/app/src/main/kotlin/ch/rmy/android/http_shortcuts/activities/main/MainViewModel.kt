package ch.rmy.android.http_shortcuts.activities.main

import android.app.Application
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.activities.BaseViewModel
import ch.rmy.android.http_shortcuts.activities.categories.CategoriesActivity
import ch.rmy.android.http_shortcuts.activities.editor.ShortcutEditorActivity
import ch.rmy.android.http_shortcuts.activities.settings.AboutActivity
import ch.rmy.android.http_shortcuts.activities.settings.ImportExportActivity
import ch.rmy.android.http_shortcuts.activities.settings.SettingsActivity
import ch.rmy.android.http_shortcuts.activities.variables.VariablesActivity
import ch.rmy.android.http_shortcuts.data.domains.app.AppRepository
import ch.rmy.android.http_shortcuts.data.domains.categories.CategoryRepository
import ch.rmy.android.http_shortcuts.data.domains.shortcuts.ShortcutRepository
import ch.rmy.android.http_shortcuts.data.enums.ShortcutExecutionType
import ch.rmy.android.http_shortcuts.extensions.attachTo
import ch.rmy.android.http_shortcuts.extensions.takeUnlessEmpty
import ch.rmy.android.http_shortcuts.extensions.toLocalizable
import ch.rmy.android.http_shortcuts.utils.ExternalURLs
import ch.rmy.android.http_shortcuts.utils.SelectionMode
import ch.rmy.android.http_shortcuts.utils.text.StringResLocalizable

class MainViewModel(application: Application) : BaseViewModel<MainViewState>(application) {

    private val shortcutRepository: ShortcutRepository = ShortcutRepository()
    private val categoryRepository: CategoryRepository = CategoryRepository()
    private val appRepository: AppRepository = AppRepository()

    private var initialized = false
    private lateinit var selectionMode: SelectionMode
    private var initialCategoryId: String? = null
    private var widgetId: Int? = null

    override fun initViewState() = MainViewState()

    fun initialize(selectionMode: SelectionMode, initialCategoryId: String?, widgetId: Int?) {
        if (initialized) {
            return
        }
        this.selectionMode = selectionMode
        this.initialCategoryId = initialCategoryId
        this.widgetId = widgetId
        initialized = true
        initialize()
    }

    override fun onInitialized() {
        observeToolbarTitle()
        observeAppLock()
    }

    private fun observeToolbarTitle() {
        appRepository.getObservableToolbarTitle()
            .subscribe { toolbarTitle ->
                updateViewState {
                    copy(toolbarTitle = toolbarTitle.takeUnlessEmpty()?.toLocalizable() ?: StringResLocalizable(R.string.app_name))
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

    var hasMovedToInitialCategory = false

    fun removeAppLock(password: String): Single<Boolean> =
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

    fun moveShortcut(shortcutId: String, targetPosition: Int? = null, targetCategoryId: String? = null) =
        shortcutRepository.moveShortcut(shortcutId, targetPosition, targetCategoryId)
            .observeOn(AndroidSchedulers.mainThread())

    fun getToolbarTitle() =
        appRepository.getToolbarTitle()
            .blockingGet() // TODO

    fun setToolbarTitle(title: String) =
        appRepository.setToolbarTitle(title.trim())
            .observeOn(AndroidSchedulers.mainThread())
    */

    /*
    private val showHiddenCategories: Boolean by lazy {
        selectionMode != SelectionMode.NORMAL
    }
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
            // TODO
            // showToolbarTitleChangeDialog()
        }
    }

    fun onCreationDialogOptionSelected(executionType: ShortcutExecutionType) {
        val categoryId = "123"// TODO: adapter.getItem(binding.viewPager.currentItem).categoryId
        openActivity(
            ShortcutEditorActivity.IntentBuilder()
                .categoryId(categoryId)
                .executionType(executionType),
            requestCode = MainActivity.REQUEST_CREATE_SHORTCUT,
        )
    }

    fun onCreationDialogHelpButtonClicked() {
        openURL(ExternalURLs.SHORTCUTS_DOCUMENTATION)
    }
}
