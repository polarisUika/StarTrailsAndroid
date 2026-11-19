package com.startrails.core.data

import com.startrails.core.model.BlendMode
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame
import com.startrails.core.model.JobStatus
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.StackJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StackJobRepository @Inject constructor(private val dao: StackDao) {

    val jobs: Flow<List<StackJob>> = dao.observeJobs().map { list -> list.map { it.toModel() } }

    val activeJob: Flow<StackJob?> = dao.observeActiveJob().map { it?.toModel() }

    suspend fun createDraft(
        blendMode: BlendMode,
        outputFormat: OutputFormat,
        quality: Int,
        recursive: Boolean,
        sourceLabel: String?,
    ): StackJob {
        val now = System.currentTimeMillis()
        val job = StackJob(
            id = UUID.randomUUID().toString(),
            status = JobStatus.DRAFT,
            createdAt = now,
            updatedAt = now,
            blendMode = blendMode,
            outputFormat = outputFormat,
            quality = quality,
            recursive = recursive,
            sourceLabel = sourceLabel,
        )
        dao.upsertJob(job.toEntity())
        return job
    }

    suspend fun save(job: StackJob) = dao.upsertJob(job.copy(updatedAt = System.currentTimeMillis()).toEntity())

    suspend fun getJob(id: String): StackJob? = dao.getJob(id)?.toModel()

    suspend fun getFrames(jobId: String): List<InputFrame> = dao.getFrames(jobId).map { it.toModel() }

    fun frames(jobId: String): Flow<List<InputFrame>> =
        dao.observeFrames(jobId).map { list -> list.map { it.toModel() } }

    suspend fun saveFrames(jobId: String, frames: List<InputFrame>) {
        dao.replaceFrames(jobId, frames.map { it.copy(jobId = jobId).toEntity() })
    }

    suspend fun deleteJob(jobId: String) = dao.deleteJobWithFrames(jobId)

    /** 是否存在除 [jobId] 之外的活动任务；草稿不占用执行通道。 */
    suspend fun hasOtherActiveJob(jobId: String): Boolean = dao.activeJobCountExcept(jobId) > 0

    /** 只回写进度计数，不会覆盖并发写入的 status。 */
    suspend fun updateProgress(jobId: String, processedCount: Int) =
        dao.updateProgress(jobId, processedCount, System.currentTimeMillis())

    /** 只回写状态，不会把已记录的进度清零。 */
    suspend fun updateStatus(jobId: String, status: JobStatus) =
        dao.updateStatus(jobId, status.name, System.currentTimeMillis())

    suspend fun markFailed(jobId: String, code: ErrorCode, message: String?) = dao.updateFailure(
        id = jobId,
        status = JobStatus.FAILED.name,
        errorCode = code.key,
        errorMessage = message,
        updatedAt = System.currentTimeMillis(),
    )

    suspend fun markCancelled(jobId: String) = dao.updateFailure(
        id = jobId,
        status = JobStatus.CANCELLED.name,
        errorCode = ErrorCode.CANCELLED.key,
        errorMessage = "任务已取消",
        updatedAt = System.currentTimeMillis(),
    )

    /** 进程被杀后残留的活动任务（未经 WorkManager 状态确认，调用方负责判定）。 */
    suspend fun interruptedJobs(): List<StackJob> = dao.getInterruptedJobs().map { it.toModel() }

    suspend fun stagingPaths(): Set<String> = dao.stagingPaths().toSet()
}
