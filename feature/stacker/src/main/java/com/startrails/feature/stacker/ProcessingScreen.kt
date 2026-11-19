package com.startrails.feature.stacker

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startrails.core.designsystem.component.BottomActionBar
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.component.SupportText
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.model.JobStatus
import kotlinx.coroutines.delay

/** 合成流程的三个可见阶段，与 Worker 的状态机一一对应。 */
private enum class ProcessingStage(val label: String) {
    DECODE("解码校验"),
    STACK("星轨合成"),
    ENCODE("编码保存"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProcessingScreen(
    state: StackerUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val fraction = if (state.total > 0) state.progress.toFloat() / state.total else 0f
    val stopped = state.job?.status?.isTerminal == true
    val succeeded = state.job?.status == JobStatus.SUCCEEDED
    val displayFraction = if (succeeded) 1f else fraction
    val animatedFraction by animateFloatAsState(
        targetValue = displayFraction,
        animationSpec = tween(durationMillis = 500),
        label = "stackProgress",
    )
    val percent = (displayFraction * 100).toInt()
    val stage = remember(state.job?.status, state.progress, state.total) { currentStage(state) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    // 任务运行期间每秒刷新已耗时；结束后改用任务记录的真实耗时。
    var elapsedMs by remember(state.job?.id) { mutableLongStateOf(state.job?.elapsedMs ?: 0L) }
    LaunchedEffect(stopped, state.job?.id) {
        if (stopped) {
            state.job?.elapsedMs?.takeIf { it > 0 }?.let { elapsedMs = it }
            return@LaunchedEffect
        }
        val start = System.currentTimeMillis()
        while (true) {
            elapsedMs = System.currentTimeMillis() - start
            delay(1_000)
        }
    }

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = if (stopped) "任务已结束" else "正在叠加",
                subtitle = if (stopped) null else "第 3 步 · 共 3 步",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            BottomActionBar {
                if (stopped) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Layout.primaryButtonHeight),
                    ) {
                        Text("重新执行")
                    }
                    SupportText(
                        text = "任务停止后不会修改任何原始照片。",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Layout.actionButtonHeight),
                    ) {
                        Text("取消任务")
                    }
                    SupportText(
                        text = "离开页面不会停止任务，底部导航仍可正常使用。后台运行时会在通知栏显示进度。",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = ScreenContentInsets,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 208.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeWidth = 12.dp,
                        strokeCap = StrokeCap.Round,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percent%",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${state.progress}/${state.total} 帧",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // 三段式阶段指示：让长耗时过程有明确的"现在到哪一步"。
                StageIndicator(current = stage)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        text = if (stopped) {
                            state.message ?: "任务已结束"
                        } else {
                            "正在合成星轨"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (!stopped) {
                        Text(
                            text = state.currentName.ifBlank { "正在准备照片…" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ElapsedLabel(
                        elapsedMs = elapsedMs,
                        stopped = stopped,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Text(
                        text = if (stopped) {
                            "可以重新执行，或返回检查照片。"
                        } else {
                            "逐帧读取、逐帧合成，不会一次性解码全部照片。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun currentStage(state: StackerUiState): ProcessingStage {
    val status = state.job?.status
    return when {
        status == JobStatus.SAVING || status == JobStatus.SUCCEEDED -> ProcessingStage.ENCODE
        status == JobStatus.RUNNING && state.total > 0 && state.progress >= state.total ->
            ProcessingStage.ENCODE

        status == JobStatus.RUNNING -> ProcessingStage.STACK
        else -> ProcessingStage.DECODE
    }
}

@Composable
private fun StageIndicator(current: ProcessingStage, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProcessingStage.entries.forEachIndexed { index, stage ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(16.dp),
                )
            }
            val isActive = stage == current
            val isDone = stage.ordinal < current.ordinal
            val container by animateColorAsState(
                targetValue = when {
                    isActive -> MaterialTheme.colorScheme.primaryContainer
                    isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = tween(durationMillis = 260),
                label = "stageContainer",
            )
            val content = when {
                isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                isDone -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            }
            Surface(shape = CircleShape, color = container) {
                Text(
                    text = stage.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ElapsedLabel(elapsedMs: Long, stopped: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            imageVector = Icons.Filled.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = if (stopped) {
                "总耗时 ${formatElapsed(elapsedMs)}"
            } else {
                "已用时 ${formatElapsed(elapsedMs)}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 秒级精度足够，避免每帧刷新导致的数字跳动。 */
private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "$minutes 分 ${seconds} 秒" else "$seconds 秒"
}
