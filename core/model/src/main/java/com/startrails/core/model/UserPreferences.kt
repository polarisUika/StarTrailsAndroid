package com.startrails.core.model

/** 主题模式：浅色 / 深色 / 跟随系统。 */
enum class ThemeMode(val key: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode = entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}

/** DataStore 中的全部偏好项。 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultBlendMode: BlendMode = BlendMode.LIGHTEN,
    val defaultFormat: OutputFormat = OutputFormat.JPEG,
    val defaultQuality: Int = QualityLevel.HIGHEST.percent,
    val defaultRecursive: Boolean = false,
    val privacyNoticeAcknowledged: Boolean = false,
    val colorScheme: AppColorScheme = AppColorScheme.INDIGO,
)
