package com.startrails.core.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 从 SAF 多选文件或目录树中发现照片，保留来源顺序、组内自然排序并去重。 */
data class DiscoveredFrame(
    val uri: Uri,
    val documentKey: String,
    val displayName: String,
    val sourceOrder: Int,
    val mimeType: String?,
    val sizeBytes: Long,
)

@Singleton
class FrameSourceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun discover(uris: List<Uri>, recursive: Boolean): List<DiscoveredFrame> =
        withContext(Dispatchers.IO) {
            val result = mutableListOf<DiscoveredFrame>()
            val seen = HashSet<String>()
            uris.forEachIndexed { index, uri ->
                takePersistablePermission(uri)
                val group = if (DocumentsContract.isTreeUri(uri)) {
                    scanTree(uri, recursive, index)
                } else {
                    listOfNotNull(singleFile(uri, index))
                }
                group.sortedWith { a, b -> NaturalKey(a.displayName).compareTo(NaturalKey(b.displayName)) }
                    .forEach { frame -> if (seen.add(frame.documentKey)) result.add(frame) }
            }
            result
        }

    fun isReadable(uri: Uri): Boolean = try {
        resolver.openInputStream(uri)?.use { true } ?: false
    } catch (_: SecurityException) {
        false
    } catch (_: Exception) {
        false
    }

    private fun singleFile(uri: Uri, sourceOrder: Int): DiscoveredFrame? {
        val documentId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrDefault(uri.toString())
        val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: return null
        if (!isSupportedName(name)) return null
        return DiscoveredFrame(
            uri = uri,
            documentKey = "${uri.authority ?: "media"}/$documentId",
            displayName = name,
            sourceOrder = sourceOrder,
            mimeType = resolver.getType(uri),
            sizeBytes = querySize(uri),
        )
    }

    private fun scanTree(treeUri: Uri, recursive: Boolean, sourceOrder: Int): List<DiscoveredFrame> {
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull() ?: return emptyList()
        return scanChildren(treeUri, rootId, recursive, sourceOrder, HashSet())
    }

    private fun scanChildren(
        treeUri: Uri,
        parentId: String,
        recursive: Boolean,
        sourceOrder: Int,
        visited: MutableSet<String>,
    ): List<DiscoveredFrame> {
        if (!visited.add(parentId)) return emptyList()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
        )
        val frames = mutableListOf<DiscoveredFrame>()
        try {
            resolver.query(childrenUri, projection, null, null, "${Document.COLUMN_DISPLAY_NAME} ASC")
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(Document.COLUMN_DOCUMENT_ID)
                    val nameIndex = cursor.getColumnIndex(Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndex(Document.COLUMN_MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndex(Document.COLUMN_SIZE)
                    while (cursor.moveToNext()) {
                        val id = if (idIndex >= 0) cursor.getString(idIndex) else continue
                        val name = if (nameIndex >= 0) cursor.getString(nameIndex) ?: id else id
                        val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else null
                        if (mime == Document.MIME_TYPE_DIR) {
                            if (recursive) {
                                frames.addAll(scanChildren(treeUri, id, true, sourceOrder, visited))
                            }
                            continue
                        }
                        if (!isSupportedName(name)) continue
                        val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                        frames.add(
                            DiscoveredFrame(
                                uri = childUri,
                                documentKey = "${treeUri.authority}/$id",
                                displayName = name,
                                sourceOrder = sourceOrder,
                                mimeType = mime,
                                sizeBytes = size,
                            ),
                        )
                    }
                }
        } catch (_: SecurityException) {
            // 权限失效时不返回半成品，交给上层提示重新选择。
            return emptyList()
        }
        return frames
    }

    private fun takePersistablePermission(uri: Uri) {
        val alreadyGranted = resolver.persistedUriPermissions?.any {
            it.uri == uri && it.isReadPermission
        } == true
        if (alreadyGranted) return
        try {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // 非持久授权 URI 在当前进程内仍可读，忽略即可。
        }
    }

    private fun queryDisplayName(uri: Uri): String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) cursor.getString(index) else null
            } else null
        }
    } catch (_: Exception) {
        null
    }

    private fun querySize(uri: Uri): Long = try {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) cursor.getLong(index) else 0L
            } else 0L
        } ?: 0L
    } catch (_: Exception) {
        0L
    }

    private fun isSupportedName(name: String): Boolean {
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in SUPPORTED_EXTENSIONS && !name.startsWith(".")
    }

    /** IMG_2 排在 IMG_10 前：数字段按数值、文本段按字典序。 */
    private class NaturalKey(private val name: String) : Comparable<NaturalKey> {
        override fun compareTo(other: NaturalKey): Int {
            val left = TOKEN.findAll(name.lowercase()).map { it.value }.toList()
            val right = TOKEN.findAll(other.name.lowercase()).map { it.value }.toList()
            for (i in 0 until minOf(left.size, right.size)) {
                val a = left[i]
                val b = right[i]
                val aNumber = a.toBigIntegerOrNull()
                val bNumber = b.toBigIntegerOrNull()
                val result = when {
                    aNumber != null && bNumber != null -> aNumber.compareTo(bNumber)
                    else -> a.compareTo(b)
                }
                if (result != 0) return result
            }
            return left.size.compareTo(right.size)
        }

        companion object {
            private val TOKEN = Regex("\\d+|\\D+")
        }
    }

    companion object {
        private val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "tif", "tiff", "webp", "bmp")
    }
}
