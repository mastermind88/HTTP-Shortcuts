package ch.rmy.android.http_shortcuts.http

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.webkit.MimeTypeMap
import ch.rmy.android.framework.extensions.tryOrLog
import ch.rmy.android.http_shortcuts.utils.FileUtil
import java.io.FileNotFoundException

class FileUploadManager private constructor(
    private val contentResolver: ContentResolver,
    private val sharedFileUris: List<Uri>,
    private val fileRequests: Iterator<FileRequest>,
) {

    private val registeredFiles = mutableListOf<List<File>>()

    init {
        val sharedFiles = sharedFileUris.iterator()
        while (sharedFiles.hasNext()) {
            val request = getNextFileRequest() ?: break
            if (request.multiple) {
                registeredFiles.add(sharedFiles.asSequence().toList().map(::uriToFile))
            } else {
                registeredFiles.add(listOf(sharedFiles.next().let(::uriToFile)))
            }
        }
    }

    fun getFiles(): List<File> =
        registeredFiles.flatten()

    fun getFiles(index: Int): List<File> =
        registeredFiles.getOrNull(index) ?: emptyList()

    fun getFile(index: Int): File? =
        registeredFiles.getOrNull(index)?.firstOrNull()

    fun getNextFileRequest(): FileRequest? =
        fileRequests.takeIf { it.hasNext() }?.next()

    fun fulfilFileRequest(fileUris: List<Uri>) {
        registeredFiles.add(fileUris.map(::uriToFile))
    }

    private fun getType(file: Uri): String =
        contentResolver.getType(file) ?: FALLBACK_TYPE

    private fun uriToFile(uri: Uri): File =
        getType(uri).let { type ->
            File(
                mimeType = type,
                fileName = getFileName(uri, type),
                data = uri,
                fileSize = getFileSize(uri),
            )
        }

    private fun getFileName(file: Uri, type: String): String {
        // Case 1: The file was shared into the app and we can look up its original name instead of using the name of the cache file
        val cachedFileName = FileUtil.getCacheFileOriginalName(file)
        if (cachedFileName != null) {
            return cachedFileName
        }

        // Case 2: The content resolver provides the file's name
        tryOrLog {
            val fileName = FileUtil.getFileName(contentResolver, file)
            if (fileName != null) {
                return fileName
            }
        }

        // Case 3: We failed to determine the file name and fall back to a constructed file name, while trying to pick an appropriate file extension
        val potentialFileName = file.lastPathSegment ?: DEFAULT_FILE_NAME
        val expectedExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
        if (expectedExtension != null && !potentialFileName.endsWith(".$expectedExtension", ignoreCase = true)) {
            return "$potentialFileName.$expectedExtension"
        }
        return potentialFileName
    }

    private fun getFileSize(file: Uri): Long? =
        try {
            contentResolver.openAssetFileDescriptor(file, "r")
                ?.length
                ?.takeUnless { it == AssetFileDescriptor.UNKNOWN_LENGTH }
        } catch (e: FileNotFoundException) {
            null
        }

    data class File(val mimeType: String, val fileName: String, val data: Uri, val fileSize: Long?)

    class FileRequest(val multiple: Boolean)

    class Builder(private val contentResolver: ContentResolver) {

        private var sharedFiles: List<Uri>? = null
        private val fileRequests = mutableListOf<FileRequest>()

        fun withSharedFiles(fileUris: List<Uri>) = also {
            this.sharedFiles = fileUris
        }

        fun addFileRequest(multiple: Boolean = false) = also {
            fileRequests.add(FileRequest(multiple))
        }

        fun build() = FileUploadManager(
            contentResolver = contentResolver,
            sharedFileUris = sharedFiles ?: emptyList(),
            fileRequests = fileRequests.iterator()
        )
    }

    companion object {

        private const val FALLBACK_TYPE = "application/octet-stream"
        private const val DEFAULT_FILE_NAME = "file"
    }
}
