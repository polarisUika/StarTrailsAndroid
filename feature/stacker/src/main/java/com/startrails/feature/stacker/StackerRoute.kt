package com.startrails.feature.stacker

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startrails.core.designsystem.theme.Spacing

/**
 * 叠加流程入口。
 *
 * @param isDetailMode 从作品页进入详情时为 true，返回键直接交给外层导航栈处理，
 *   不再退回已完成任务的检查页。
 * @param onNavigateBack 详情模式下的返回动作，通常是 navController.popBackStack()。
 */
@Composable
fun StackerRoute(
    isDetailMode: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
) {
    val viewModel: StackerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.pickPhotos(uris)
    }
    // “继续添加”必须追加到当前任务，否则会覆盖已选照片并留下孤儿草稿。
    val addPhotosPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.appendPhotos(uris)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.pickFolder(uri)
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
        if (uri != null) viewModel.exportTo(uri)
    }

    val navigateBack: () -> Unit = {
        if (isDetailMode && onNavigateBack != null) onNavigateBack() else viewModel.goHome()
    }

    BackHandler(enabled = state.stage != StackerStage.HOME) {
        when (state.stage) {
            StackerStage.HOME -> Unit
            // 检查页与结果页返回上一级页面，其余阶段退回检查页。
            StackerStage.REVIEW, StackerStage.RESULT -> navigateBack()
            else -> viewModel.goReview()
        }
    }

    LaunchedEffect(state.shareUri) {
        val uri = state.shareUri ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = state.job?.outputFormat?.mimeType ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享星轨作品")) }
        viewModel.consumeShare()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = message, withDismissAction = true)
        viewModel.consumeMessage()
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.stage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.ordinal >= initialState.ordinal
                val direction = if (forward) 1 else -1
                (
                    fadeIn(animationSpec = tween(durationMillis = 220, delayMillis = 50)) +
                        slideInHorizontally(animationSpec = tween(durationMillis = 300)) {
                            direction * it / 14
                        }
                    ) togetherWith (
                    fadeOut(animationSpec = tween(durationMillis = 130)) +
                        slideOutHorizontally(animationSpec = tween(durationMillis = 220)) {
                            -direction * it / 14
                        }
                    )
            },
            label = "stackerStage",
        ) { stage ->
            when (stage) {
                StackerStage.HOME -> HomeScreen(
                    state = state,
                    onPickPhotos = { photoPicker.launch(arrayOf("image/*")) },
                    onPickFolder = { folderPicker.launch(null) },
                    onResume = { viewModel.resumeDraft() },
                    onOpenJob = { viewModel.openJob(it) },
                )

                StackerStage.REVIEW -> ReviewScreen(
                    state = state,
                    onRemove = { viewModel.removeFrame(it) },
                    onClear = { viewModel.clearFrames() },
                    onAddMore = { addPhotosPicker.launch(arrayOf("image/*")) },
                    onNext = { viewModel.goSettings() },
                    onBack = { navigateBack() },
                )

                StackerStage.SETTINGS -> StackSettingsScreen(
                    state = state,
                    onMode = { viewModel.setBlendMode(it) },
                    onFormat = { viewModel.setOutputFormat(it) },
                    onQuality = { viewModel.setQuality(it) },
                    onRecursive = { viewModel.setRecursive(it) },
                    onStart = { viewModel.start() },
                    onBack = { viewModel.goReview() },
                )

                StackerStage.PROCESSING -> ProcessingScreen(
                    state = state,
                    onCancel = { viewModel.cancel() },
                    onRetry = { viewModel.retry() },
                    onBack = { viewModel.goReview() },
                )

                StackerStage.RESULT -> ResultScreen(
                    state = state,
                    onSave = { viewModel.saveToGallery() },
                    onExport = {
                        exportPicker.launch(
                            "star-trails.${state.job?.outputFormat?.extension ?: "jpg"}",
                        )
                    },
                    onShare = { viewModel.share() },
                    onDelete = {
                        viewModel.delete()
                        if (isDetailMode) onNavigateBack?.invoke()
                    },
                    onNewTask = { viewModel.goHome() },
                    onBack = { navigateBack() },
                )
            }
        }

        if (state.busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.24f))
                    // 遮罩必须自己消费点击，否则用户能穿透点到下层按钮重复触发操作。
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(Spacing.lg)
                            .size(32.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Spacing.lg),
        )
    }
}
