package com.startrails.core.model

/** 一次叠加任务的完整快照。 */
data class StackJob(
    val id: String,
    val status: JobStatus = JobStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val blendMode: BlendMode = BlendMode.LIGHTEN,
    val recursive: Boolean = false,
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val quality: Int = QualityLevel.HIGHEST.percent,
    val frameCount: Int = 0,
    val processedCount: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val outputUri: String? = null,
    val stagingPath: String? = null,
    val errorCode: ErrorCode? = null,
    val errorMessage: String? = null,
    val workId: String? = null,
    val elapsedMs: Long = 0,
    val sourceLabel: String? = null,
) {
    val isFinished: Boolean get() = status.isTerminal
    val isBusy: Boolean get() = status.isActive
}
