package com.startrails.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.startrails.core.model.BlendMode
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame
import com.startrails.core.model.JobStatus
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.StackJob

@Entity(tableName = "stack_jobs")
data class StackJobEntity(
    @PrimaryKey val id: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val blendMode: String,
    val recursive: Boolean,
    val outputFormat: String,
    val quality: Int,
    val frameCount: Int,
    val processedCount: Int,
    val width: Int,
    val height: Int,
    val outputUri: String?,
    val stagingPath: String?,
    val errorCode: String?,
    val errorMessage: String?,
    val workId: String?,
    val elapsedMs: Long,
    val sourceLabel: String?,
)

@Entity(
    tableName = "input_frames",
    primaryKeys = ["jobId", "frameOrder"],
    indices = [Index("jobId")],
)
data class InputFrameEntity(
    val jobId: String,
    val uri: String,
    val documentKey: String,
    val displayName: String,
    val sourceOrder: Int,
    val frameOrder: Int,
    val mimeType: String?,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val issueCode: String?,
    val issueDetail: String?,
)

fun StackJob.toEntity(): StackJobEntity = StackJobEntity(
    id = id,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    blendMode = blendMode.key,
    recursive = recursive,
    outputFormat = outputFormat.key,
    quality = quality,
    frameCount = frameCount,
    processedCount = processedCount,
    width = width,
    height = height,
    outputUri = outputUri,
    stagingPath = stagingPath,
    errorCode = errorCode?.key,
    errorMessage = errorMessage,
    workId = workId,
    elapsedMs = elapsedMs,
    sourceLabel = sourceLabel,
)

fun StackJobEntity.toModel(): StackJob = StackJob(
    id = id,
    status = runCatching { JobStatus.valueOf(status) }.getOrDefault(JobStatus.FAILED),
    createdAt = createdAt,
    updatedAt = updatedAt,
    blendMode = BlendMode.fromKey(blendMode),
    recursive = recursive,
    outputFormat = OutputFormat.fromKey(outputFormat),
    quality = quality,
    frameCount = frameCount,
    processedCount = processedCount,
    width = width,
    height = height,
    outputUri = outputUri,
    stagingPath = stagingPath,
    errorCode = errorCode?.let { ErrorCode.fromKey(it) },
    errorMessage = errorMessage,
    workId = workId,
    elapsedMs = elapsedMs,
    sourceLabel = sourceLabel,
)

fun InputFrame.toEntity(): InputFrameEntity = InputFrameEntity(
    jobId = jobId,
    uri = uri,
    documentKey = documentKey,
    displayName = displayName,
    sourceOrder = sourceOrder,
    frameOrder = frameOrder,
    mimeType = mimeType,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    issueCode = issueCode?.key,
    issueDetail = issueDetail,
)

fun InputFrameEntity.toModel(): InputFrame = InputFrame(
    id = 0,
    jobId = jobId,
    uri = uri,
    documentKey = documentKey,
    displayName = displayName,
    sourceOrder = sourceOrder,
    frameOrder = frameOrder,
    mimeType = mimeType,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    issueCode = issueCode?.let { ErrorCode.fromKey(it) },
    issueDetail = issueDetail,
)
