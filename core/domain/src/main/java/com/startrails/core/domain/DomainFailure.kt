package com.startrails.core.domain

import com.startrails.core.model.ErrorCode

/** 领域层失败，携带稳定错误码和用户可读文案。 */
class DomainFailure(
    val code: ErrorCode,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        fun permission(name: String) = DomainFailure(ErrorCode.URI_PERMISSION_LOST, "无法访问：$name，请重新选择照片")
        fun memory() = DomainFailure(ErrorCode.INSUFFICIENT_MEMORY, "图片过大，请减少照片数量或使用较低分辨率的原图")
        fun storage() = DomainFailure(ErrorCode.INSUFFICIENT_STORAGE, "存储空间不足，请清理后重试")
        fun notEnoughFrames() = DomainFailure(ErrorCode.NOT_ENOUGH_FRAMES, "至少需要 2 张照片才能叠加")
        fun busy() = DomainFailure(ErrorCode.UNKNOWN, "已有一个叠加任务在运行，请等待完成或取消")
    }
}
