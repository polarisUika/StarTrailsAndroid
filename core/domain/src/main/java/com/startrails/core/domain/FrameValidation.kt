package com.startrails.core.domain

import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame

/**
 * 以第 0 帧为基准重新计算所有帧的尺寸校验结果。
 * 删除或追加照片后必须重算，否则基准帧变化会让其余照片永久带着错误标记。
 */
object FrameValidation {

    fun normalize(frames: List<InputFrame>): List<InputFrame> {
        val base = frames.firstOrNull() ?: return emptyList()
        return frames.mapIndexed { index, frame ->
            val mismatch = frame.width != base.width || frame.height != base.height
            frame.copy(
                frameOrder = index,
                issueCode = if (mismatch) ErrorCode.DIMENSION_MISMATCH else null,
                issueDetail = if (mismatch) {
                    "基准 ${base.width}×${base.height}，本图 ${frame.width}×${frame.height}"
                } else {
                    null
                },
            )
        }
    }
}
