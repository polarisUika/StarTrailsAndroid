package com.startrails.core.designsystem.theme

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable

/**
 * 页面内 Scaffold 的内容安全区。
 *
 * 顶部的状态栏由各页面 TopAppBar 自己处理，底部的导航栏由顶层导航外壳统一预留，
 * 因此页面内容只需要处理横屏刘海等左右方向的系统区域，避免重复留白。
 */
val ScreenContentInsets: WindowInsets
    @Composable
    get() = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
