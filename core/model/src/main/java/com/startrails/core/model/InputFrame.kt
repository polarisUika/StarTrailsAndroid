package com.startrails.core.model

/**
 * 一张输入照片的元数据。Room 只保存这些信息，绝不保存完整图片。
 * [documentKey] 为 authority + documentId，用于跨 URI 去重。
 */
data class InputFrame(
    val id: Long = 0,
    val jobId: String,
    val uri: String,
    val documentKey: String,
    val displayName: String,
    val sourceOrder: Int,
    val frameOrder: Int,
    val mimeType: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val sizeBytes: Long = 0,
    val issueCode: ErrorCode? = null,
    val issueDetail: String? = null,
)
