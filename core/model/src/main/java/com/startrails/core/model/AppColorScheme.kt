package com.startrails.core.model

/** 动态取色关闭后可选的四套静态配色。 */
enum class AppColorScheme(val key: String) {
    INDIGO("indigo"),
    AURORA("aurora"),
    TWILIGHT("twilight"),
    SUNSET("sunset");

    companion object {
        fun fromKey(key: String?): AppColorScheme =
            entries.firstOrNull { it.key == key } ?: INDIGO
    }
}
