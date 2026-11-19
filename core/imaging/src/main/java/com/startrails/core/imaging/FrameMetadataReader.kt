package com.startrails.core.imaging

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

data class FrameMetadata(
    val width: Int,
    val height: Int,
    val mimeType: String?,
    val sizeBytes: Long,
)

/** 只读尺寸元数据，不解码完整像素，用于任务开始前的一致性检查。 */
object FrameMetadataReader {

    fun read(resolver: ContentResolver, uri: Uri, name: String): FrameMetadata {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        try {
            // decodeStream 在 inJustDecodeBounds 模式下只解析文件头，不会把整张图读入堆内存。
            val opened = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
                true
            } ?: false
            if (!opened) throw ImagingException.PermissionLost(name)
        } catch (e: SecurityException) {
            throw ImagingException.PermissionLost(name)
        } catch (e: ImagingException) {
            throw e
        } catch (e: Exception) {
            throw ImagingException.Decode(name, e)
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) {
            throw ImagingException.Decode(name)
        }
        val swapped = readOrientation(resolver, uri) in setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE,
        )
        return FrameMetadata(
            width = if (swapped) options.outHeight else options.outWidth,
            height = if (swapped) options.outWidth else options.outHeight,
            mimeType = resolver.getType(uri),
            sizeBytes = querySize(resolver, uri),
        )
    }

    /** EXIF 只影响宽高是否需要交换，读取失败时按不旋转处理，不阻断导入。 */
    private fun readOrientation(resolver: ContentResolver, uri: Uri): Int = try {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
        } ?: ExifInterface.ORIENTATION_UNDEFINED
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_UNDEFINED
    }

    private fun querySize(resolver: ContentResolver, uri: Uri): Long = try {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (index >= 0) cursor.getLong(index) else 0L
                } else 0L
            } ?: 0L
    } catch (_: Exception) {
        0L
    }
}
