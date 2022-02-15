package ch.rmy.android.http_shortcuts.import_export

import android.content.Context
import android.net.Uri
import ch.rmy.android.http_shortcuts.R
import ch.rmy.android.http_shortcuts.data.RealmFactory
import ch.rmy.android.http_shortcuts.data.domains.getBase
import ch.rmy.android.http_shortcuts.data.migration.ImportMigrator
import ch.rmy.android.http_shortcuts.data.migration.ImportVersionMismatchException
import ch.rmy.android.http_shortcuts.data.models.Base
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.extensions.isWebUrl
import ch.rmy.android.http_shortcuts.extensions.logInfo
import ch.rmy.android.http_shortcuts.utils.FileUtil
import ch.rmy.android.http_shortcuts.utils.GsonUtil
import ch.rmy.android.http_shortcuts.utils.IconUtil
import ch.rmy.android.http_shortcuts.utils.NoCloseInputStream
import ch.rmy.android.http_shortcuts.utils.RxUtils
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.realm.Realm
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URISyntaxException
import java.net.URL
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

class Importer(private val context: Context) {

    fun importFromUri(uri: Uri, importMode: ImportMode): Single<ImportStatus> =
        RxUtils
            .single {
                val cacheFile = FileUtil.createCacheFile(context, IMPORT_TEMP_FILE)
                getStream(context, uri).use { inStream ->
                    FileUtil.getOutputStream(context, cacheFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }

                try {
                    context.contentResolver.openInputStream(cacheFile)!!.use { stream ->
                        importFromZIP(stream, importMode)
                    }
                } catch (e: ZipException) {
                    context.contentResolver.openInputStream(cacheFile)!!.use { stream ->
                        importFromJSON(stream, importMode)
                    }
                }
            }
            .onErrorResumeNext { error ->
                Single.error(handleError(error))
            }
            .subscribeOn(Schedulers.io())

    private fun importFromZIP(inputStream: InputStream, importMode: ImportMode): ImportStatus {
        var importStatus: ImportStatus? = null
        ZipInputStream(inputStream).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                when {
                    entry.name == Exporter.JSON_FILE -> {
                        importStatus = importFromJSON(NoCloseInputStream(stream), importMode)
                    }
                    IconUtil.isCustomIconName(entry.name) -> {
                        NoCloseInputStream(stream).copyTo(FileOutputStream(File(context.filesDir, entry.name)))
                    }
                    else -> {
                        stream.closeEntry()
                    }
                }
            }
        }
        return importStatus ?: throw ZipException("Invalid file")
    }

    fun importFromJSON(inputStream: InputStream, importMode: ImportMode): ImportStatus {
        val importData = BufferedReader(InputStreamReader(inputStream)).use { reader ->
            JsonParser.parseReader(reader)
        }
        logInfo("Importing from v${importData.asJsonObject.get("version") ?: "?"}: ${importData.asJsonObject.keySet()}")
        val migratedImportData = ImportMigrator.migrate(importData)
        val newBase = GsonUtil.importData(migratedImportData)
        try {
            newBase.validate()
        } catch (e: IllegalArgumentException) {
            throw ImportException(e.message!!)
        }
        importBase(newBase, importMode)
        return ImportStatus(
            importedShortcuts = newBase.shortcuts.size,
            needsRussianWarning = newBase.shortcuts.any { it.url.contains(".beeline.ru") }
        )
    }

    private fun getStream(context: Context, uri: Uri): InputStream =
        if (uri.isWebUrl) {
            URL(uri.toString()).openStream()
        } else {
            context.contentResolver.openInputStream(uri)
                ?: throw IOException("Failed to open input stream")
        }

    private fun importBase(base: Base, importMode: ImportMode) {
        RealmFactory.withRealmContext {
            realmInstance.executeTransaction {
                val oldBase = getBase().findFirst()!!
                if (base.title != null) {
                    oldBase.title = base.title
                }
                if (!base.globalCode.isNullOrEmpty() && oldBase.globalCode.isNullOrEmpty()) {
                    oldBase.globalCode = base.globalCode
                }
                when (importMode) {
                    ImportMode.MERGE -> {
                        if (oldBase.categories.singleOrNull()?.shortcuts?.isEmpty() == true) {
                            oldBase.categories.clear()
                        }

                        base.categories.forEach { category ->
                            importCategory(realm, oldBase, category)
                        }

                        val persistedVariables = realm.copyToRealmOrUpdate(base.variables)
                        oldBase.variables.removeAll(persistedVariables)
                        oldBase.variables.addAll(persistedVariables)
                    }
                    ImportMode.REPLACE -> {
                        oldBase.categories.clear()
                        oldBase.categories.addAll(realm.copyToRealmOrUpdate(base.categories))

                        oldBase.variables.clear()
                        oldBase.variables.addAll(realm.copyToRealmOrUpdate(base.variables))
                    }
                }
            }
        }
    }

    private fun importCategory(realm: Realm, base: Base, category: Category) {
        val oldCategory = base.categories.find { it.id == category.id }
        if (oldCategory == null) {
            base.categories.add(realm.copyToRealmOrUpdate(category))
        } else {
            oldCategory.name = category.name
            oldCategory.background = category.background
            oldCategory.hidden = category.hidden
            oldCategory.layoutType = category.layoutType
            category.shortcuts.forEach { shortcut ->
                importShortcut(realm, oldCategory, shortcut)
            }
        }
    }

    private fun importShortcut(realm: Realm, category: Category, shortcut: Shortcut) {
        val oldShortcut = category.shortcuts.find { it.id == shortcut.id }
        if (oldShortcut == null) {
            category.shortcuts.add(realm.copyToRealmOrUpdate(shortcut))
        } else {
            realm.copyToRealmOrUpdate(shortcut)
        }
    }

    private fun handleError(error: Throwable): Throwable =
        getHumanReadableErrorMessage(error)
            ?.let { ImportException(it) }
            ?: error

    private fun getHumanReadableErrorMessage(e: Throwable, recursive: Boolean = true): String? = with(context) {
        when (e) {
            is JsonParseException, is JsonSyntaxException -> {
                getString(R.string.import_failure_reason_invalid_json)
            }
            is ImportVersionMismatchException -> {
                getString(R.string.import_failure_reason_data_version_mismatch)
            }
            is URISyntaxException,
            is IllegalArgumentException,
            is IllegalStateException,
            is IOException,
            -> {
                e.message
            }
            else ->
                e.cause
                    ?.takeIf { recursive }
                    ?.let {
                        getHumanReadableErrorMessage(it, recursive = false)
                    }
        }
    }

    data class ImportStatus(val importedShortcuts: Int, val needsRussianWarning: Boolean)

    enum class ImportMode {
        MERGE,
        REPLACE,
    }

    companion object {
        private const val IMPORT_TEMP_FILE = "import"
    }
}
