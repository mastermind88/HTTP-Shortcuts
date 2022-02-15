package ch.rmy.android.http_shortcuts.import_export

import android.content.Context
import android.net.Uri
import ch.rmy.android.http_shortcuts.data.RealmFactory
import ch.rmy.android.http_shortcuts.data.domains.getBase
import ch.rmy.android.http_shortcuts.data.models.Base
import ch.rmy.android.http_shortcuts.data.models.Category
import ch.rmy.android.http_shortcuts.data.models.ClientCertParams
import ch.rmy.android.http_shortcuts.data.models.Header
import ch.rmy.android.http_shortcuts.data.models.Option
import ch.rmy.android.http_shortcuts.data.models.Parameter
import ch.rmy.android.http_shortcuts.data.models.ResponseHandling
import ch.rmy.android.http_shortcuts.data.models.Shortcut
import ch.rmy.android.http_shortcuts.data.models.Variable
import ch.rmy.android.http_shortcuts.extensions.detachFromRealm
import ch.rmy.android.http_shortcuts.extensions.logException
import ch.rmy.android.http_shortcuts.extensions.mapFor
import ch.rmy.android.http_shortcuts.extensions.mapIf
import ch.rmy.android.http_shortcuts.extensions.safeRemoveIf
import ch.rmy.android.http_shortcuts.icons.ShortcutIcon
import ch.rmy.android.http_shortcuts.utils.FileUtil
import ch.rmy.android.http_shortcuts.utils.GsonUtil
import ch.rmy.android.http_shortcuts.utils.IconUtil
import ch.rmy.android.http_shortcuts.utils.RxUtils
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Exporter(private val context: Context) {

    fun exportToUri(
        uri: Uri,
        format: ExportFormat = ExportFormat.ZIP,
        shortcutId: String? = null,
        variableIds: Collection<String>? = null,
        excludeDefaults: Boolean = false,
    ): Single<ExportStatus> =
        RxUtils
            .single {
                val base = getDetachedBase(shortcutId, variableIds)

                when (format) {
                    ExportFormat.ZIP -> {
                        ZipOutputStream(FileUtil.getOutputStream(context, uri)).use { out ->

                            out.putNextEntry(ZipEntry(JSON_FILE))
                            val writer = out.bufferedWriter()
                            val result = export(writer, base, excludeDefaults)
                            writer.flush()
                            out.closeEntry()

                            getFilesToExport(context, base).forEach { file ->
                                out.putNextEntry(ZipEntry(file.name))
                                FileInputStream(file).copyTo(out)
                                writer.flush()
                                out.closeEntry()
                            }
                            result
                        }
                    }
                    ExportFormat.LEGACY_JSON -> {
                        FileUtil.getWriter(context, uri).use { writer ->
                            export(writer, base, excludeDefaults)
                        }
                    }
                }
            }
            .subscribeOn(Schedulers.io())

    private fun export(
        writer: Appendable,
        base: Base,
        excludeDefaults: Boolean = false,
    ): ExportStatus {
        exportData(base, writer, excludeDefaults)
        return ExportStatus(exportedShortcuts = base.shortcuts.size)
    }

    private fun getDetachedBase(shortcutId: String?, variableIds: Collection<String>?): Base =
        RealmFactory.withRealmContext {
            getBase().findFirst()!!.detachFromRealm()
        }
            .also { base ->
                if (shortcutId != null) {
                    base.title = null
                    base.categories.safeRemoveIf {
                        it.shortcuts.none { it.id == shortcutId }
                    }
                    base.categories.firstOrNull()?.shortcuts?.safeRemoveIf { it.id != shortcutId }
                }
                if (variableIds != null) {
                    base.variables.safeRemoveIf { !variableIds.contains(it.id) }
                }
            }

    private fun exportData(base: Base, writer: Appendable, excludeDefaults: Boolean = false) {
        try {
            val serializer = ModelSerializer()
            GsonUtil.gson
                .newBuilder()
                .setPrettyPrinting()
                .mapIf(excludeDefaults) {
                    mapFor(MODEL_CLASSES) { clazz ->
                        registerTypeAdapter(clazz, serializer)
                    }
                }
                .create()
                .toJson(base, writer)
        } catch (e: Throwable) {
            if (e !is NoClassDefFoundError) {
                logException(e)
            }
            GsonUtil.gson
                .newBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(base, writer)
        }
    }

    private fun getFilesToExport(context: Context, base: Base): List<File> =
        getShortcutIconFiles(context, base)
            .plus(getClientCertFiles(context, base))
            .filter { it.exists() }
            .toList()

    private fun getShortcutIconFiles(context: Context, base: Base) =
        base.shortcuts.asSequence()
            .map { it.icon }
            .filterIsInstance(ShortcutIcon.CustomIcon::class.java)
            .plus(
                getReferencedIconNames(base)
                    .map { fileName ->
                        ShortcutIcon.CustomIcon(fileName)
                    }
            )
            .distinct()
            .mapNotNull {
                it.getFile(context)
            }

    private fun getReferencedIconNames(base: Base): Set<String> =
        IconUtil.extractCustomIconNames(base.globalCode ?: "")
            .plus(base.shortcuts.flatMap(::getReferencedIconNames))

    private fun getReferencedIconNames(shortcut: Shortcut): Set<String> =
        IconUtil.extractCustomIconNames(shortcut.codeOnSuccess)
            .plus(IconUtil.extractCustomIconNames(shortcut.codeOnFailure))
            .plus(IconUtil.extractCustomIconNames(shortcut.codeOnPrepare))

    private fun getClientCertFiles(context: Context, base: Base) =
        base.shortcuts.asSequence()
            .mapNotNull { (it.clientCertParams as? ClientCertParams.File) }
            .map { it.getFile(context) }

    data class ExportStatus(val exportedShortcuts: Int)

    companion object {
        const val JSON_FILE = "shortcuts.json"

        private val MODEL_CLASSES = setOf(
            Base::class.java,
            Header::class.java,
            Parameter::class.java,
            Shortcut::class.java,
            Option::class.java,
            Variable::class.java,
            Category::class.java,
            ResponseHandling::class.java,
        )
    }
}
