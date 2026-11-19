package com.startrails.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 形状阶梯。相比 MD3 基线整体更圆润一档，用于呼应星轨的弧线语言：
 * 小控件 8dp、按钮/输入框 12dp、卡片 16dp、大容器 24dp、对话框与预览 32dp。
 */
val StarTrailsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
