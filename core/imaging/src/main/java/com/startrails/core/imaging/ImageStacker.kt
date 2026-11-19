package com.startrails.core.imaging

import android.content.ContentResolver
import com.startrails.core.model.BlendMode
import com.startrails.core.model.InputFrame

/** 串起解码、尺寸校验和逐帧累加；回调返回 false 表示协作式取消。 */
class ImageStacker(
    private val resolver: ContentResolver,
    private val engine: StackingEngine,
) {

    suspend fun stack(
        mode: BlendMode,
        frames: List<InputFrame>,
        onProgress: suspend (processed: Int, name: String) -> Boolean,
    ): FramePixels {
        if (frames.isEmpty()) throw ImagingException.Decode("(无输入)")

        val first = ImageDecoder.decode(resolver, android.net.Uri.parse(frames[0].uri))
        val accumulator = engine.create(mode, first)
        if (!onProgress(1, frames[0].displayName)) throw ImagingException.Cancelled()

        for (index in 1 until frames.size) {
            val frame = frames[index]
            // ImageDecoder 内部已把 SecurityException 包装成 PermissionLost。
            val pixels = ImageDecoder.decode(resolver, android.net.Uri.parse(frame.uri))

            if (pixels.width != accumulator.width || pixels.height != accumulator.height) {
                throw ImagingException.DimensionMismatch(
                    first = frames[0].displayName,
                    firstSize = "${accumulator.width}×${accumulator.height}",
                    second = frame.displayName,
                    secondSize = "${pixels.width}×${pixels.height}",
                )
            }
            if (!onProgress(index + 1, frame.displayName)) throw ImagingException.Cancelled()
            accumulator.add(pixels)
        }
        return accumulator.result()
    }
}
