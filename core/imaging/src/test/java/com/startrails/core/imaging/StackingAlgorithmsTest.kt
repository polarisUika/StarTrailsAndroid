package com.startrails.core.imaging

import com.google.common.truth.Truth.assertThat
import com.startrails.core.model.BlendMode
import org.junit.Test

class StackingAlgorithmsTest {

    private val engine = KotlinStackingEngine()

    private fun frame(vararg bytes: Int): FramePixels =
        FramePixels(bytes.size / 3, 1, ByteArray(bytes.size) { bytes[it].toByte() })

    @Test
    fun lighten_takesPerChannelMaximum() {
        val accumulator = engine.create(BlendMode.LIGHTEN, frame(10, 20, 30, 40, 50, 60))
        accumulator.add(frame(60, 5, 35, 20, 90, 10))
        assertThat(accumulator.result().rgb.map { it.toInt() and 0xFF })
            .containsExactly(60, 20, 35, 40, 90, 60)
            .inOrder()
    }

    @Test
    fun screen_matchesDesktopFormula() {
        assertThat(screen(128, 128)).isEqualTo(192)
        assertThat(screen(0, 0)).isEqualTo(0)
        assertThat(screen(255, 255)).isEqualTo(255)
        assertThat(screen(10, 255)).isEqualTo(255)

        val accumulator = engine.create(BlendMode.SCREEN, frame(128, 128, 128))
        accumulator.add(frame(128, 128, 128))
        assertThat(accumulator.result().rgb.map { it.toInt() and 0xFF })
            .containsExactly(192, 192, 192)
            .inOrder()
    }

    @Test
    fun average_usesRoundHalfToEvenLikeNumpyRint() {
        assertThat(rint(0.5f)).isEqualTo(0)
        assertThat(rint(1.5f)).isEqualTo(2)
        assertThat(rint(2.5f)).isEqualTo(2)
        assertThat(rint(3.5f)).isEqualTo(4)

        val accumulator = engine.create(BlendMode.AVERAGE, frame(0, 1, 2))
        accumulator.add(frame(1, 2, 3))
        // 0.5→0、1.5→2、2.5→2
        assertThat(accumulator.result().rgb.map { it.toInt() and 0xFF })
            .containsExactly(0, 2, 2)
            .inOrder()
    }

    @Test(expected = IllegalArgumentException::class)
    fun accumulator_rejectsDimensionMismatch() {
        engine.create(BlendMode.LIGHTEN, frame(1, 2, 3))
            .add(frame(1, 2, 3, 4, 5, 6))
    }

    @Test
    fun singleFrame_resultEqualsInputForEveryMode() {
        BlendMode.entries.forEach { mode ->
            val input = frame(7, 13, 200)
            assertThat(engine.create(mode, input).result().rgb.map { it.toInt() and 0xFF })
                .containsExactly(7, 13, 200)
                .inOrder()
        }
    }
}
