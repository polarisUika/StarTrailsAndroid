package com.startrails.core.imaging

import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 24 位未压缩 BMP，自下而上存储，每行补齐到 4 字节。 */
internal object BmpWriter {

    fun write(pixels: FramePixels, output: OutputStream) {
        val rowSize = (pixels.width * 3 + 3) / 4 * 4
        val imageSize = rowSize * pixels.height

        val header = ByteBuffer.allocate(14 + 40).order(ByteOrder.LITTLE_ENDIAN)
        header.put('B'.code.toByte()).put('M'.code.toByte())
        header.putInt(14 + 40 + imageSize)
        header.putShort(0).putShort(0)
        header.putInt(54)

        header.putInt(40)
        header.putInt(pixels.width)
        header.putInt(pixels.height)
        header.putShort(1)
        header.putShort(24)
        header.putInt(0)
        header.putInt(imageSize)
        header.putInt(0).putInt(0).putInt(0).putInt(0)
        output.write(header.array())

        // 复用单行缓冲逐行写出，避免一次性分配整张未压缩图像。
        val row = ByteArray(rowSize)
        var rowIndex = 0
        while (rowIndex < pixels.height) {
            val y = pixels.height - 1 - rowIndex
            var src = y * pixels.width * 3
            var dst = 0
            while (dst < pixels.width * 3) {
                row[dst] = pixels.rgb[src + 2]
                row[dst + 1] = pixels.rgb[src + 1]
                row[dst + 2] = pixels.rgb[src]
                src += 3
                dst += 3
            }
            output.write(row, 0, rowSize)
            rowIndex++
        }
    }
}
