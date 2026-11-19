package com.startrails.feature.stacker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.startrails.core.designsystem.component.BottomActionBar
import com.startrails.core.designsystem.component.EmptyState
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.component.SupportText
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.model.InputFrame
import com.startrails.core.model.userMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewScreen(
    state: StackerUiState,
    onRemove: (InputFrame) -> Unit,
    onClear: () -> Unit,
    onAddMore: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    val hasIssue = state.frames.any { it.issueCode != null || it.width == 0 }
    val validFrames = state.frames.size >= 2 && !hasIssue
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "已选择 ${state.frames.size} 张",
                subtitle = "检查尺寸与顺序后继续",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
                actions = {
                    // 清空是低频的破坏性操作，放在顶栏与主流程按钮拉开距离，避免误触。
                    TextButton(
                        onClick = onClear,
                        enabled = state.frames.isNotEmpty(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("清空")
                    }
                },
            )
        },
        bottomBar = {
            BottomActionBar {
                when {
                    hasIssue -> SupportText(
                        text = "有照片存在问题，请移除后再继续",
                        emphasis = true,
                    )

                    state.frames.size < 2 -> SupportText(text = "至少需要两张照片")
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onAddMore,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Layout.actionButtonHeight),
                    ) {
                        Text("继续添加")
                    }
                    Button(
                        onClick = onNext,
                        enabled = validFrames,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Layout.actionButtonHeight),
                    ) {
                        Text("下一步")
                    }
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScreenContentInsets,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.frames.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.AddPhotoAlternate,
                        title = "还没有照片",
                        description = "选择固定机位连续拍摄的照片，建议20张以上。",
                        action = {
                            OutlinedButton(onClick = onAddMore) { Text("选择照片") }
                        },
                    )
                }
                return@Box
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = Spacing.sm,
                    bottom = Spacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(
                    items = state.frames,
                    key = { "${it.jobId}-${it.frameOrder}-${it.documentKey}" },
                ) { frame ->
                    FrameRow(
                        frame = frame,
                        onRemove = onRemove,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameRow(
    frame: InputFrame,
    onRemove: (InputFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasIssue = frame.issueCode != null || frame.width == 0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (hasIssue) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasIssue) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = frame.uri,
                contentDescription = "第 ${frame.frameOrder + 1} 张缩略图",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = frame.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "第 ${frame.frameOrder + 1} 张 · " +
                        if (frame.width > 0) "${frame.width}×${frame.height}" else "读取中",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (hasIssue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(Spacing.xs))
                        Text(
                            text = frame.issueDetail ?: frame.issueCode?.userMessage() ?: "尺寸与其他照片不同",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            IconButton(onClick = { onRemove(frame) }) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "移除 ${frame.displayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
