package com.startrails.feature.stacker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.startrails.core.designsystem.component.SectionCard
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.model.StackJob
import com.startrails.core.model.displayName

@Composable
internal fun HomeScreen(
    state: StackerUiState,
    onPickPhotos: () -> Unit,
    onPickFolder: () -> Unit,
    onResume: () -> Unit,
    onOpenJob: (StackJob) -> Unit,
) {
    val activeJob = state.job?.takeIf { it.status.isActive }

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "StarTrails",
                subtitle = "星轨合成",
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
                    .padding(top = Spacing.xs, bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                HeroPanel()

                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Button(
                        onClick = onPickPhotos,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Layout.primaryButtonHeight),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text("选择照片", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onPickFolder,
                        modifier = Modifier.fillMaxWidth().heightIn(min = Layout.actionButtonHeight),
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text("选择文件夹", style = MaterialTheme.typography.labelLarge)
                    }
                }

                AnimatedVisibility(
                    visible = state.resumableJob != null,
                    enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(180)),
                ) {
                    state.resumableJob?.let { draft ->
                        ResumeCard(draft = draft, onResume = onResume)
                    }
                }

                AnimatedVisibility(
                    visible = activeJob != null,
                    enter = fadeIn(tween(220)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(120)) + shrinkVertically(tween(180)),
                ) {
                    activeJob?.let { job ->
                        ActiveJobCard(
                            job = job,
                            progress = state.progress,
                            total = state.total,
                            onOpen = { onOpenJob(job) },
                        )
                    }
                }

                WorkflowCard()
                PrivacyNote()
            }
        }
    }
}

/** 三步流程说明，兼顾新用户引导与首页纵向留白的平衡。 */
@Composable
private fun WorkflowCard() {
    SectionCard(title = "基本操作") {
        WorkflowStep(1, "选择照片", "从相册多选，或选择整个文件夹")
        WorkflowStep(2, "检查与参数", "确认照片尺寸一致，选择叠加模式")
        WorkflowStep(3, "合成与保存", "后台合成，保存到相册")
    }
}

@Composable
private fun WorkflowStep(index: Int, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(28.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 首页顶部主视觉：容器色渐变 + 星点与星轨弧线装饰。
 * 只使用 colorScheme 的容器角色，动态配色下同样协调。
 */
@Composable
private fun HeroPanel() {
    val scheme = MaterialTheme.colorScheme
    val onContainer = scheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(scheme.primaryContainer, scheme.secondaryContainer),
                ),
            )
            .drawBehind { drawStarTrail(color = onContainer) },
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Surface(
                shape = CircleShape,
                color = onContainer.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Text(
                text = "把星光串成光轨",
                style = MaterialTheme.typography.headlineSmall,
                color = onContainer,
            )
            Text(
                text = "选择固定机位连续拍摄的照片合成星轨",
                style = MaterialTheme.typography.bodyMedium,
                color = onContainer.copy(alpha = 0.78f),
            )
        }
    }
}

/** 低透明度星点与弧线，营造夜空氛围但不干扰文字阅读。 */
private fun DrawScope.drawStarTrail(color: Color) {
    val arcStroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val arcColor = color.copy(alpha = 0.14f)
    drawArc(
        color = arcColor,
        startAngle = 200f,
        sweepAngle = 62f,
        useCenter = false,
        topLeft = Offset(size.width * 0.45f, -size.height * 0.45f),
        size = Size(size.width * 0.85f, size.height * 1.25f),
        style = arcStroke,
    )
    drawArc(
        color = arcColor.copy(alpha = 0.7f),
        startAngle = 208f,
        sweepAngle = 52f,
        useCenter = false,
        topLeft = Offset(size.width * 0.55f, -size.height * 0.3f),
        size = Size(size.width * 0.7f, size.height * 1.05f),
        style = arcStroke,
    )

    val starColor = color.copy(alpha = 0.55f)
    listOf(
        0.06f to 0.16f,
        0.14f to 0.78f,
        0.30f to 0.10f,
        0.42f to 0.88f,
        0.58f to 0.20f,
        0.70f to 0.72f,
        0.84f to 0.12f,
        0.92f to 0.46f,
        0.52f to 0.52f,
    ).forEachIndexed { index, (x, y) ->
        drawCircle(
            color = starColor,
            radius = if (index % 3 == 0) 2.dp.toPx() else 1.2.dp.toPx(),
            center = Offset(size.width * x, size.height * y),
        )
    }
}

@Composable
private fun ResumeCard(draft: StackJob, onResume: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "继续上次编辑",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "${draft.frameCount} 张照片 · ${draft.sourceLabel ?: "未命名来源"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            TextButton(onClick = onResume) {
                Text("继续")
                Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ActiveJobCard(
    job: StackJob,
    progress: Int,
    total: Int,
    onOpen: () -> Unit,
) {
    val fraction = if (total > 0) progress.toFloat() / total else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 450),
        label = "homeProgress",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = "叠加任务进行中",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$progress/$total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            LinearProgressIndicator(
                progress = { animatedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                strokeCap = StrokeCap.Round,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = job.blendMode.displayName() + " · 已合成 ${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpen) {
                    Text("查看进度")
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PrivacyNote() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = "数据仅在本地存储和处理",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )
    }
}
