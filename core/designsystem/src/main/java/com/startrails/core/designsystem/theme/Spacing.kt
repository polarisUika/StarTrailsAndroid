package com.startrails.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * 4dp 基础网格上的间距 token。页面内不要出现 13dp、18dp 这类随手值。
 */
object Spacing {
    /** 4dp：图标与文字之间的最小间隙 */
    val xs = 4.dp

    /** 8dp：同组元素之间 */
    val sm = 8.dp

    /** 12dp：列表项内部、按钮之间 */
    val md = 12.dp

    /** 16dp：卡片内边距、区块之间 */
    val lg = 16.dp

    /** 24dp：大区块之间、页面顶部留白 */
    val xl = 24.dp

    /** 32dp：页面级分区 */
    val xxl = 32.dp

    /** 紧凑屏页面左右边距 */
    val screenHorizontal = 16.dp

    /** 宽屏页面左右边距 */
    val screenHorizontalWide = 24.dp
}

/**
 * 布局约束：内容区最大宽度，避免平板上文字被拉成超长行。
 */
object Layout {
    /** 表单 / 设置类单列内容的最大宽度 */
    val formMaxWidth = 640.dp

    /** 图文内容的最大宽度 */
    val contentMaxWidth = 840.dp

    /** 图库网格单个格子的最小宽度 */
    val gridMinSize = 168.dp

    /** 底部操作栏的按钮最小高度，满足大屏触控观感 */
    val actionButtonHeight = 52.dp

    /** 主要操作按钮高度 */
    val primaryButtonHeight = 56.dp
}
