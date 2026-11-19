package com.startrails.core.domain

import android.content.Context
import android.net.Uri
import com.startrails.core.data.ExportException
import com.startrails.core.data.ResultRepository
import com.startrails.core.data.StackJobRepository
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.StackJob
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class SaveResultUseCase @Inject constructor(
    private val jobs: StackJobRepository,
    private val results: ResultRepository,
) {
    /**
     * [target] 为 null 表示保存到相册，非 null 表示另存到用户选择的位置。
     * 另存不会改写作品自身的 [StackJob.outputUri]，否则删除作品时只能删掉导出副本，
     * 相册里那一份会变成孤儿文件。
     */
    suspend operator fun invoke(jobId: String, target: Uri?, createdByUs: Boolean): StackJob {
        val job = jobs.getJob(jobId) ?: throw DomainFailure(ErrorCode.UNKNOWN, "任务不存在")
        val staging = job.stagingPath?.let { File(it) }
        if (staging == null || !staging.exists()) {
            throw DomainFailure(ErrorCode.OUTPUT_FAILED, "临时结果已丢失，请重新执行任务")
        }
        try {
            if (target != null) {
                results.exportTo(staging, target, createdByUs)
                return job
            }
            // 叠加完成时 Worker 已自动写入相册，这里再写一次只会产生重复文件。
            if (!job.outputUri.isNullOrBlank()) return job
            val uri = results.saveToGallery(staging, job)
            val saved = job.copy(outputUri = uri.toString())
            jobs.save(saved)
            return saved
        } catch (e: ExportException) {
            throw DomainFailure(ErrorCode.OUTPUT_FAILED, e.message ?: "导出失败", e)
        }
    }
}

class ShareResultUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val results: ResultRepository,
) {
    suspend operator fun invoke(job: StackJob): Uri = withContext(Dispatchers.IO) {
        val name = "share-${job.id.take(8)}.${job.outputFormat.extension}"
        val target = results.shareCopy(name)
        val staging = job.stagingPath?.let { File(it) }
        if (staging != null && staging.exists()) {
            staging.copyTo(target, overwrite = true)
        } else {
            val uri = job.outputUri?.let { Uri.parse(it) }
                ?: throw DomainFailure(ErrorCode.OUTPUT_FAILED, "结果文件不可用")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw DomainFailure(ErrorCode.URI_PERMISSION_LOST, "结果文件不可用")
        }
        results.shareUri(target)
    }
}

class DeleteResultUseCase @Inject constructor(
    private val jobs: StackJobRepository,
    private val results: ResultRepository,
) {
    suspend operator fun invoke(jobId: String) {
        val job = jobs.getJob(jobId) ?: return
        job.outputUri?.let { results.delete(it) }
        job.stagingPath?.let { runCatching { File(it).delete() } }
        jobs.deleteJob(jobId)
    }
}
