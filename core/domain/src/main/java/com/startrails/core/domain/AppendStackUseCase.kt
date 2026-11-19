package com.startrails.core.domain

import android.content.Context
import android.net.Uri
import com.startrails.core.data.FrameSourceRepository
import com.startrails.core.data.StackJobRepository
import com.startrails.core.imaging.FrameMetadataReader
import com.startrails.core.imaging.ImagingException
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 向“当前草稿”追加照片：按 documentKey 去重，保留已有帧并按新基准重算尺寸校验。
 * 与 [PrepareStackUseCase] 的区别是不会新建任务，也不会覆盖已经选好的照片。
 */
class AppendStackUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobRepository: StackJobRepository,
    private val frameSourceRepository: FrameSourceRepository,
) {
    suspend operator fun invoke(jobId: String, sources: List<Uri>, recursive: Boolean): PreparedStack =
        withContext(Dispatchers.IO) {
            val job = jobRepository.getJob(jobId)
                ?: throw DomainFailure(ErrorCode.UNKNOWN, "任务不存在")
            if (job.status.isActive) {
                throw DomainFailure(ErrorCode.UNKNOWN, "任务正在运行，无法修改照片")
            }

            val existing = jobRepository.getFrames(jobId)
            val knownKeys = existing.mapTo(HashSet()) { it.documentKey }
            val appended = existing.toMutableList()
            var added = 0

            val discovered = frameSourceRepository.discover(sources, recursive)
            if (discovered.isEmpty()) {
                throw DomainFailure(ErrorCode.UNSUPPORTED_FORMAT, "没有找到支持的图片格式")
            }
            discovered.forEach { frame ->
                if (!knownKeys.add(frame.documentKey)) return@forEach
                if (!frameSourceRepository.isReadable(frame.uri)) {
                    throw DomainFailure.permission(frame.displayName)
                }
                val metadata = try {
                    FrameMetadataReader.read(context.contentResolver, frame.uri, frame.displayName)
                } catch (e: ImagingException) {
                    throw DomainFailure(e.code, e.message ?: "读取照片失败", e)
                }
                appended.add(
                    InputFrame(
                        jobId = jobId,
                        uri = frame.uri.toString(),
                        documentKey = frame.documentKey,
                        displayName = frame.displayName,
                        sourceOrder = frame.sourceOrder + existing.size,
                        frameOrder = appended.size,
                        mimeType = metadata.mimeType ?: frame.mimeType,
                        width = metadata.width,
                        height = metadata.height,
                        sizeBytes = metadata.sizeBytes,
                    ),
                )
                added++
            }

            if (added == 0) throw DomainFailure(ErrorCode.UNKNOWN, "没有发现新的照片")

            val normalized = FrameValidation.normalize(appended)
            jobRepository.saveFrames(jobId, normalized)
            val updated = job.copy(
                frameCount = normalized.size,
                width = normalized.firstOrNull()?.width ?: 0,
                height = normalized.firstOrNull()?.height ?: 0,
            )
            jobRepository.save(updated)
            PreparedStack(updated, normalized)
        }
}
