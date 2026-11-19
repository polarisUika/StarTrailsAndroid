package com.startrails.core.model

/**
 * 输出质量的四个档位，[percent] 直接传给平台编码器。
 * 历史版本保存的任意 1–100 质量值通过 [fromQuality] 归入最近档位。
 */
enum class QualityLevel(val percent: Int) {
    LOW(25),
    MEDIUM(50),
    HIGH(75),
    HIGHEST(100);

    companion object {
        fun fromQuality(value: Int): QualityLevel {
            val clamped = value.coerceIn(1, 100)
            return entries.minByOrNull { kotlin.math.abs(it.percent - clamped) } ?: HIGHEST
        }
    }
}
