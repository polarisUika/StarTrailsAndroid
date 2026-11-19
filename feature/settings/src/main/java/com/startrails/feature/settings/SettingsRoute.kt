package com.startrails.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startrails.core.designsystem.component.InfoRow
import com.startrails.core.designsystem.component.SectionCard
import com.startrails.core.designsystem.component.SectionHeader
import com.startrails.core.designsystem.component.SegmentedChoice
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.component.SwitchRow
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.designsystem.theme.toColorScheme
import com.startrails.core.model.AppColorScheme
import com.startrails.core.model.BlendMode
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.QualityLevel
import com.startrails.core.model.ThemeMode
import com.startrails.core.model.displayName

@Composable
fun SettingsRoute() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 版本号从已安装包读取，避免硬编码与 build.gradle 不一致。
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val darkTheme = when (state.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "设置",
            )
        },
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
                SectionHeader(title = "外观")
                SectionCard {
                    Text(text = "主题", style = MaterialTheme.typography.bodyLarge)
                    SegmentedChoice(
                        options = ThemeMode.entries.toList(),
                        selected = state.themeMode,
                        onSelect = { viewModel.setTheme(it) },
                        label = { mode ->
                            when (mode) {
                                ThemeMode.SYSTEM -> "跟随系统"
                                ThemeMode.LIGHT -> "浅色"
                                ThemeMode.DARK -> "深色"
                            }
                        },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SwitchRow(
                        title = "动态配色",
                        description = if (supportsDynamicColor) {
                            "跟随系统壁纸取色，关闭后使用下方配色方案"
                        } else {
                            "需要 Android 12 或更高版本"
                        },
                        checked = state.dynamicColor && supportsDynamicColor,
                        enabled = supportsDynamicColor,
                        onCheckedChange = { viewModel.setDynamicColor(it) },
                    )
                    if (!supportsDynamicColor || !state.dynamicColor) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        Text(text = "配色方案", style = MaterialTheme.typography.bodyLarge)
                        ColorSchemePicker(
                            selected = state.colorScheme,
                            darkTheme = darkTheme,
                            onSelect = { viewModel.setColorScheme(it) },
                        )
                    }
                }

                SectionHeader(
                    title = "任务默认参数",
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                SectionCard {
                    Text(text = "叠加模式", style = MaterialTheme.typography.bodyLarge)
                    SegmentedChoice(
                        options = BlendMode.entries.toList(),
                        selected = state.defaultBlendMode,
                        onSelect = { viewModel.setBlendMode(it) },
                        label = { it.displayName() },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    FormatSelector(
                        current = state.defaultFormat,
                        onChange = { viewModel.setFormat(it) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Text(text = "JPEG / WebP 质量", style = MaterialTheme.typography.bodyLarge)
                    SegmentedChoice(
                        options = QualityLevel.entries.toList(),
                        selected = QualityLevel.fromQuality(state.defaultQuality),
                        onSelect = { viewModel.setQuality(it.percent) },
                        label = { it.displayName() },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    SwitchRow(
                        title = "文件夹默认包含子文件夹",
                        checked = state.defaultRecursive,
                        onCheckedChange = { viewModel.setRecursive(it) },
                    )
                }

                SectionHeader(
                    title = "关于",
                    modifier = Modifier.padding(top = Spacing.sm),
                )
                SectionCard {
                    InfoRow(label = "版本", value = versionName.ifBlank { "未知" })
                }
            }
        }
    }
}

/**
 * 四套静态配色的色板选择器：三枚色点分别代表 primary / secondary / tertiary。
 * 选中态使用 secondaryContainer，保证在深浅主题下都有足够对比度。
 */
@Composable
private fun ColorSchemePicker(
    selected: AppColorScheme,
    darkTheme: Boolean,
    onSelect: (AppColorScheme) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AppColorScheme.entries.forEach { option ->
            val isSelected = option == selected
            val scheme = option.toColorScheme(darkTheme)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = option.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSelector(current: OutputFormat, onChange: (OutputFormat) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.displayName(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("默认输出格式") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OutputFormat.entries.forEach { format ->
                DropdownMenuItem(
                    text = { Text(format.displayName()) },
                    onClick = {
                        onChange(format)
                        expanded = false
                    },
                )
            }
        }
    }
}
