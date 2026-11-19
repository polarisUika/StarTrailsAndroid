package com.startrails.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/*
 * StarTrails 品牌色板。
 *
 * 色彩语义：
 *  - 午夜靛蓝（Indigo）：夜空与品牌主色，承担主要操作与强调；
 *  - 星光青（Cyan）：次级信息、辅助操作、进度；
 *  - 暖金光（Amber）：第三强调色，用于提醒、徽标与高亮点缀；
 *  - 冷调中性色（Neutral）：表面、容器与文字层级，保证图像内容不被染色干扰。
 *
 * 色调编号遵循 Material 3 的 tonal palette 习惯：数值越大越亮。
 * 页面代码只能使用 MaterialTheme.colorScheme 提供的语义角色，不得直接引用本文件。
 */

// 主色：午夜靛蓝
internal val Indigo10 = Color(0xFF06154A)
internal val Indigo20 = Color(0xFF182B63)
internal val Indigo30 = Color(0xFF2B417D)
internal val Indigo40 = Color(0xFF3F5899)
internal val Indigo60 = Color(0xFF728CC9)
internal val Indigo70 = Color(0xFF8CA6DC)
internal val Indigo80 = Color(0xFFB4C5FF)
internal val Indigo90 = Color(0xFFDCE1FF)
internal val Indigo95 = Color(0xFFEFF1FF)

// 次色：星光青
internal val Cyan10 = Color(0xFF001F28)
internal val Cyan20 = Color(0xFF00363F)
internal val Cyan30 = Color(0xFF004E5C)
internal val Cyan40 = Color(0xFF00677A)
internal val Cyan60 = Color(0xFF009DB8)
internal val Cyan70 = Color(0xFF3EBACF)
internal val Cyan80 = Color(0xFF7FD4E6)
internal val Cyan90 = Color(0xFFB6EBF7)
internal val Cyan95 = Color(0xFFD9F6FC)

// 第三色：暖金光
internal val Amber10 = Color(0xFF241A00)
internal val Amber20 = Color(0xFF3F2E00)
internal val Amber30 = Color(0xFF5C4400)
internal val Amber40 = Color(0xFF7A5C00)
internal val Amber70 = Color(0xFFDDB000)
internal val Amber80 = Color(0xFFF5C24C)
internal val Amber90 = Color(0xFFFFDF9C)
internal val Amber95 = Color(0xFFFFF0D6)

// 错误色
internal val Red10 = Color(0xFF410002)
internal val Red20 = Color(0xFF690005)
internal val Red30 = Color(0xFF93000A)
internal val Red40 = Color(0xFFBA1A1A)
internal val Red80 = Color(0xFFFFB4AB)
internal val Red90 = Color(0xFFFFDAD6)

// 中性色（表面 / 文字 / 描边）
internal val Neutral0 = Color(0xFFFFFFFF)
internal val Neutral10 = Color(0xFF1B1B21)
internal val Neutral12 = Color(0xFF202026)
internal val Neutral20 = Color(0xFF303038)
internal val Neutral22 = Color(0xFF35363E)
internal val Neutral24 = Color(0xFF3A3B43)
internal val Neutral30 = Color(0xFF45464F)
internal val Neutral50 = Color(0xFF787680)
internal val Neutral60 = Color(0xFF90909A)
internal val Neutral80 = Color(0xFFC7C5D0)
internal val Neutral87 = Color(0xFFDBD9E3)
internal val Neutral90 = Color(0xFFE4E2EC)
internal val Neutral92 = Color(0xFFEDECF5)
internal val Neutral94 = Color(0xFFF3F2FB)
internal val Neutral95 = Color(0xFFF7F5FE)
internal val Neutral96 = Color(0xFFF9F8FF)
internal val Neutral98 = Color(0xFFFCFAFF)

// 深色主题表面（带一丝午夜蓝，避免纯黑割裂感）
internal val DarkSurfaceDim = Color(0xFF0F1218)
internal val DarkSurfaceLowest = Color(0xFF090C11)
internal val DarkSurfaceLow = Color(0xFF161A21)
internal val DarkSurface = Color(0xFF1A1E25)
internal val DarkSurfaceHigh = Color(0xFF252930)
internal val DarkSurfaceHighest = Color(0xFF30343C)
internal val DarkSurfaceBright = Color(0xFF353941)

/**
 * 图片预览 / 取景区底色：固定中性深色，保证星轨对比度不随主题变化。
 * 属于设计系统公开 token，可在 feature 模块的图片容器上直接使用。
 */
val PreviewSurfaceNeutral = Color(0xFF101418)

/** 照片浮层文字色：压在照片上的固定白色，与主题无关。 */
val PhotoOverlayContent = Color(0xFFFFFFFF)

/** 照片浮层遮罩：固定黑色半透明，仅用于提升照片上文字的对比度。 */
val PhotoOverlayScrim = Color(0x73000000)
