package com.startrails.core.imaging

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/** 把 SAF/MediaStore URI 解码为 EXIF 转正、黑底合成后的 8 位 RGB。 */
object ImageDecoder {

    fun decode(resolver: ContentResolver, uri: Uri): FramePixels {
        val name = uri.lastPathSegment ?: "unknown"
        val orientation = readOrientation(resolver, uri, name)

        val base = try {
            val stream = resolver.openInputStream(uri) ?: throw ImagingException.PermissionLost(name)
            stream.use {
                BitmapFactory.decodeStream(
                    it,
                    null,
                    BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 },
                )
            } ?: throw ImagingException.Decode(name)
        } catch (e: ImagingException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw ImagingException.Memory(e)
        } catch (e: SecurityException) {
            throw ImagingException.PermissionLost(name)
        } catch (e: Exception) {
            throw ImagingException.Decode(name, e)
        }

        val oriented = try {
            rotateAccordingToExif(base, orientation)
        } catch (e: OutOfMemoryError) {
            base.recycle()
            throw ImagingException.Memory(e)
        }
        if (oriented !== base) base.recycle()
        return toRgb(oriented)
    }

    /** 按行读取像素，避免为整张图额外分配一份 4 B/px 的 IntArray。 */
    private fun toRgb(bitmap: Bitmap): FramePixels {
        val width = bitmap.width
        val height = bitmap.height
        val rgb = try {
            ByteArray(width * height * 3)
        } catch (e: OutOfMemoryError) {
            bitmap.recycle()
            throw ImagingException.Memory(e)
        }
        try {
            val row = IntArray(width)
            var offset = 0
            var y = 0
            while (y < height) {
                bitmap.getPixels(row, 0, width, 0, y, width, 1)
                var x = 0
                while (x < width) {
                    val pixel = row[x]
                    val alpha = pixel ushr 24
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF
                    if (alpha == 0xFF) {
                        rgb[offset] = r.toByte()
                        rgb[offset + 1] = g.toByte()
                        rgb[offset + 2] = b.toByte()
                    } else {
                        // 透明像素合成到黑色背景，与 Pillow alpha_composite 对齐。
                        rgb[offset] = ((r * alpha + 127) / 255).toByte()
                        rgb[offset + 1] = ((g * alpha + 127) / 255).toByte()
                        rgb[offset + 2] = ((b * alpha + 127) / 255).toByte()
                    }
                    offset += 3
                    x++
                }
                y++
            }
        } catch (e: OutOfMemoryError) {
            throw ImagingException.Memory(e)
        } finally {
            bitmap.recycle()
        }
        return FramePixels(width, height, rgb)
    }

    private fun readOrientation(resolver: ContentResolver, uri: Uri, name: String): Int = try {
        val stream = resolver.openInputStream(uri) ?: throw ImagingException.PermissionLost(name)
        stream.use {
            ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED,
            )
        }
    } catch (e: ImagingException) {
        throw e
    } catch (e: SecurityException) {
        throw ImagingException.PermissionLost(name)
    } catch (_: Exception) {
        // EXIF 缺失或损坏不影响像素解码。
        ExifInterface.ORIENTATION_UNDEFINED
    }

    private fun rotateAccordingToExif(source: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return source
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }
}
