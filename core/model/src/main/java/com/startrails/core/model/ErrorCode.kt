package com.startrails.core.model

/** 稳定错误码；界面负责映射为可理解文案，技术细节只进调试日志。 */
enum class ErrorCode(val key: String) {
    NOT_ENOUGH_FRAMES("NOT_ENOUGH_FRAMES"),
    UNSUPPORTED_FORMAT("UNSUPPORTED_FORMAT"),
    DECODE_FAILED("DECODE_FAILED"),
    DIMENSION_MISMATCH("DIMENSION_MISMATCH"),
    URI_PERMISSION_LOST("URI_PERMISSION_LOST"),
    INSUFFICIENT_MEMORY("INSUFFICIENT_MEMORY"),
    INSUFFICIENT_STORAGE("INSUFFICIENT_STORAGE"),
    OUTPUT_FAILED("OUTPUT_FAILED"),
    CANCELLED("CANCELLED"),
    UNKNOWN("UNKNOWN");

    companion object {
        fun fromKey(key: String?): ErrorCode = entries.firstOrNull { it.key == key } ?: UNKNOWN
    }
}

/** 面向任务/界面的失败描述，detail 仅供调试日志使用。 */
data class StackFailure(
    val code: ErrorCode,
    val detail: String? = null,
)
