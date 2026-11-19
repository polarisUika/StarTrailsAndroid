package com.startrails.core.imaging

import com.startrails.core.model.BlendMode
import kotlin.math.floor

/** 逐帧累加器。只保留当前帧和累加结果，不同时驻留全部照片。 */
interface PixelAccumulator {
    val width: Int
    val height: Int

    fun add(frame: FramePixels)

    /** 取回最终结果；调用后累加器不应再被使用。 */
    fun result(): FramePixels
}

/** 图像计算接口。当前由 Kotlin 实现，后续可替换为 C++/NDK 实现而无需改动调用方。 */
interface StackingEngine {
    fun create(mode: BlendMode, first: FramePixels): PixelAccumulator
}

/** 与桌面版 Python/NumPy 算法逐像素对齐的参考实现。 */
class KotlinStackingEngine : StackingEngine {
    override fun create(mode: BlendMode, first: FramePixels): PixelAccumulator = when (mode) {
        BlendMode.LIGHTEN -> ByteAccumulator(first, ::lighten)
        BlendMode.SCREEN -> ByteAccumulator(first, ::screen)
        BlendMode.AVERAGE -> AverageAccumulator(first)
    }
}

private typealias ByteKernel = (Int, Int) -> Int

private class ByteAccumulator(
    first: FramePixels,
    private val kernel: ByteKernel,
) : PixelAccumulator {
    override val width = first.width
    override val height = first.height
    private val buffer = first.rgb.copyOf()

    override fun add(frame: FramePixels) {
        require(frame.width == width && frame.height == height) { "尺寸不一致" }
        val source = frame.rgb
        var i = 0
        while (i < buffer.size) {
            buffer[i] = kernel(buffer[i].toInt() and 0xFF, source[i].toInt() and 0xFF).toByte()
            i++
        }
    }

    /** 直接交出内部缓冲，避免再复制一份整图大小的数组。 */
    override fun result(): FramePixels = FramePixels(width, height, buffer)
}

private class AverageAccumulator(first: FramePixels) : PixelAccumulator {
    override val width = first.width
    override val height = first.height

    // 8 位通道的累加用 IntArray：既没有浮点累加误差，也只有 FloatArray 一半的内存。
    private val sum = IntArray(first.rgb.size) { first.rgb[it].toInt() and 0xFF }
    private var count = 1

    override fun add(frame: FramePixels) {
        require(frame.width == width && frame.height == height) { "尺寸不一致" }
        var i = 0
        while (i < sum.size) {
            sum[i] += frame.rgb[i].toInt() and 0xFF
            i++
        }
        count++
    }

    override fun result(): FramePixels {
        val out = ByteArray(sum.size)
        var i = 0
        while (i < sum.size) {
            val mean = sum[i].toFloat() / count
            out[i] = rint(mean).coerceIn(0, 255).toByte()
            i++
        }
        return FramePixels(width, height, out)
    }
}

/** NumPy rint：最近取整，中点取偶数。 */
internal fun rint(value: Float): Int {
    val base = floor(value)
    val fraction = value - base
    return when {
        fraction < 0.5f -> base.toInt()
        fraction > 0.5f -> base.toInt() + 1
        else -> if ((base.toLong() and 1L) == 0L) base.toInt() else base.toInt() + 1
    }
}

internal fun lighten(a: Int, b: Int): Int = maxOf(a, b)

/** screen(a,b) = 255 - ((255-a)(255-b)+127)/255，整数中间值避免 8 位溢出。 */
internal fun screen(a: Int, b: Int): Int {
    val inverse = (255 - a) * (255 - b) + 127
    return 255 - inverse / 255
}
