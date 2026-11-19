package com.startrails.feature.stacker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startrails.core.designsystem.component.BottomActionBar
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.component.SupportText
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing

@Composable
internal fun ProcessingScreen(
    state: StackerUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val fraction = if (state.total > 0) state.progress.toFloat() / state.total else 0f
    val stopped = state.job?.status?.isTerminal == true
    val succeeded = state.job?.status?.name == "SUCCEEDED"
    val displayFraction = if (succeeded) 1f else fraction
    val animatedFraction by animateFloatAsState(
        targetValue = displayFraction,
        animationSpec = tween(durationMillis = 500),
        label = "stackProgress",
    )
    val percent = (displayFraction * 100).toInt()

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = if (stopped) "任务已结束" else "正在叠加",
                subtitle = if (stopped) null else "第 3 步 · 共 3 步",
                onBack = onBack,
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
                        .widthIn(max = 220.dp)
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
