package com.startrails.core.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.startrails.core.model.StackJob
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class ExportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** 结果只写应用私有暂存区，编码成功后才发布到 MediaStore 或用户指定位置。 */
@Singleton
class ResultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val resolver: ContentResolver = context.contentResolver

    suspend fun saveToGallery(staging: File, job: StackJob): Uri = withContext(Dispatchers.IO) {
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val name = "star-trails-${job.id.take(8)}.${job.outputFormat.extension}"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, job.outputFormat.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/StarTrails")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = try {
            resolver.insert(collection, values) ?: throw ExportException("无法创建相册条目")
        } catch (e: Exception) {
            throw ExportException("无法创建相册条目", e)
        }
        try {
            copyFile(staging, uri)
            val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            resolver.update(uri, published, null, null)
            uri
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw if (e is ExportException) e else ExportException("保存到相册失败", e)
        }
    }

    suspend fun exportTo(staging: File, target: Uri, createdByUs: Boolean) = withContext(Dispatchers.IO) {
        try {
            copyFile(staging, target)
        } catch (e: Exception) {
            if (createdByUs && DocumentsContract.isDocumentUri(context, target)) {
                runCatching { DocumentsContract.deleteDocument(resolver, target) }
            }
            throw ExportException("导出文件失败", e)
        }
    }

    suspend fun delete(uriString: String): Boolean = withContext(Dispatchers.IO) {
        val uri = Uri.parse(uriString)
        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                DocumentsContract.deleteDocument(resolver, uri)
            } else {
                resolver.delete(uri, null, null) > 0
            }
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun shareUri(file: File): Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )

    fun stagingFile(name: String): File = File(File(context.filesDir, STAGING_DIR), name).also {
        it.parentFile?.mkdirs()
    }

    fun shareCopy(name: String): File = File(File(context.cacheDir, SHARE_DIR), name).also {
        it.parentFile?.mkdirs()
    }

    /**
     * 清理缓存：过期的分享副本，以及数据库中已无归属的孤儿暂存文件。
     * 只删除足够旧的文件，避免误删正在编码的任务产物。
     */
    suspend fun cleanupCaches(keepStaging: Set<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        File(context.cacheDir, SHARE_DIR).listFiles()?.forEach { file ->
            if (now - file.lastModified() > SHARE_MAX_AGE_MS) runCatching { file.delete() }
        }
        File(context.filesDir, STAGING_DIR).listFiles()?.forEach { file ->
            val orphan = file.absolutePath !in keepStaging
            if (orphan && now - file.lastModified() > ORPHAN_MAX_AGE_MS) {
                runCatching { file.delete() }
            }
        }
    }

    private fun copyFile(source: File, target: Uri) {
        resolver.openOutputStream(target, "wt")?.use { output ->
            source.inputStream().use { input -> input.copyTo(output, 256 * 1024) }
        } ?: throw ExportException("无法写入目标位置")
    }

    private companion object {
        const val STAGING_DIR = "staging"
        const val SHARE_DIR = "shares"
        const val SHARE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
        const val ORPHAN_MAX_AGE_MS = 24L * 60 * 60 * 1000
    }
}
