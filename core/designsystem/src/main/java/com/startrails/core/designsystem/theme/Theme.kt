package com.startrails.core.designsystem.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.startrails.core.model.AppColorScheme
import com.startrails.core.model.ThemeMode
import com.startrails.core.model.UserPreferences

/**
 * 统一的浅色角色装配：四套方案只替换 primary / secondary / tertiary 三组，
 * 中性表面与错误色保持全局一致，避免照片内容被界面染色。
 * 色值遵循 Material 3 tonal palette：主色 40、容器 90、onContainer 10。
 */
private fun starTrailsLightScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    inversePrimary: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Red40,
    onError = Neutral0,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = Neutral90,
    onSurfaceVariant = Neutral30,
    surfaceTint = primary,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    outline = Neutral50,
    outlineVariant = Neutral80,
    scrim = Color.Black,
    surfaceBright = Neutral98,
    surfaceDim = Neutral87,
    surfaceContainerLowest = Neutral0,
    surfaceContainerLow = Neutral96,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
)

/**
 * 统一的深色角色装配：不是简单反色，而是以深午夜蓝为基底的分层表面。
 * 主要文本使用 84% 白，次要文本使用低饱和蓝灰，避免大字号刺眼。
 */
private fun starTrailsDarkScheme(
    primary: Color,
    onPrimary: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    inversePrimary: Color,
    secondary: Color,
    onSecondary: Color,
    secondaryContainer: Color,
    onSecondaryContainer: Color,
    tertiary: Color,
    onTertiary: Color,
    tertiaryContainer: Color,
    onTertiaryContainer: Color,
): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    inversePrimary = inversePrimary,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = DarkSurfaceDim,
    onBackground = Neutral90,
    surface = DarkSurfaceDim,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,
    surfaceTint = primary,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    outline = Neutral60,
    outlineVariant = Neutral30,
    scrim = Color.Black,
    surfaceBright = DarkSurfaceBright,
    surfaceDim = DarkSurfaceDim,
    surfaceContainerLowest = DarkSurfaceLowest,
    surfaceContainerLow = DarkSurfaceLow,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
)

/** 星空靛蓝：品牌默认方案，午夜靛蓝 + 星光青 + 暖金光。 */
private val IndigoLightScheme = starTrailsLightScheme(
    primary = Indigo40,
    onPrimary = Neutral0,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,
    inversePrimary = Indigo80,
    secondary = Cyan40,
    onSecondary = Neutral0,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Cyan10,
    tertiary = Amber40,
    onTertiary = Neutral0,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
)

private val IndigoDarkScheme = starTrailsDarkScheme(
    primary = Indigo80,
    onPrimary = Indigo20,
    primaryContainer = Indigo30,
    onPrimaryContainer = Indigo90,
    inversePrimary = Indigo40,
    secondary = Cyan80,
    onSecondary = Cyan20,
    secondaryContainer = Cyan30,
    onSecondaryContainer = Cyan90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
)

/** 极光绿：森林绿主色 + 苔藓次色 + 湖水第三色。 */
private val AuroraLightScheme = starTrailsLightScheme(
    primary = Color(0xFF386A20),
    onPrimary = Neutral0,
    primaryContainer = Color(0xFFB7F397),
    onPrimaryContainer = Color(0xFF042100),
    inversePrimary = Color(0xFF9CD67D),
    secondary = Color(0xFF55624C),
    onSecondary = Neutral0,
    secondaryContainer = Color(0xFFD9E7CB),
    onSecondaryContainer = Color(0xFF131F0D),
    tertiary = Color(0xFF386667),
    onTertiary = Neutral0,
    tertiaryContainer = Color(0xFFBBEBEC),
    onTertiaryContainer = Color(0xFF002020),
)

private val AuroraDarkScheme = starTrailsDarkScheme(
    primary = Color(0xFF9CD67D),
    onPrimary = Color(0xFF0A3900),
    primaryContainer = Color(0xFF205107),
    onPrimaryContainer = Color(0xFFB7F397),
    inversePrimary = Color(0xFF386A20),
    secondary = Color(0xFFBDCBB0),
    onSecondary = Color(0xFF283420),
    secondaryContainer = Color(0xFF3E4A36),
    onSecondaryContainer = Color(0xFFD9E7CB),
    tertiary = Color(0xFFA0CFD0),
    onTertiary = Color(0xFF003738),
    tertiaryContainer = Color(0xFF1E4E4F),
    onTertiaryContainer = Color(0xFFBBEBEC),
)

/** 暮光紫：Material 3 基线紫 + 中性紫灰 + 暮色粉。 */
private val TwilightLightScheme = starTrailsLightScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Neutral0,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    inversePrimary = Color(0xFFD0BCFF),
    secondary = Color(0xFF625B71),
    onSecondary = Neutral0,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Neutral0,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
)

private val TwilightDarkScheme = starTrailsDarkScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    inversePrimary = Color(0xFF6750A4),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
)

/** 落日橙：暖橙主色 + 陶土次色 + 橄榄第三色。 */
private val SunsetLightScheme = starTrailsLightScheme(
    primary = Color(0xFF8B5000),
    onPrimary = Neutral0,
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2D1600),
    inversePrimary = Color(0xFFFFB870),
    secondary = Color(0xFF725A42),
    onSecondary = Neutral0,
    secondaryContainer = Color(0xFFFFDDC0),
    onSecondaryContainer = Color(0xFF291806),
    tertiary = Color(0xFF5C6237),
    onTertiary = Neutral0,
    tertiaryContainer = Color(0xFFE0E7B1),
    onTertiaryContainer = Color(0xFF191E00),
)

private val SunsetDarkScheme = starTrailsDarkScheme(
    primary = Color(0xFFFFB870),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C00),
    onPrimaryContainer = Color(0xFFFFDCBE),
    inversePrimary = Color(0xFF8B5000),
    secondary = Color(0xFFE1C1A4),
    onSecondary = Color(0xFF402C18),
    secondaryContainer = Color(0xFF58422C),
    onSecondaryContainer = Color(0xFFFFDDC0),
    tertiary = Color(0xFFC4CA97),
    onTertiary = Color(0xFF2E330B),
    tertiaryContainer = Color(0xFF444A20),
    onTertiaryContainer = Color(0xFFE0E7B1),
)

/** 把用户选择的静态配色映射为当前明暗模式下的完整 ColorScheme。 */
fun AppColorScheme.toColorScheme(dark: Boolean): ColorScheme = when (this) {
    AppColorScheme.INDIGO -> if (dark) IndigoDarkScheme else IndigoLightScheme
    AppColorScheme.AURORA -> if (dark) AuroraDarkScheme else AuroraLightScheme
    AppColorScheme.TWILIGHT -> if (dark) TwilightDarkScheme else TwilightLightScheme
    AppColorScheme.SUNSET -> if (dark) SunsetDarkScheme else SunsetLightScheme
}

/**
 * 应用主题入口。
 *
 * @param preferences 用户偏好：主题模式、动态配色开关与静态配色方案。
 */
@Composable
fun StarTrailsTheme(
    preferences: UserPreferences,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (preferences.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        preferences.dynamicColor && supportsDynamic ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> preferences.colorScheme.toColorScheme(darkTheme)
    }

    SystemBarsAppearance(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StarTrailsTypography,
        shapes = StarTrailsShapes,
        content = content,
    )
}

/**
 * 让状态栏 / 导航栏图标的明暗跟随应用主题，而不是跟随系统。
 * 应用内手动切到深色主题时，系统仍可能处于浅色模式，这里必须同步，
 * 否则会出现"深色图标压在深色背景上"的不可读状态。
 */
@Composable
private fun SystemBarsAppearance(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context.findActivity() ?: return

    SideEffect {
        val window = activity.window
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 关闭系统给手势导航栏自动加的对比遮罩，保持边到边视觉纯净。
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
    }
}

/** 递归解包 ContextWrapper，避免把被包装的 Context 直接强转为 Activity 而崩溃。 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
