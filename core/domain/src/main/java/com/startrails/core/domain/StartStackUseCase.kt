package com.startrails.core.domain

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.startrails.core.data.ResultRepository
import com.startrails.core.data.StackJobRepository
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.JobStatus
import com.startrails.core.model.StackJob
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject

object StackWork {
    const val UNIQUE_NAME = "startrails_stack"
    const val KEY_JOB_ID = "job_id"
    const val KEY_ERROR_CODE = "error_code"
    const val KEY_CURRENT_NAME = "current_name"
    const val NOTIFICATION_ID = 4201
    const val CHANNEL_ID = "startrails_stack"

    /** 取消后等待 WorkManager 进入终态的时间，超时则交由 Worker 自己写回终态。 */
    const val CANCEL_SETTLE_TIMEOUT_MS = 3_000L
}

class StartStackUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StackJobRepository,
) {
    suspend operator fun invoke(jobId: String): StackJob {
        val job = repository.getJob(jobId) ?: throw DomainFailure(ErrorCode.UNKNOWN, "任务不存在")
        // 只在“别的任务占用执行通道”时拦截；草稿（VALIDATING）不占用通道。
        if (repository.hasOtherActiveJob(jobId) || job.status.isActive) throw DomainFailure.busy()

        val frames = repository.getFrames(jobId)
        if (frames.size < 2) throw DomainFailure.notEnoughFrames()
        if (frames.any { it.issueCode != null }) {
            throw DomainFailure(ErrorCode.DIMENSION_MISMATCH, "存在尺寸不一致的照片，请先移除")
        }

        val queued = job.copy(
            status = JobStatus.QUEUED,
            processedCount = 0,
            elapsedMs = 0,
            errorCode = null,
            errorMessage = null,
            stagingPath = null,
        )
        repository.save(queued)

        val request = OneTimeWorkRequestBuilder<StackWorker>()
            .setInputData(Data.Builder().putString(StackWork.KEY_JOB_ID, jobId).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        repository.save(queued.copy(workId = request.id.toString()))

        // 能走到这里说明数据库中没有其他活动任务，残留的旧请求只可能是已结束的历史，
        // 用 REPLACE 代替 KEEP，避免新请求被丢弃后任务永久停留在 QUEUED。
        WorkManager.getInstance(context).enqueueUniqueWork(
            StackWork.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
        return queued.copy(workId = request.id.toString())
    }
}

class CancelStackUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StackJobRepository,
) {
    suspend operator fun invoke(jobId: String) {
        val job = repository.getJob(jobId) ?: return
        if (!job.status.isActive) {
            repository.markCancelled(jobId)
            return
        }
        repository.save(job.copy(status = JobStatus.CANCELLING))

        val uuid = job.workId?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        if (uuid == null) {
            repository.markCancelled(jobId)
            return
        }
        val workManager = WorkManager.getInstance(context)
        runCatching { workManager.cancelWorkById(uuid) }

        // 尚在排队就被取消的任务不会再有 Worker 回调，需要在这里补写终态；
        // 正在运行的 Worker 会自己写回 CANCELLED，超时后交给它处理。
        val settled = withTimeoutOrNull(StackWork.CANCEL_SETTLE_TIMEOUT_MS) {
            workManager.getWorkInfoByIdFlow(uuid).first { it == null || it.state.isFinished }
        }
        if (settled != null) repository.markCancelled(jobId)
    }
}

/**
 * 进程被杀后恢复任务状态：
 * 先查 WorkManager 中对应请求的真实状态，只对已结束或已不存在的残留记录做失败兜底，
 * 避免与进程重启后自动重新调度的 StackWorker 抢写数据库。
 */
class RecoverJobsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StackJobRepository,
    private val results: ResultRepository,
) {
    suspend operator fun invoke() = withContext(Dispatchers.IO) {
        val interrupted = repository.interruptedJobs()
        if (interrupted.isNotEmpty()) recover(interrupted)
        results.cleanupCaches(repository.stagingPaths())
    }

    private suspend fun recover(interrupted: List<StackJob>) {
        val infos = runCatching {
            WorkManager.getInstance(context).getWorkInfosForUniqueWork(StackWork.UNIQUE_NAME).get()
        }.getOrNull() ?: return // 无法确认时宁可不改，也不要误杀正在运行的任务

        val states = infos.associateBy { it.id.toString() }
        interrupted.forEach { job ->
            val info = job.workId?.let { states[it] }
            val stillAlive = info != null && info.state in LIVE_STATES
            if (!stillAlive) {
                repository.markFailed(job.id, ErrorCode.UNKNOWN, "任务被中断，可以重新执行")
            }
        }
    }

    private companion object {
        val LIVE_STATES = setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING, WorkInfo.State.BLOCKED)
    }
}
