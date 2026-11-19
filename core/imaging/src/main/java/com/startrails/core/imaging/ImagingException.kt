package com.startrails.core.imaging

import com.startrails.core.model.ErrorCode

/** 图像处理层异常，携带面向界面的稳定错误码。 */
sealed class ImagingException(
    val code: ErrorCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    class Decode(name: String, cause: Throwable? = null) :
        ImagingException(ErrorCode.DECODE_FAILED, "无法读取图像：$name", cause)

    class DimensionMismatch(
        first: String,
        firstSize: String,
        second: String,
        secondSize: String,
    ) : ImagingException(
        ErrorCode.DIMENSION_MISMATCH,
        "图像尺寸不一致：$first 为 $firstSize，$second 为 $secondSize",
    )

    class PermissionLost(name: String) :
        ImagingException(ErrorCode.URI_PERMISSION_LOST, "无法访问照片：$name，请重新选择")

    class Cancelled : ImagingException(ErrorCode.CANCELLED, "任务已取消")

    class Memory(cause: Throwable? = null) :
        ImagingException(ErrorCode.INSUFFICIENT_MEMORY, "内存不足，图像过大", cause)

    class Output(message: String, cause: Throwable? = null) :
        ImagingException(ErrorCode.OUTPUT_FAILED, message, cause)
}
