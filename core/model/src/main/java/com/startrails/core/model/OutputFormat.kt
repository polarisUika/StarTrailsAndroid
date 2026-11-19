package com.startrails.core.model

/** V1 支持的全部输出格式。 */
enum class OutputFormat(
    val key: String,
    val extension: String,
    val mimeType: String,
    val supportsQuality: Boolean,
) {
    JPEG("jpg", "jpg", "image/jpeg", true),
    PNG("png", "png", "image/png", false),
    TIFF("tiff", "tiff", "image/tiff", false),
    WEBP("webp", "webp", "image/webp", true),
    BMP("bmp", "bmp", "image/bmp", false);

    companion object {
        fun fromKey(key: String?): OutputFormat = entries.firstOrNull { it.key == key } ?: JPEG
    }
}
