package com.startrails.core.model

/** 稳定错误码到用户可理解文案的映射；技术细节留在调试日志中。 */
fun ErrorCode.userMessage(): String = when (this) {
    ErrorCode.NOT_ENOUGH_FRAMES -> "至少选择两张照片才能叠加"
    ErrorCode.UNSUPPORTED_FORMAT -> "包含不支持的图片格式"
    ErrorCode.DECODE_FAILED -> "有照片无法读取，请移除后重试"
    ErrorCode.DIMENSION_MISMATCH -> "照片尺寸不一致，请移除异常照片"
    ErrorCode.URI_PERMISSION_LOST -> "照片访问权限已失效，请重新选择"
    ErrorCode.INSUFFICIENT_MEMORY -> "图片过大，请减少数量或使用较小原图"
    ErrorCode.INSUFFICIENT_STORAGE -> "存储空间不足，请清理后重试"
    ErrorCode.OUTPUT_FAILED -> "保存失败，原片未受影响，可重试导出"
    ErrorCode.CANCELLED -> "任务已取消"
    ErrorCode.UNKNOWN -> "出现未知问题，请重试"
}

fun BlendMode.displayName(): String = when (this) {
    BlendMode.LIGHTEN -> "最大值"
    BlendMode.SCREEN -> "滤色"
    BlendMode.AVERAGE -> "平均"
}

fun BlendMode.description(): String = when (this) {
    BlendMode.LIGHTEN -> "逐通道取最大值，标准星轨合成，保留每帧最亮的星点。"
    BlendMode.SCREEN -> "滤色混合，叠加更柔和，但天空会逐渐变亮。"
    BlendMode.AVERAGE -> "逐像素求平均，适合降噪，不会形成分离的星轨。"
}

fun OutputFormat.displayName(): String = when (this) {
    OutputFormat.JPEG -> "JPEG"
    OutputFormat.PNG -> "PNG"
    OutputFormat.TIFF -> "TIFF"
    OutputFormat.WEBP -> "WebP"
    OutputFormat.BMP -> "BMP"
}

fun QualityLevel.displayName(): String = when (this) {
    QualityLevel.LOW -> "低"
    QualityLevel.MEDIUM -> "中"
    QualityLevel.HIGH -> "高"
    QualityLevel.HIGHEST -> "最高"
}

fun AppColorScheme.displayName(): String = when (this) {
    AppColorScheme.INDIGO -> "星空靛蓝"
    AppColorScheme.AURORA -> "极光绿"
    AppColorScheme.TWILIGHT -> "暮光紫"
    AppColorScheme.SUNSET -> "落日橙"
}
