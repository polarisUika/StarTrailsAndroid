package com.startrails.core.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.startrails.core.data.ResultRepository
import com.startrails.core.data.StackJobRepository
import com.startrails.core.imaging.ImageEncoder
import com.startrails.core.imaging.ImageStacker
import com.startrails.core.imaging.ImagingException
import com.startrails.core.imaging.StackingEngine
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.JobStatus
import com.startrails.core.model.StackJob
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 使用官方 @HiltWorker 注入，由 HiltWorkerFactory 创建。
 * 依赖只能用 @Singleton 或无作用域的绑定。
 */
@HiltWorker
class StackWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val jobs: StackJobRepository,
    private val results: ResultRepository,
    private val engine: StackingEngine,
) : CoroutineWorker(context, params) {

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(0, 0, "准备中")

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(StackWork.KEY_JOB_ID) ?: return Result.failure()
        val stored = jobs.getJob(jobId) ?: return Result.failure()
        if (stored.status == JobStatus.CANCELLING || stored.status == JobStatus.CANCELLED) {
            // 入队后立刻被取消：不要再进入 RUNNING，直接落到终态。
            withContext(NonCancellable) { jobs.markCancelled(jobId) }
            return Result.failure()
        }

        val startedAt = System.currentTimeMillis()
        val job = stored.copy(status = JobStatus.RUNNING, workId = id.toString())
        jobs.save(job)
        setForeground(foregroundInfo(0, job.frameCount, "开始处理"))

        var staging: File? = null
        return try {
            val frames = jobs.getFrames(jobId)
            val stacker = ImageStacker(applicationContext.contentResolver, engine)
            val pixels = stacker.stack(job.blendMode, frames) { processed, name ->
                // 只更新进度字段，避免把并发写入的 CANCELLING 覆盖回 RUNNING。
                jobs.updateProgress(jobId, processed)
                setProgress(workDataOf(StackWork.KEY_CURRENT_NAME to name))
                setForeground(foregroundInfo(processed, frames.size, name))
                !isStopped
            }

            // 只切状态，保留上面逐帧写入的进度计数。
            jobs.updateStatus(jobId, JobStatus.SAVING)
            val fileName = "star-trails-${job.id.take(8)}.${job.outputFormat.extension}"
            val file = results.stagingFile(fileName)
            staging = file
            withContext(Dispatchers.IO) {
                file.outputStream().use { ImageEncoder.encode(pixels, job.outputFormat, job.quality, it) }
                ImageEncoder.writeMarker(file, job.outputFormat)
            }
            val outputUri = results.saveToGallery(file, job)

            val finished = job.copy(
                status = JobStatus.SUCCEEDED,
                processedCount = frames.size,
                outputUri = outputUri.toString(),
                stagingPath = file.absolutePath,
                elapsedMs = System.currentTimeMillis() - startedAt,
                errorCode = null,
                errorMessage = null,
            )
            jobs.save(finished)
            setProgress(workDataOf(StackWork.KEY_CURRENT_NAME to "叠加完成"))
            setForeground(foregroundInfo(frames.size, frames.size, "叠加完成"))
            Result.success(Data.Builder().putString(StackWork.KEY_JOB_ID, jobId).build())
        } catch (e: CancellationException) {
            // 取消不是失败：在不可取消的作用域里清理暂存文件并写回 CANCELLED。
            withContext(NonCancellable) {
                discard(staging, job)
                jobs.markCancelled(jobId)
            }
            throw e
        } catch (e: ImagingException) {
            fail(job, staging, e.code, e.message)
        } catch (e: SecurityException) {
            fail(job, staging, ErrorCode.URI_PERMISSION_LOST, "照片访问权限已失效，请重新选择")
        } catch (e: OutOfMemoryError) {
            fail(job, staging, ErrorCode.INSUFFICIENT_MEMORY, "图片过大，请减少照片数量")
        } catch (e: Exception) {
            fail(job, staging, ErrorCode.OUTPUT_FAILED, "叠加失败，可尝试重新执行")
        }
    }

    private suspend fun fail(
        job: StackJob,
        staging: File?,
        code: ErrorCode,
        message: String?,
    ): Result = withContext(NonCancellable) {
        discard(staging, job)
        if (code == ErrorCode.CANCELLED) {
            jobs.markCancelled(job.id)
        } else {
            jobs.markFailed(job.id, code, message)
        }
        Result.failure(
            Data.Builder()
                .putString(StackWork.KEY_JOB_ID, job.id)
                .putString(StackWork.KEY_ERROR_CODE, code.key)
                .build(),
        )
    }

    /** 删除本次运行的暂存文件，以及上一次残留记录的暂存文件。 */
    private fun discard(staging: File?, job: StackJob) {
        staging?.let { runCatching { it.delete() } }
        job.stagingPath?.let { path -> runCatching { File(path).delete() } }
    }

    private fun foregroundInfo(processed: Int, total: Int, name: String): ForegroundInfo {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(StackWork.CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        StackWork.CHANNEL_ID,
                        "叠加任务",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, StackWork.CHANNEL_ID)
            .setContentTitle("正在叠加星轨")
            .setContentText(if (total > 0) "正在处理 $processed/$total · $name" else name)
            .setProgress(if (total > 0) total else 0, processed, total <= 0)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "取消任务", cancelIntent)
            .build()
        // Android 14+ 要求 ForegroundInfo 显式声明前台服务类型，
        // 否则会抛 InvalidForegroundServiceTypeException。叠加属于数据处理。
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                StackWork.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(StackWork.NOTIFICATION_ID, notification)
        }
    }
}
