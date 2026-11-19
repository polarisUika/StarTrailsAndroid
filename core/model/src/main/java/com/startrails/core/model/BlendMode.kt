package com.startrails.core.model

/** 三种叠加算法，键名与桌面版保持一致。 */
enum class BlendMode(val key: String) {
    LIGHTEN("lighten"),
    SCREEN("screen"),
    AVERAGE("average");

    companion object {
        fun fromKey(key: String?): BlendMode = entries.firstOrNull { it.key == key } ?: LIGHTEN
    }
}
