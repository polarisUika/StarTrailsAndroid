package com.startrails.feature.stacker

import android.net.Uri
import android.text.format.Formatter
import android.view.View
import android.view.ViewParent
import android.view.Window
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.startrails.core.designsystem.component.SectionCard
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.PhotoOverlayContent
import com.startrails.core.designsystem.theme.PhotoOverlayScrim
import com.startrails.core.designsystem.theme.PreviewSurfaceNeutral
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.designsystem.theme.findActivity
import com.startrails.core.model.displayName
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    var menuOpen by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    val model: Any? = job.outputUri?.let { Uri.parse(it) } ?: job.stagingPath?.let { File(it) }
    // 文件大小由 ViewModel 在 IO 线程读取，这里避免在主线程做同步磁盘访问。
    val fileSize = state.resultSizeBytes
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val configuration = LocalConfiguration.current
    val previewHeight = remember(
        configuration.screenWidthDp,
        configuration.screenHeightDp,
        job.width,
        job.height,
    ) {
        previewHeightFor(
            screenWidthDp = configuration.screenWidthDp.dp,
            screenHeightDp = configuration.screenHeightDp.dp,
            imageWidth = job.width,
            imageHeight = job.height,
        )
    }

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "星轨作品",
                subtitle = "合成完成",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    // 低频且有破坏性的操作收进溢出菜单，避免与分享/导出争夺注意力。
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "更多操作")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("新建任务") },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                            onClick = {
                                menuOpen = false
                                onNewTask()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除作品") },
                            leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                            colors = MenuDefaults.itemColors(
                                textColor = MaterialTheme.colorScheme.error,
                                leadingIconColor = MaterialTheme.colorScheme.error,
                            ),
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    .padding(top = Spacing.xs, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                ResultPreview(
                    model = model,
                    height = previewHeight,
                    onExpand = { fullscreen = true },
                )

                SectionCard(title = "作品信息") {
                    SpecGrid(
                        specs = listOf(
                            "尺寸" to "${job.width}×${job.height}",
                            "帧数" to "${job.frameCount} 帧",
                            "模式" to job.blendMode.displayName(),
                            "格式" to job.outputFormat.displayName(),
                            "耗时" to formatDuration(job.elapsedMs),
                            "大小" to if (fileSize > 0) {
                                Formatter.formatShortFileSize(context, fileSize)
                            } else {
                                "未知"
                            },
                        ),
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
                        // 叠加完成时 Worker 已自动写入相册，这里只做轻量状态说明。
                        SavedStatus()
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        FilledTonalButton(
                            onClick = onExport,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = Layout.actionButtonHeight),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.xs))
                            Text("导出文件")
                        }
                        Button(
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
                }
            }
        }

        if (fullscreen && model != null) {
            FullscreenPreview(model = model, onDismiss = { fullscreen = false })
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
 * 预览容器高度：按作品宽高比推导，再收敛到屏幕可用范围内。
 * 横幅作品不会被压成细条，竖幅作品也不会把整页挤出首屏。
 */
private fun previewHeightFor(
    screenWidthDp: Dp,
    screenHeightDp: Dp,
    imageWidth: Int,
    imageHeight: Int,
): Dp {
    val ratio = if (imageWidth > 0 && imageHeight > 0) {
        imageWidth.toFloat() / imageHeight
    } else {
        4f / 3f
    }
    val contentWidth = minOf(screenWidthDp, Layout.contentMaxWidth) - Spacing.lg * 2
    val maxHeight = (screenHeightDp * 0.52f).coerceIn(200.dp, 460.dp)
    return (contentWidth / ratio).coerceIn(180.dp, maxHeight)
}

/** 作品主体：固定中性深色底，点击进入沉浸式查看。 */
@Composable
private fun ResultPreview(
    model: Any?,
    height: Dp,
    onExpand: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(MaterialTheme.shapes.large)
            .background(PreviewSurfaceNeutral)
            .then(
                if (model != null) {
                    Modifier.pointerInput(Unit) { detectTapGestures(onTap = { onExpand() }) }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = "星轨作品大图，点击全屏查看",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Spacing.md)
                    .clip(CircleShape)
                    .background(PhotoOverlayScrim),
            ) {
                IconButton(
                    onClick = onExpand,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = PhotoOverlayContent),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fullscreen,
                        contentDescription = "全屏查看",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        } else {
            Text(
                text = "预览不可用",
                color = PhotoOverlayContent.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 沉浸式全屏查看：独立窗口铺满屏幕并隐藏系统栏，只保留退出与缩放控件。
 * 退出时恢复系统栏，避免影响返回后的页面布局。
 */
@Composable
private fun FullscreenPreview(model: Any, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        val dialogView = LocalView.current
        val dialogWindow = remember(dialogView) { dialogView.findDialogWindow() }
        DisposableEffect(dialogWindow) {
            val controller = dialogWindow?.let {
                WindowCompat.getInsetsController(it, dialogView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PreviewSurfaceNeutral),
        ) {
            AsyncImage(
                model = model,
                contentDescription = "星轨作品全屏预览，可双指缩放",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
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
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )

            IconButton(
                onClick = onDismiss,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PhotoOverlayScrim,
                    contentColor = PhotoOverlayContent,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Spacing.lg),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "退出全屏")
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.xl)
                    .clip(CircleShape)
                    .background(PhotoOverlayScrim)
                    .padding(horizontal = Spacing.xs),
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
                    modifier = Modifier.widthIn(min = 48.dp),
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
}

/** 已自动保存时的轻量状态标签，不再占用一个主按钮的位置。 */
@Composable
private fun SavedStatus() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = "已自动保存到相册 Pictures/StarTrails",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 找到对话框自己的 Window。
 *
 * Compose 的 Dialog 内容挂在 DialogLayout 上，该视图实现了 DialogWindowProvider，
 * 只有拿到它才能正确隐藏对话框窗口的系统栏；兜底回退到 Activity 窗口。
 */
private fun View.findDialogWindow(): Window? {
    var parent: ViewParent? = this.parent
    while (parent != null) {
        if (parent is DialogWindowProvider) return parent.window
        parent = parent.parent
    }
    return context.findActivity()?.window
}

/** 2×3 参数网格：值在上、标签在下，用网格替代六行竖排以压低页面高度。 */
@Composable
private fun SpecGrid(specs: List<Pair<String, String>>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        specs.chunked(2).forEach { rowSpecs ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                rowSpecs.forEach { (label, value) ->
                    SpecCell(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (rowSpecs.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpecCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 不足一分钟显示一位小数秒，超过一分钟显示"分 + 秒"，避免整除丢失精度。 */
private fun formatDuration(elapsedMs: Long): String {
    if (elapsedMs <= 0) return "0 秒"
    if (elapsedMs < 60_000) {
        return String.format(Locale.getDefault(), "%.1f 秒", elapsedMs / 1000.0)
    }
    val minutes = elapsedMs / 60_000
    val seconds = (elapsedMs % 60_000) / 1000
    return "$minutes 分 $seconds 秒"
}
