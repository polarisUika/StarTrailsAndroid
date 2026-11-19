package com.startrails.feature.gallery

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.startrails.core.designsystem.component.EmptyState
import com.startrails.core.designsystem.component.StarTrailsTopBar
import com.startrails.core.designsystem.theme.Layout
import com.startrails.core.designsystem.theme.PhotoOverlayContent
import com.startrails.core.designsystem.theme.ScreenContentInsets
import com.startrails.core.designsystem.theme.Spacing
import com.startrails.core.model.StackJob

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryRoute(onOpenStacker: () -> Unit, onOpenJob: (String) -> Unit = { onOpenStacker() }) {
    val viewModel: GalleryViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExport by remember { mutableStateOf<StackJob?>(null) }
    var pendingDelete by remember { mutableStateOf<StackJob?>(null) }
    var pendingShare by remember { mutableStateOf<StackJob?>(null) }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        if (uri != null) pendingExport?.let { viewModel.export(it, uri) }
        pendingExport = null
    }

    LaunchedEffect(state.shareUri) {
        val uri = state.shareUri ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = pendingShare?.outputFormat?.mimeType ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享星轨作品")) }
        pendingShare = null
        viewModel.consumeShare()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message, withDismissAction = true)
        viewModel.consumeMessage()
    }

    Scaffold(
        topBar = {
            StarTrailsTopBar(
                title = "作品",
                subtitle = if (state.items.isEmpty()) "合成结果会保存在这里" else "共 ${state.items.size} 个作品",
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = ScreenContentInsets,
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.items.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(Spacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = Icons.Filled.PhotoLibrary,
                        title = "还没有星轨作品",
                        description = "完成一次叠加后，作品会自动出现在这里，可随时预览、分享或导出。",
                        action = {
                            Button(onClick = onOpenStacker) { Text("去叠加照片") }
                        },
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(Layout.gridMinSize),
                    contentPadding = PaddingValues(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.items, key = { it.job.id }) { item ->
                        GalleryCard(
                            item = item,
                            onOpen = { onOpenJob(item.job.id) },
                            onShare = { pendingShare = item.job; viewModel.share(item.job) },
                            onExport = {
                                pendingExport = item.job
                                exportPicker.launch("star-trails.${item.job.outputFormat.extension}")
                            },
                            onDelete = { pendingDelete = item.job },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("删除这个作品？") },
            text = { Text("只会删除合成结果，不会影响原始照片。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(job)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryCard(
    item: GalleryItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    // previewFile 已在 ViewModel 中确认存在，缺失时回退到相册 URI。
    val model: Any? = item.previewFile ?: item.job.outputUri?.let { Uri.parse(it) }

    Card(
        onClick = { if (item.available) onOpen() },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            if (item.available && model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = "${item.modeText} 星轨作品",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "文件不可用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 底部渐变压暗，保证日期文字在任何照片上都可读
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.scrim.copy(alpha = 0.78f),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = item.dateText,
                    style = MaterialTheme.typography.labelLarge,
                    color = PhotoOverlayContent,
                )
                Text(
                    text = "${item.modeText} · ${item.sizeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PhotoOverlayContent.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.xs)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
            ) {
                IconButton(
                    onClick = { menuOpen = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = PhotoOverlayContent),
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多操作",
                        modifier = Modifier.size(20.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("打开") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                        enabled = item.available,
                        onClick = {
                            menuOpen = false
                            onOpen()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("分享") },
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                        enabled = item.available,
                        onClick = {
                            menuOpen = false
                            onShare()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("导出") },
                        leadingIcon = { Icon(Icons.Filled.Save, contentDescription = null) },
                        enabled = item.available,
                        onClick = {
                            menuOpen = false
                            onExport()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
