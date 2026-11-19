package com.startrails.core.domain

import com.google.common.truth.Truth.assertThat
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame
import org.junit.Test

class FrameValidationTest {

    private fun frame(order: Int, width: Int, height: Int, issue: ErrorCode? = null): InputFrame =
        InputFrame(
            jobId = "job",
            uri = "content://photo/$order",
            documentKey = "key/$order",
            displayName = "IMG_$order.jpg",
            sourceOrder = 0,
            frameOrder = order,
            width = width,
            height = height,
            issueCode = issue,
            issueDetail = issue?.let { "旧标记" },
        )

    @Test
    fun removingAbnormalFirstFrameClearsMismatchOnRemainingFrames() {
        // 第 0 帧尺寸异常，导致其余正常帧被标为不一致。
        val frames = listOf(
            frame(0, 100, 100),
            frame(1, 4000, 3000, ErrorCode.DIMENSION_MISMATCH),
            frame(2, 4000, 3000, ErrorCode.DIMENSION_MISMATCH),
        )

        val normalized = FrameValidation.normalize(frames.drop(1))

        assertThat(normalized.all { it.issueCode == null }).isTrue()
        assertThat(normalized.all { it.issueDetail == null }).isTrue()
        assertThat(normalized.map { it.frameOrder }).containsExactly(0, 1).inOrder()
    }

    @Test
    fun framesWithDifferentSizeKeepMismatchAgainstFirstFrame() {
        val normalized = FrameValidation.normalize(
            listOf(frame(0, 4000, 3000), frame(1, 4000, 3000), frame(2, 2000, 1500)),
        )

        assertThat(normalized[1].issueCode).isNull()
        assertThat(normalized[2].issueCode).isEqualTo(ErrorCode.DIMENSION_MISMATCH)
        assertThat(normalized[2].issueDetail).contains("4000×3000")
    }

    @Test
    fun emptyInputReturnsEmptyList() {
        assertThat(FrameValidation.normalize(emptyList())).isEmpty()
    }
}
