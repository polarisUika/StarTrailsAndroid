package com.startrails.feature.stacker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.startrails.core.designsystem.component.BottomActionBar
import com.startrails.core.designsystem.component.InfoRow
import com.startrails.core.designsystem.component.SectionCard
import com.startrails.core.designsystem.component.SectionHeader
import com.startrails.core.designsystem.component.SegmentedChoice
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.component.SupportText
import com.startrails.core.designsystem.component.SwitchRow
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.model.BlendMode
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.QualityLevel
import com.startrails.core.model.description
import com.startrails.core.model.displayName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StackSettingsScreen(
    state: StackerUiState,
    onMode: (BlendMode) -> Unit,
    onFormat: (OutputFormat) -> Unit,
    onQuality: (Int) -> Unit,
    onRecursive: (Boolean) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    var formatMenuExpanded by remember { mutableStateOf(false) }
    val folderSource = state.job?.sourceLabel == "文件夹来源"
    val canStart = state.frames.size >= 2 && state.frames.none { it.issueCode != null || it.width == 0 }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "叠加参数",
                subtitle = "第 2 步 · 共 3 步",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            BottomActionBar {
                Button(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Layout.primaryButtonHeight),
                ) {
                    Text("开始叠加", style = MaterialTheme.typography.labelLarge)
                }
                SupportText("合成期间可在其他页面继续使用，任务不会中断。")
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
                    .widthIn(max = Layout.formMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SectionHeader(
                    title = "叠加模式",
                    description = "不同算法适合不同的星轨效果",
                )
                SectionCard {
                    SegmentedChoice(
                        options = BlendMode.entries.toList(),
                        selected = state.blendMode,
                        onSelect = onMode,
                        label = { it.displayName() },
                    )
                    Text(
                        text = state.blendMode.description(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionHeader(
                    title = "输出设置",
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                SectionCard {
                    ExposedDropdownMenuBox(
                        expanded = formatMenuExpanded,
                        onExpandedChange = { formatMenuExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = state.outputFormat.displayName(),
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            label = { Text("输出格式") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(formatMenuExpanded)
                            },
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = formatMenuExpanded,
                            onDismissRequest = { formatMenuExpanded = false },
                        ) {
                            OutputFormat.entries.forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.displayName()) },
                                    onClick = {
                                        onFormat(format)
                                        formatMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = state.outputFormat.supportsQuality,
                        enter = fadeIn(tween(200)) + expandVertically(tween(240)),
                        exit = fadeOut(tween(120)) + shrinkVertically(tween(180)),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                text = "输出质量",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            SegmentedChoice(
                                options = QualityLevel.entries.toList(),
                                selected = QualityLevel.fromQuality(state.quality),
                                onSelect = { onQuality(it.percent) },
                                label = { it.displayName() },
                            )
                            SupportText("仅 JPEG 与 WebP 使用该质量参数")
                        }
                    }

                    if (folderSource) {
                        SwitchRow(
                            title = "包含子文件夹",
                            description = "仅对文件夹来源生效，重新选择后才会重新扫描",
                            checked = state.recursive,
                            onCheckedChange = onRecursive,
                        )
                    }
                }

                SectionHeader(
                    title = "本次任务",
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                SectionCard {
                    InfoRow("照片帧数", "${state.frames.size} 张")
                    InfoRow(
                        "输出尺寸",
                        if ((state.job?.width ?: 0) > 0) {
                            "${state.job?.width}×${state.job?.height}"
                        } else {
                            "待检测"
                        },
                    )
                    SupportText("原片以只读方式访问。")
                }
            }
        }
    }
}
