package com.startrails.feature.gallery

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startrails.core.data.StackJobRepository
import com.startrails.core.domain.DeleteResultUseCase
import com.startrails.core.domain.DomainFailure
import com.startrails.core.domain.SaveResultUseCase
import com.startrails.core.domain.ShareResultUseCase
import com.startrails.core.model.JobStatus
import com.startrails.core.model.StackJob
import com.startrails.core.model.displayName
import com.startrails.core.model.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class GalleryItem(
    val job: StackJob,
    val available: Boolean,
    /** 已确认存在的暂存文件；不存在时为 null，界面回退到 outputUri。 */
    val previewFile: File?,
    val dateText: String,
    val modeText: String,
    val sizeText: String,
)

data class GalleryUiState(
    val items: List<GalleryItem> = emptyList(),
    val shareUri: Uri? = null,
    val message: String? = null,
)

@HiltViewModel
class GalleryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StackJobRepository,
    private val saveResult: SaveResultUseCase,
    private val shareResult: ShareResultUseCase,
    private val deleteResult: DeleteResultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.jobs
                .map { jobs -> jobs.filter { it.status == JobStatus.SUCCEEDED }.map { it.toItem() } }
                .flowOn(Dispatchers.IO)
                .collect { items -> _uiState.update { it.copy(items = items) } }
        }
    }

    fun export(job: StackJob, target: Uri) {
        viewModelScope.launch {
            try {
                saveResult(job.id, target, true)
                _uiState.update { it.copy(message = "文件已导出") }
            } catch (e: Exception) {
                report(e)
            }
        }
    }

    fun share(job: StackJob) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(shareUri = shareResult(job)) }
            } catch (e: Exception) {
                report(e)
            }
        }
    }

    fun consumeShare() = _uiState.update { it.copy(shareUri = null) }

    fun delete(job: StackJob) {
        viewModelScope.launch {
            deleteResult(job.id)
            _uiState.update { it.copy(message = "作品已删除") }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    private fun StackJob.toItem(): GalleryItem {
        // 暂存文件可能已被清理，必须确认存在，否则 Coil 会加载不存在的文件显示裂图。
        val staged = stagingPath?.let { File(it) }?.takeIf { it.exists() }
        val available = staged != null || isUriReadable(outputUri)
        val size = staged?.length() ?: 0L
        return GalleryItem(
            job = this,
            available = available,
            previewFile = staged,
            dateText = DATE_FORMAT.format(Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault())),
            modeText = blendMode.displayName(),
            sizeText = if (size > 0) {
                Formatter.formatShortFileSize(context, size)
            } else {
                outputFormat.displayName()
            },
        )
    }

    private fun isUriReadable(uriString: String?): Boolean {
        val value = uriString ?: return false
        return try {
            context.contentResolver.openAssetFileDescriptor(Uri.parse(value), "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun report(e: Exception) {
        val message = when (e) {
            is DomainFailure -> e.message ?: e.code.userMessage()
            else -> e.message ?: "操作失败"
        }
        _uiState.update { it.copy(message = message) }
    }

    private companion object {
        /** DateTimeFormatter 不可变且线程安全，可以复用同一个实例。 */
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    }
}
