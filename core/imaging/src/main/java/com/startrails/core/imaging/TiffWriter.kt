package com.startrails.core.imaging

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 基线 TIFF（RGB 8 位、单条带、无压缩）+ ImageDescription 标记。
 * LZW 压缩需要原生库参与，属于后续 NDK 阶段；此处保证格式可用且无损。
 */
internal object TiffWriter {

    private const val TYPE_ASCII = 2
    private const val TYPE_SHORT = 3
    private const val TYPE_LONG = 4
    private const val ENTRY_COUNT = 11

    fun write(pixels: FramePixels, output: OutputStream) {
        val description = (ImageEncoder.OUTPUT_MARKER + '\u0000').toByteArray(Charsets.US_ASCII)
        val dataStart = 8 + 2 + ENTRY_COUNT * 12 + 4
        val bitsOffset = dataStart
        val descriptionOffset = bitsOffset + 6
        val pixelsOffset = descriptionOffset + description.size

        // 只缓冲文件头与 IFD，像素数据直接流式写出，避免再分配一份整图大小的数组。
        val header = ByteBuffer.allocate(pixelsOffset).order(ByteOrder.LITTLE_ENDIAN)
        header.put('I'.code.toByte()).put('I'.code.toByte())
        header.putShort(42)
        header.putInt(8)
        header.putShort(ENTRY_COUNT.toShort())

        fun entry(tag: Int, type: Int, count: Int, value: Int) {
            header.putShort(tag.toShort()).putShort(type.toShort()).putInt(count)
            if (type == TYPE_SHORT && count == 1) {
                header.putShort(value.toShort()).putShort(0)
            } else {
                header.putInt(value)
            }
        }

        // IFD 必须按 tag 升序排列。
        entry(256, TYPE_LONG, 1, pixels.width)
        entry(257, TYPE_LONG, 1, pixels.height)
        entry(258, TYPE_SHORT, 3, bitsOffset)
        entry(259, TYPE_SHORT, 1, 1) // 无压缩
        entry(262, TYPE_SHORT, 1, 2) // RGB
        entry(270, TYPE_ASCII, description.size, descriptionOffset)
        entry(273, TYPE_LONG, 1, pixelsOffset)
        entry(277, TYPE_SHORT, 1, 3)
        entry(278, TYPE_LONG, 1, pixels.height)
        entry(279, TYPE_LONG, 1, pixels.rgb.size)
        entry(284, TYPE_SHORT, 1, 1)
        header.putInt(0) // 下一个 IFD 偏移

        header.position(bitsOffset)
        header.putShort(8).putShort(8).putShort(8)
        header.position(descriptionOffset)
        header.put(description)

        output.write(header.array())
        output.write(pixels.rgb)
    }
}
