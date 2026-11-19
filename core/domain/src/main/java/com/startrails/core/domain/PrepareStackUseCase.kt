package com.startrails.core.domain

import android.content.Context
import android.net.Uri
import com.startrails.core.data.FrameSourceRepository
import com.startrails.core.data.StackJobRepository
import com.startrails.core.imaging.FrameMetadataReader
import com.startrails.core.imaging.ImagingException
import com.startrails.core.model.BlendMode
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame
import com.startrails.core.model.JobStatus
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.StackJob
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PreparedStack(
    val job: StackJob,
    val frames: List<InputFrame>,
)

/** 只读取尺寸元数据并检查权限/内存/存储；不解码完整像素。 */
class PrepareStackUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobRepository: StackJobRepository,
    private val frameSourceRepository: FrameSourceRepository,
) {
    suspend operator fun invoke(
        sources: List<Uri>,
        recursive: Boolean,
        mode: BlendMode,
        format: OutputFormat,
        quality: Int,
        sourceLabel: String?,
    ): PreparedStack = withContext(Dispatchers.IO) {
        val discovered = frameSourceRepository.discover(sources, recursive)
        if (discovered.isEmpty()) {
            throw DomainFailure(ErrorCode.UNSUPPORTED_FORMAT, "没有找到支持的图片格式")
        }
        if (discovered.size < 2) throw DomainFailure.notEnoughFrames()

        val frames = mutableListOf<InputFrame>()
        var baseWidth = 0
        var baseHeight = 0
        discovered.forEachIndexed { index, frame ->
            if (!frameSourceRepository.isReadable(frame.uri)) {
                throw DomainFailure.permission(frame.displayName)
            }
            val metadata = try {
                FrameMetadataReader.read(context.contentResolver, frame.uri, frame.displayName)
            } catch (e: ImagingException) {
                throw DomainFailure(e.code, e.message ?: "读取照片失败", e)
            }

            if (index == 0) {
                baseWidth = metadata.width
                baseHeight = metadata.height
            }
            val mismatch = metadata.width != baseWidth || metadata.height != baseHeight
            frames.add(
                InputFrame(
                    jobId = "",
                    uri = frame.uri.toString(),
                    documentKey = frame.documentKey,
                    displayName = frame.displayName,
                    sourceOrder = frame.sourceOrder,
                    frameOrder = index,
                    mimeType = metadata.mimeType ?: frame.mimeType,
                    width = metadata.width,
                    height = metadata.height,
                    sizeBytes = metadata.sizeBytes,
                    issueCode = if (mismatch) ErrorCode.DIMENSION_MISMATCH else null,
                    issueDetail = if (mismatch) {
                        "基准 ${baseWidth}×${baseHeight}，本图 ${metadata.width}×${metadata.height}"
                    } else null,
                ),
            )
        }

        checkResources(mode, baseWidth, baseHeight)

        val job = jobRepository.createDraft(mode, format, quality, recursive, sourceLabel).copy(
            status = JobStatus.VALIDATING,
            frameCount = frames.size,
            width = baseWidth,
            height = baseHeight,
        )
        jobRepository.save(job)
        jobRepository.saveFrames(job.id, frames)
        PreparedStack(job, frames)
    }

    /**
     * 内存/存储检查。峰值出现在“解码位图 + 转正位图 + RGB 缓冲 + 累加器”同时存在时，
     * 因此按实际内存模型估算，而不是固定的小系数。
     */
    private fun checkResources(mode: BlendMode, width: Int, height: Int) {
        val accumulator = if (mode == BlendMode.AVERAGE) 4L else 1L
        val bytesPerPixel = DECODE_BYTES_PER_PIXEL + accumulator
        val required = width.toLong() * height.toLong() * bytesPerPixel
        val runtime = Runtime.getRuntime()
        val available = (runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())) * 0.6
        if (required > 0 && required > available) throw DomainFailure.memory()

        // 暂存文件写在内部存储 files/staging，检查的也必须是同一块空间。
        val free = context.filesDir.usableSpace
        val estimatedOutput = width.toLong() * height.toLong() * 3L + OUTPUT_MARGIN_BYTES
        if (free > 0 && estimatedOutput > free) throw DomainFailure.storage()
    }

    private companion object {
        /** 解码位图 4 B/px + 转正位图 4 B/px + RGB 缓冲 3 B/px。 */
        const val DECODE_BYTES_PER_PIXEL = 11L
        const val OUTPUT_MARGIN_BYTES = 8L * 1024 * 1024
    }
}
