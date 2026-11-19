package com.startrails.feature.stacker

import android.net.Uri
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.startrails.core.designsystem.component.InfoRow
import com.startrails.core.designsystem.component.SectionCard
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.PhotoOverlayContent
import com.startrails.core.designsystem.theme.PhotoOverlayScrim
import com.startrails.core.designsystem.theme.PreviewSurfaceNeutral
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.model.displayName
import java.io.File
import java.util.Locale

@Composable
internal fun ResultScreen(
    state: StackerUiState,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onNewTask: () -> Unit,
    onBack: () -> Unit,
) {
    val job = state.job ?: return
    var confirmDelete by remember { mutableStateOf(false) }
    val model: Any? = job.outputUri?.let { Uri.parse(it) } ?: job.stagingPath?.let { File(it) }
    // 文件大小由 ViewModel 在 IO 线程读取，这里避免在主线程做同步磁盘访问。
    val fileSize = state.resultSizeBytes
    val context = LocalContext.current

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "星轨作品",
                subtitle = "合成完成",
                onBack = onBack,
            )
        },
        contentWindowInsets = ScreenContentInsets,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = Layout.contentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ZoomablePreview(model = model)

                SectionCard(title = "作品信息") {
                    InfoRow("尺寸", "${job.width}×${job.height}")
                    InfoRow("帧数", "${job.frameCount} 帧")
                    InfoRow("模式", job.blendMode.displayName())
                    InfoRow("格式", job.outputFormat.displayName())
                    InfoRow("耗时", formatDuration(job.elapsedMs))
                    InfoRow(
                        "大小",
                        if (fileSize > 0) {
                            Formatter.formatShortFileSize(context, fileSize)
                        } else {
                            "未知"
                        },
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    if (job.outputUri == null) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = Layout.primaryButtonHeight),
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Text("保存到相册", style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        // 叠加完成时 Worker 已自动写入相册，不再提供重复保存入口。
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = "已自动保存到相册 Pictures/StarTrails",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedButton(
                            onClick = onExport,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Layout.actionButtonHeight),
                        ) {
                            Text("导出文件")
                        }
                        OutlinedButton(
                            onClick = onShare,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Layout.actionButtonHeight),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text("分享")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        TextButton(
                            onClick = onNewTask,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Layout.actionButtonHeight),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text("新建任务")
                        }
                        TextButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Layout.actionButtonHeight),
                        ) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text("删除结果")
                        }
                    }
                }
            }
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                title = { Text("删除这个作品？") },
                text = { Text("只删除合成结果，不会影响任何原始照片。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmDelete = false
                            onDelete()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text("取消") }
                },
            )
        }
    }
}

/**
 * 可缩放预览：固定中性深色底，避免主题色影响对星轨对比度的判断。
 * 缩放工具条浮在图片上，不额外占用垂直空间。
 */
@Composable
private fun ZoomablePreview(model: Any?) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(MaterialTheme.shapes.large)
            .background(PreviewSurfaceNeutral)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    offset = if (scale <= 1f) Offset.Zero else offset + pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = if (scale > 1f) 1f else 2.5f
                    if (scale <= 1f) offset = Offset.Zero
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "星轨作品大图，可双指缩放",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )
        } else {
            Text(
                text = "预览不可用",
                color = PhotoOverlayContent.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.md)
                .clip(CircleShape)
                .background(PhotoOverlayScrim),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    scale = (scale - 0.5f).coerceIn(1f, 6f)
                    // 缩回 100% 时同步把平移归零，否则图片会停在偏离中心的位置。
                    if (scale <= 1f) offset = Offset.Zero
                },
                colors = IconButtonDefaults.iconButtonColors(contentColor = PhotoOverlayContent),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "缩小", modifier = Modifier.size(20.dp))
            }
            Text(
                text = "${(scale * 100).toInt()}%",
                color = PhotoOverlayContent,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 44.dp),
            )
            IconButton(
                onClick = { scale = (scale + 0.5f).coerceIn(1f, 6f) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = PhotoOverlayContent),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "放大", modifier = Modifier.size(20.dp))
            }
            TextButton(
                onClick = {
                    scale = 1f
                    offset = Offset.Zero
                },
                colors = ButtonDefaults.textButtonColors(contentColor = PhotoOverlayContent),
            ) {
                Text("重置")
            }
        }
    }
}

/** 不足一分钟显示一位小数秒，超过一分钟显示“分 + 秒”，避免整除丢失精度。 */
private fun formatDuration(elapsedMs: Long): String {
    if (elapsedMs <= 0) return "0 秒"
    if (elapsedMs < 60_000) {
        return String.format(Locale.getDefault(), "%.1f 秒", elapsedMs / 1000.0)
    }
    val minutes = elapsedMs / 60_000
    val seconds = (elapsedMs % 60_000) / 1000
    return "$minutes 分 $seconds 秒"
}
