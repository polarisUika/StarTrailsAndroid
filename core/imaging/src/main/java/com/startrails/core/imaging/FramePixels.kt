package com.startrails.core.imaging

/** 8 位 RGB 像素缓冲，每像素连续 3 字节，不保存 alpha。 */
class FramePixels(val width: Int, val height: Int, val rgb: ByteArray) {

    /** ByteArray 默认只比较引用，这里按内容比较，避免缓存命中/相等判断失效。 */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FramePixels) return false
        return width == other.width && height == other.height && rgb.contentEquals(other.rgb)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + rgb.contentHashCode()
        return result
    }

    override fun toString(): String = "FramePixels(${width}x$height, ${rgb.size} bytes)"
}
