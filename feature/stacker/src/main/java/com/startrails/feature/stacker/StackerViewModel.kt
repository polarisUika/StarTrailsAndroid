package com.startrails.feature.stacker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.startrails.core.data.PreferencesRepository
import com.startrails.core.data.StackJobRepository
import com.startrails.core.domain.AppendStackUseCase
import com.startrails.core.domain.CancelStackUseCase
import com.startrails.core.domain.DeleteResultUseCase
import com.startrails.core.domain.DomainFailure
import com.startrails.core.domain.FrameValidation
import com.startrails.core.domain.PrepareStackUseCase
import com.startrails.core.domain.SaveResultUseCase
import com.startrails.core.domain.ShareResultUseCase
import com.startrails.core.domain.StartStackUseCase
import com.startrails.core.domain.StackWork
import com.startrails.core.model.BlendMode
import com.startrails.core.model.ErrorCode
import com.startrails.core.model.InputFrame
import com.startrails.core.model.JobStatus
import com.startrails.core.model.OutputFormat
import com.startrails.core.model.QualityLevel
import com.startrails.core.model.StackJob
import com.startrails.core.model.UserPreferences
import com.startrails.core.model.userMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class StackerStage { HOME, REVIEW, SETTINGS, PROCESSING, RESULT }

data class StackerUiState(
    val stage: StackerStage = StackerStage.HOME,
    val job: StackJob? = null,
    val frames: List<InputFrame> = emptyList(),
    val resumableJob: StackJob? = null,
    val blendMode: BlendMode = BlendMode.LIGHTEN,
    val outputFormat: OutputFormat = OutputFormat.JPEG,
    val quality: Int = QualityLevel.HIGHEST.percent,
    val recursive: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val currentName: String = "",
    val resultSizeBytes: Long = 0,
    val busy: Boolean = false,
    val message: String? = null,
    val shareUri: Uri? = null,
)

@HiltViewModel
class StackerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val preferencesRepository: PreferencesRepository,
    private val repository: StackJobRepository,
    private val prepare: PrepareStackUseCase,
    private val append: AppendStackUseCase,
    private val startStack: StartStackUseCase,
    private val cancelStack: CancelStackUseCase,
    private val saveResult: SaveResultUseCase,
    private val shareResult: ShareResultUseCase,
    private val deleteResult: DeleteResultUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StackerUiState())
    val uiState: StateFlow<StackerUiState> = _uiState.asStateFlow()

    private var framesObserver: Job? = null
    private var workObserver: Job? = null
    private var observedWorkId: String? = null

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs -> applyDefaults(prefs) }
        }
        viewModelScope.launch {
            repository.jobs.collect { jobs ->
                val draft = jobs.firstOrNull {
                    it.status == JobStatus.DRAFT || it.status == JobStatus.VALIDATING
                }
                val currentId = _uiState.value.job?.id
                val current = currentId?.let { id -> jobs.firstOrNull { it.id == id } }
                _uiState.update { state ->
                    var next = state.copy(resumableJob = draft)
                    if (current != null) {
                        next = next.copy(
                            job = current,
                            progress = current.processedCount,
                            total = current.frameCount,
                            stage = when {
                                current.status == JobStatus.SUCCEEDED &&
                                    next.stage == StackerStage.PROCESSING -> StackerStage.RESULT

                                else -> next.stage
                            },
                            message = when {
                                current.status == JobStatus.FAILED ->
                                    current.errorCode?.userMessage() ?: current.errorMessage

                                current.status == JobStatus.CANCELLED && state.message == null -> "任务已取消"

                                current.status == JobStatus.SUCCEEDED &&
                                    state.stage == StackerStage.PROCESSING -> null

                                else -> state.message
                            },
                        )
                    }
                    next
                }
                if (current != null && current.status == JobStatus.SUCCEEDED) {
                    refreshResultSize(current)
                }
            }
        }
        viewModelScope.launch {
            repository.activeJob.collect { job ->
                observeWorkProgress(job)
                if (job == null) return@collect
                _uiState.update { state ->
                    when {
                        state.job == null -> state.copy(
                            job = job,
                            progress = job.processedCount,
                            total = job.frameCount,
                            blendMode = job.blendMode,
                            outputFormat = job.outputFormat,
                            quality = QualityLevel.fromQuality(job.quality).percent,
                            recursive = job.recursive,
                        )

                        state.job?.id != job.id -> state

                        else -> state.copy(
                            progress = job.processedCount,
                            total = job.frameCount,
                            blendMode = job.blendMode,
                            outputFormat = job.outputFormat,
                            quality = QualityLevel.fromQuality(job.quality).percent,
                            recursive = job.recursive,
                        )
                    }
                }
            }
        }
        savedStateHandle.get<String>("jobId")?.let { id ->
            viewModelScope.launch { repository.getJob(id)?.let { openJob(it) } }
        }
    }

    private fun applyDefaults(prefs: UserPreferences) {
        _uiState.update { state ->
            if (state.job != null || state.stage != StackerStage.HOME) state else state.copy(
                blendMode = prefs.defaultBlendMode,
                outputFormat = prefs.defaultFormat,
                quality = QualityLevel.fromQuality(prefs.defaultQuality).percent,
                recursive = prefs.defaultRecursive,
            )
        }
    }

    /** 首页“选择照片/选择文件夹”：新建一次任务。 */
    fun pickPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        prepareSources(uris, "已选择 ${uris.size} 张照片")
    }

    fun pickFolder(uri: Uri) {
        prepareSources(listOf(uri), "文件夹来源")
    }

    /** 检查页“继续添加”：追加到当前草稿，保留已选照片。 */
    fun appendPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val job = _uiState.value.job
        if (job == null) {
            pickPhotos(uris)
            return
        }
        viewModelScope.launch {
            setBusy(true)
            clearMessage()
            val before = _uiState.value.frames.size
            try {
                val prepared = append(job.id, uris, _uiState.value.recursive)
                observeFrames(prepared.job.id)
                _uiState.update {
                    it.copy(
                        stage = StackerStage.REVIEW,
                        job = prepared.job,
                        frames = prepared.frames,
                        total = prepared.frames.size,
                        message = if (prepared.frames.size == before) "没有新增照片" else null,
                    )
                }
            } catch (e: Exception) {
                report(e)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun prepareSources(uris: List<Uri>, label: String) {
        viewModelScope.launch {
            setBusy(true)
            clearMessage()
            try {
                val state = _uiState.value
                val prepared = prepare(
                    sources = uris,
                    recursive = state.recursive,
                    mode = state.blendMode,
                    format = state.outputFormat,
                    quality = state.quality,
                    sourceLabel = label,
                )
                observeFrames(prepared.job.id)
                _uiState.update {
                    it.copy(
                        stage = StackerStage.REVIEW,
                        job = prepared.job,
                        frames = prepared.frames,
                        total = prepared.frames.size,
                        blendMode = prepared.job.blendMode,
                        outputFormat = prepared.job.outputFormat,
                        recursive = prepared.job.recursive,
                    )
                }
            } catch (e: Exception) {
                report(e)
            } finally {
                setBusy(false)
            }
        }
    }

    fun resumeDraft() {
        val draft = _uiState.value.resumableJob ?: return
        viewModelScope.launch {
            val frames = repository.getFrames(draft.id)
            observeFrames(draft.id)
            _uiState.update {
                it.copy(
                    stage = StackerStage.REVIEW,
                    job = draft,
                    frames = frames,
                    total = frames.size,
                    blendMode = draft.blendMode,
                    outputFormat = draft.outputFormat,
                    quality = QualityLevel.fromQuality(draft.quality).percent,
                    recursive = draft.recursive,
                )
            }
        }
    }

    private fun observeFrames(jobId: String) {
        framesObserver?.cancel()
        framesObserver = viewModelScope.launch {
            repository.frames(jobId).collect { frames ->
                _uiState.update { it.copy(frames = frames, total = frames.size) }
            }
        }
    }

    /** 通过 WorkInfo.progress 展示当前正在处理的文件名。 */
    private fun observeWorkProgress(job: StackJob?) {
        val workId = job?.workId
        if (workId == observedWorkId) return
        observedWorkId = workId
        workObserver?.cancel()
        if (workId == null) {
            _uiState.update { it.copy(currentName = "") }
            return
        }
        val uuid = runCatching { UUID.fromString(workId) }.getOrNull() ?: return
        workObserver = viewModelScope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(uuid).collect { info ->
                val name = info?.progress?.getString(StackWork.KEY_CURRENT_NAME).orEmpty()
                _uiState.update { it.copy(currentName = name) }
            }
        }
    }

    fun removeFrame(frame: InputFrame) {
        val job = _uiState.value.job ?: return
        if (!ensureEditable(job)) return
        viewModelScope.launch {
            val remaining = _uiState.value.frames.filterNot {
                it.documentKey == frame.documentKey && it.frameOrder == frame.frameOrder
            }
            // 基准帧可能被删掉，必须按新基准重算其余帧的校验状态。
            val normalized = FrameValidation.normalize(remaining)
            repository.saveFrames(job.id, normalized)
            repository.save(
                job.copy(
                    frameCount = normalized.size,
                    width = normalized.firstOrNull()?.width ?: 0,
                    height = normalized.firstOrNull()?.height ?: 0,
                ),
            )
        }
    }

    fun clearFrames() {
        val job = _uiState.value.job ?: return
        if (!ensureEditable(job)) return
        viewModelScope.launch {
            repository.saveFrames(job.id, emptyList())
            repository.save(job.copy(frameCount = 0, width = 0, height = 0))
            _uiState.update { it.copy(frames = emptyList(), total = 0) }
        }
    }

    /** 运行中的任务不允许改照片，否则数据库里的输入与已产出的结果会不一致。 */
    private fun ensureEditable(job: StackJob): Boolean {
        if (job.status.isActive) {
            _uiState.update { it.copy(message = "任务正在运行，无法修改照片") }
            return false
        }
        return true
    }

    fun goSettings() = _uiState.update { it.copy(stage = StackerStage.SETTINGS, message = null) }

    fun goReview() = _uiState.update { it.copy(stage = StackerStage.REVIEW, message = null) }

    fun goHome() {
        framesObserver?.cancel()
        framesObserver = null
        _uiState.update {
            it.copy(
                stage = StackerStage.HOME,
                job = null,
                frames = emptyList(),
                message = null,
                progress = 0,
                total = 0,
                currentName = "",
                resultSizeBytes = 0,
            )
        }
    }

    fun setBlendMode(value: BlendMode) = _uiState.update { it.copy(blendMode = value) }

    fun setOutputFormat(value: OutputFormat) = _uiState.update { it.copy(outputFormat = value) }

    fun setQuality(value: Int) = _uiState.update {
        it.copy(quality = QualityLevel.fromQuality(value).percent)
    }

    fun setRecursive(value: Boolean) = _uiState.update { it.copy(recursive = value) }

    fun start() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            setBusy(true)
            clearMessage()
            try {
                val updated = repository.saveParameters(
                    job,
                    _uiState.value.blendMode,
                    _uiState.value.outputFormat,
                    _uiState.value.quality,
                    _uiState.value.recursive,
                )
                val started = startStack(updated.id)
                _uiState.update {
                    it.copy(stage = StackerStage.PROCESSING, job = started, currentName = "")
                }
            } catch (e: Exception) {
                report(e)
            } finally {
                setBusy(false)
            }
        }
    }

    fun retry() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                val started = startStack(job.id)
                _uiState.update {
                    it.copy(message = null, stage = StackerStage.PROCESSING, job = started, currentName = "")
                }
            } catch (e: Exception) {
                report(e)
            } finally {
                setBusy(false)
            }
        }
    }

    fun cancel() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            try {
                cancelStack(job.id)
                _uiState.update { it.copy(message = "任务已取消") }
            } catch (e: Exception) {
                report(e)
            }
        }
    }

    fun saveToGallery() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                val updated = saveResult(job.id, null, false)
                _uiState.update { it.copy(job = updated, message = "已保存到相册") }
            } catch (e: Exception) {
                report(e)
            } finally {
                setBusy(false)
            }
        }
    }

    fun exportTo(uri: Uri) {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            setBusy(true)
            try {
                val updated = saveResult(job.id, uri, true)
                _uiState.update { it.copy(job = updated, message = "文件已导出") }
            } catch (e: Exception) {
                report(e)
            } finally {
                setBusy(false)
            }
        }
    }

    fun share() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(shareUri = shareResult(job)) }
            } catch (e: Exception) {
                report(e)
            }
        }
    }

    fun consumeShare() = _uiState.update { it.copy(shareUri = null) }

    fun delete() {
        val job = _uiState.value.job ?: return
        viewModelScope.launch {
            framesObserver?.cancel()
            framesObserver = null
            deleteResult(job.id)
            _uiState.update {
                it.copy(
                    stage = StackerStage.HOME,
                    job = null,
                    frames = emptyList(),
                    resultSizeBytes = 0,
                    message = "作品已删除",
                )
            }
        }
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null) }

    fun openJob(job: StackJob) {
        viewModelScope.launch {
            val frames = repository.getFrames(job.id)
            observeFrames(job.id)
            _uiState.update {
                it.copy(
                    // 已完成 → 结果页；草稿 → 检查页；其余（含失败/取消）→ 进度页提供重试。
                    stage = when (job.status) {
                        JobStatus.SUCCEEDED -> StackerStage.RESULT
                        JobStatus.DRAFT, JobStatus.VALIDATING -> StackerStage.REVIEW
                        else -> StackerStage.PROCESSING
                    },
                    job = job,
                    frames = frames,
                    total = frames.size,
                    progress = job.processedCount,
                    blendMode = job.blendMode,
                    outputFormat = job.outputFormat,
                    quality = QualityLevel.fromQuality(job.quality).percent,
                    recursive = job.recursive,
                )
            }
            refreshResultSize(job)
        }
    }

    /** 结果文件大小在 IO 线程读取，界面不再做同步磁盘访问。 */
    private fun refreshResultSize(job: StackJob) {
        viewModelScope.launch {
            val size = withContext(Dispatchers.IO) {
                job.stagingPath?.let { File(it) }?.takeIf { it.exists() }?.length() ?: 0L
            }
            _uiState.update { state ->
                if (state.job?.id == job.id) state.copy(resultSizeBytes = size) else state
            }
        }
    }

    private fun setBusy(value: Boolean) = _uiState.update { it.copy(busy = value) }

    private fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun report(e: Exception) {
        val message = when (e) {
            is DomainFailure -> e.message ?: e.code.userMessage()
            else -> e.message ?: ErrorCode.UNKNOWN.userMessage()
        }
        _uiState.update { it.copy(message = message) }
    }
}

private suspend fun StackJobRepository.saveParameters(
    job: StackJob,
    mode: BlendMode,
    format: OutputFormat,
    quality: Int,
    recursive: Boolean,
): StackJob = job.copy(
    blendMode = mode,
    outputFormat = format,
    quality = QualityLevel.fromQuality(quality).percent,
    recursive = recursive,
).also { save(it) }
