package com.startrails.core.imaging

import android.graphics.Bitmap
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import com.startrails.core.model.OutputFormat
import java.io.File
import java.io.OutputStream

/** 把 RGB 缓冲编码为目标格式；TIFF/BMP 由内置无损写入器处理。 */
object ImageEncoder {

    const val OUTPUT_MARKER = "StarTrails output"

    fun encode(pixels: FramePixels, format: OutputFormat, quality: Int, output: OutputStream) {
        when (format) {
            OutputFormat.TIFF -> TiffWriter.write(pixels, output)
            OutputFormat.BMP -> BmpWriter.write(pixels, output)
            else -> encodeBitmap(pixels, format, quality, output)
        }
    }

    private fun encodeBitmap(pixels: FramePixels, format: OutputFormat, quality: Int, output: OutputStream) {
        val bitmap = try {
            Bitmap.createBitmap(pixels.width, pixels.height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            throw ImagingException.Memory(e)
        }
        try {
            // 逐行 setPixels，避免再分配一份 4 B/px 的整图 IntArray。
            val row = IntArray(pixels.width)
            var y = 0
            var offset = 0
            while (y < pixels.height) {
                var x = 0
                while (x < pixels.width) {
                    row[x] = (0xFF shl 24) or
                        ((pixels.rgb[offset].toInt() and 0xFF) shl 16) or
                        ((pixels.rgb[offset + 1].toInt() and 0xFF) shl 8) or
                        (pixels.rgb[offset + 2].toInt() and 0xFF)
                    offset += 3
                    x++
                }
                bitmap.setPixels(row, 0, pixels.width, 0, y, pixels.width, 1)
                y++
            }
            val compressFormat = when (format) {
                OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                OutputFormat.PNG -> Bitmap.CompressFormat.PNG
                OutputFormat.WEBP -> webpFormat(quality)
                else -> throw ImagingException.Output("不支持的输出格式：${format.key}")
            }
            val ok = bitmap.compress(compressFormat, quality.coerceIn(1, 100), output)
            if (!ok) throw ImagingException.Output("图像编码失败")
        } catch (e: ImagingException) {
            throw e
        } catch (e: OutOfMemoryError) {
            throw ImagingException.Memory(e)
        } catch (e: Exception) {
            throw ImagingException.Output("图像编码失败", e)
        } finally {
            bitmap.recycle()
        }
    }

    /** Bitmap.CompressFormat.WEBP 自 API 30 起废弃，按质量选择有损/无损新常量。 */
    @Suppress("DEPRECATION")
    private fun webpFormat(quality: Int): Bitmap.CompressFormat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (quality >= 100) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP_LOSSY
            }
        } else {
            Bitmap.CompressFormat.WEBP
        }

    /** JPEG/PNG/WebP 写入 ImageDescription 标记；TIFF 由写入器自身写入，BMP 无元数据。 */
    fun writeMarker(file: File, format: OutputFormat) {
        if (format == OutputFormat.BMP || format == OutputFormat.TIFF) return
        try {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, OUTPUT_MARKER)
                saveAttributes()
            }
        } catch (_: Exception) {
            // 标记失败不影响图像本身，不阻断导出。
        }
    }
}
