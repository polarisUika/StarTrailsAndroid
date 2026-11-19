# StarTrails Android 项目问题检测与分析报告

## 概览

对项目源码（共 9 个 Gradle 模块：`:app`、`:core:model`、`:core:designsystem`、`:core:data`、`:core:domain`、`:core:imaging`、`:feature:stacker`、`:feature:gallery`、`:feature:settings`）进行全面静态审计后，共发现 **20 项问题**，按严重程度分布如下：

| 严重级别 | 数量 | 核心影响 |
| :--- | :---: | :--- |
| **P0 严重** | 5 | 任务取消状态卡死、并发调度死锁、核心流程数据丢失、校验无法恢复 |
| **P1 高危** | 5 | 大图处理 OOM 崩溃、导航返回逻辑异常、重复保存相册、类型强转崩溃 |
| **P2 中危** | 6 | 进度文案不更新、暂存文件泄漏、资源校验失真、DB 非原子操作、构建配置缺陷 |
| **P3 低危 / 优化** | 4 | 废弃 API、未用生命周期感知流收集、死代码、测试覆盖严重不足 |

---

## 一、严重问题（P0）

### 1.1 `StackWorker` 吞掉 `CancellationException` 且取消后写库失效
* **位置**：`core/domain/src/main/java/com/startrails/core/domain/StackWorker.kt`（第 56、82–101 行）
* **问题分析**：
  1. `doWork()` 中的 `catch (e: Exception)`（第 88 行）会捕获协程取消时抛出的 `CancellationException`，将其误判为 `ErrorCode.OUTPUT_FAILED`（“叠加失败，可尝试重新执行”）。
  2. 捕获异常后调用的 `fail()` 会执行挂起函数 `jobs.save(...)`（第 95 行）。当 Worker 已被取消时，当前协程上下文处于已取消状态，挂起写库会再次抛出 `CancellationException` 而中止，导致数据库中的任务状态长期停留在 `CANCELLING` 或 `RUNNING`。
  3. 进度回调中 `jobs.save(job.copy(status = JobStatus.RUNNING, ...))`（第 56 行）会无条件把 `CancelStackUseCase` 刚写入的 `CANCELLING` 覆盖回 `RUNNING`。
* **修复建议**：
  * 显式 `catch (e: CancellationException)`，在 `withContext(NonCancellable)` 中更新数据库状态为 `JobStatus.CANCELLED` 并清理暂存文件后重新抛出（或按 WorkManager 规范返回）。
  * `fail()` 内部所有清理和持久化操作统一包裹在 `withContext(NonCancellable)` 中。
  * 进度更新改用局部 SQL 更新（仅更新 `processedCount` 和 `updatedAt`），避免覆盖 `status`。

### 1.2 `JobStatus.VALIDATING` 被归为活动状态，导致任务调度互锁与永久 `QUEUED`
* **位置**：
  * `core/model/src/main/java/com/startrails/core/model/JobStatus.kt`（第 19–20 行）
  * `core/domain/src/main/java/com/startrails/core/domain/StartStackUseCase.kt`（第 30、53–57 行）
* **问题分析**：
  * `JobStatus.isActive` 包含了 `VALIDATING`，而 `PrepareStackUseCase` 创建的所有待确认草稿均被保存为 `VALIDATING`。
  * **场景 A（误拦截）**：用户选完照片返回首页（留下 `VALIDATING` 草稿），再去重试历史失败任务（`status == FAILED`，`!job.status.isActive` 为真）时，`repository.hasActiveJob()` 返回 `true`，触发 `DomainFailure.busy()`，导致实际上无任务运行时也无法重试。
  * **场景 B（永久卡死）**：当任务 A 正在运行时，若用户启动处于 `VALIDATING` 状态的任务 B，由于 `job.status.isActive` 为 `true`，第 30 行的防并发检查被绕过；随后任务 B 在 Room 中被标为 `QUEUED`，但 `WorkManager.enqueueUniqueWork(..., ExistingWorkPolicy.KEEP, request)` 会直接丢弃任务 B 的请求，使其在数据库中永久处于 `QUEUED` 且永不执行。
* **修复建议**：
  * 将 `VALIDATING` 移出 `JobStatus.isActive`（以及 `StackDao.observeActiveJob` / `activeJobCount` / `getInterruptedJobs` 的 SQL 条件），仅将 `QUEUED / RUNNING / SAVING / CANCELLING` 视为活动任务。
  * 并发检查应改为判断“是否存在 `id != jobId` 的活动任务”。

### 1.3 `StarTrailsApplication` 启动恢复与 WorkManager 存在竞态条件
* **位置**：
  * `app/src/main/java/com/startrails/StarTrailsApplication.kt`（第 27 行）
  * `core/data/src/main/java/com/startrails/core/data/StackJobRepository.kt`（第 67–80 行）
* **问题分析**：
  * 应用进程被系统回收后，WorkManager 会在进程重启时自动重新调度未完成的 `StackWorker`。
  * 但 `StarTrailsApplication.onCreate()` 无条件执行 `recoverJobs()`，将所有处于活动状态的任务直接标为 `FAILED`（“任务被中断，可以重新执行”），既未查询 `WorkManager.getWorkInfosForUniqueWork(StackWork.UNIQUE_NAME)` 确认任务是否仍在队列中，也可能与正在启动的 `StackWorker.doWork()` 产生写库竞态。
* **修复建议**：
  * 恢复逻辑中先查询 WorkManager 中对应 `workId` 的 `WorkInfo.State`，仅对状态已为终态（`FAILED / CANCELLED / SUCCEEDED`）或已不存在于 WorkManager 中的残留记录执行失败兜底。

### 1.4 照片检查页“继续添加”会覆盖已有照片并产生孤儿草稿
* **位置**：
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/StackerRoute.kt`（第 44–46、113 行）
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/StackerViewModel.kt`（第 157–198 行）
* **问题分析**：
  * `ReviewScreen` 的“继续添加”复用了首页的 `photoPicker`，回调直接调用 `viewModel.pickPhotos(uris) -> prepareSources(uris, ...)`。
  * `prepare()` 会创建一个全新的 `StackJob` 并完全替换当前 `state.frames`，原有已选照片全部丢失，同时在数据库中留下无主草稿；若用户只补选 1 张照片，还会直接触发 `NOT_ENOUGH_FRAMES` 报错。
* **修复建议**：
  * 区分“新建选择”与“向当前任务追加照片”，在 `StackerViewModel` 中新增 `appendPhotos(uris: List<Uri>)`：基于现有 `job` 发现新帧、按 `documentKey` 去重、校验尺寸并追加保存到当前 `jobId`。

### 1.5 删除首帧异常照片后未重新计算其余帧的尺寸校验状态
* **位置**：`feature/stacker/src/main/java/com/startrails/feature/stacker/StackerViewModel.kt`（第 229–244 行）
* **问题分析**：
  * `PrepareStackUseCase` 以第 0 帧的宽高为基准，若第 0 帧本身尺寸异常，则后续所有正常帧都会被标上 `issueCode = DIMENSION_MISMATCH`。
  * 用户在 `ReviewScreen` 中删除第 0 帧后，`removeFrame()` 虽更新了 `job.width` / `job.height`，但**没有重新计算剩余帧的 `issueCode` 和 `issueDetail`**。其余原本一致的照片仍带有错误标记，导致“下一步”按钮永久置灰。
* **修复建议**：
  * 在 `removeFrame()` 中以 `remaining.firstOrNull()` 为新基准，遍历重新计算所有剩余帧的 `issueCode` 与 `issueDetail` 后再写入数据库。

---

## 二、高危问题（P1）

### 2.1 图像读取、解码与编码存在严重内存峰值与 OOM 风险
* **位置**：
  * `core/imaging/src/main/java/com/startrails/core/imaging/FrameMetadataReader.kt`（第 23–30 行）
  * `core/imaging/src/main/java/com/startrails/core/imaging/ImageDecoder.kt`（第 17、42–57、99 行）
  * `core/imaging/src/main/java/com/startrails/core/imaging/StackingEngine.kt`（第 38、50、56 行）
  * `core/imaging/src/main/java/com/startrails/core/imaging/TiffWriter.kt`（第 26 行）与 `BmpWriter.kt`（第 13 行）
* **问题分析**：
  1. **元数据读取全量入内存**：`FrameMetadataReader.read()` 注释声称“只读尺寸元数据，不解码完整像素”，但实际调用了 `stream.readBytes()` 将整张原图完整读入堆内存；且 `openInputStream(uri)` 返回 `null` 时会误报 `Decode` 而非 `PermissionLost`。
  2. **解码内存峰值叠加**：`ImageDecoder.decode()` 同样先 `readBytes()`，再分配 `base Bitmap`（4 B/px）→ 旋转后的 `oriented Bitmap`（4 B/px）→ `argb IntArray`（4 B/px）→ `rgb ByteArray`（3 B/px）。一张 24MP 旋转照片单帧解码峰值超过 **370 MB**，再叠加 `AverageAccumulator` 的 `FloatArray`（288 MB），在绝大多数手机上会直接 OOM。
  3. **`OutOfMemoryError` 漏捕**：`ImageDecoder` 仅在 `BitmapFactory.decodeByteArray` 处捕获了 `OutOfMemoryError`，对 `readBytes()`、`Bitmap.createBitmap()`、`IntArray`、`ByteArray` 均未捕获。
  4. **无损写入整包分配内存**：`TiffWriter.write()` 和 `BmpWriter.write()` 均通过 `ByteBuffer.allocate(total)` 一次性分配整张未压缩图像大小的字节数组（24MP 约 72 MB）再整体写出，内存直接翻倍；`ByteAccumulator` 也在构造和 `result()` 时各做了一次 `copyOf()`。
* **修复建议**：
  * `FrameMetadataReader` 改用流式读取（分别打开 `InputStream` 给 `BitmapFactory.decodeStream(..., options)` 与 `ExifInterface(stream)`），不再调用 `readBytes()`。
  * `ImageDecoder` 按行（或分块）调用 `Bitmap.getPixels()` 写入 `rgb`，避免分配全图 `IntArray`；`base.recycle()` 放入 `try/finally`。
  * `TiffWriter` 先写出头部与 IFD，再直接 `output.write(pixels.rgb)`；`BmpWriter` 按行复用单行缓冲写入 `OutputStream`。
  * `AverageAccumulator` 可改用 `IntArray` 替代 `FloatArray` 消除浮点累加误差并提升吞吐。

### 2.2 首页“查看进度”跳转错误，且 `VALIDATING` 草稿同时展示两张卡片
* **位置**：
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/StackerViewModel.kt`（第 113–139、259、384 行）
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/HomeScreen.kt`（第 77、123–146 行）
* **问题分析**：
  1. `openJob(job)` 第 384 行写死为 `stage = if (job.status == JobStatus.SUCCEEDED) StackerStage.RESULT else StackerStage.REVIEW`。用户在首页点击 `ActiveJobCard` 的“查看进度”时，不会进入 `PROCESSING` 进度页，而是跳回 `REVIEW` 列表页（甚至允许在任务运行期间删除输入帧）。
  2. 因 `VALIDATING` 属于 `isActive`，用户在首页时 `repository.activeJob` 会将草稿注入 `state.job`，导致首页同时出现“继续上次编辑（`ResumeCard`）”和“叠加任务进行中 0/N（`ActiveJobCard`）”两张互相矛盾的卡片。
  3. `goHome()` 未取消 `framesObserver`，回到首页后仍在持续监听上一个任务的帧列表。
* **修复建议**：
  * `openJob(job)` 按状态精确路由：`SUCCEEDED -> RESULT`，`QUEUED/RUNNING/SAVING/CANCELLING/FAILED/CANCELLED -> PROCESSING`，`DRAFT/VALIDATING -> REVIEW`。
  * 结合 1.2 修复 `isActive` 定义，并在 `goHome()` 中调用 `framesObserver?.cancel()`。

### 2.3 从作品页进入详情后，系统返回键无法返回作品页
* **位置**：
  * `app/src/main/java/com/startrails/ui/AppNavigation.kt`（第 297、301 行）
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/StackerRoute.kt`（第 54–60 行）
* **问题分析**：
  * 从 `GalleryRoute` 点击作品会导航到 `stacker/detail/{jobId}`，该路由复用 `StackerRoute` 并打开 `StackerStage.RESULT`。
  * 此时按下系统返回键，会被 `StackerRoute` 的 `BackHandler(enabled = state.stage != StackerStage.HOME)` 拦截，先跳转到已完成任务的 `REVIEW` 页，再按一次跳到 `HOME` 页，用户被困在 `detail` 路由内部，无法正常 `popBackStack()` 回到作品页。
* **修复建议**：
  * 给 `StackerRoute` 增加 `isDetailMode: Boolean` 与 `onNavigateBack: () -> Unit` 参数；在详情模式或 `RESULT` 阶段按返回键时，直接交由 `navController.popBackStack()` 或返回 `HOME`，不再退回已完成任务的 `REVIEW` 页面，并在 `ResultScreen` 顶栏提供返回按钮。

### 2.4 自动保存相册与结果页“保存到相册”重复，且导出会覆盖原 `outputUri`
* **位置**：
  * `core/domain/src/main/java/com/startrails/core/domain/StackWorker.kt`（第 68 行）
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/ResultScreen.kt`（第 115–124 行）
  * `core/domain/src/main/java/com/startrails/core/domain/ResultUseCases.kt`（第 32、67 行）
* **问题分析**：
  1. `StackWorker` 在叠加完成后已自动调用 `results.saveToGallery(staging, job)` 并写入 `outputUri`，但 `ResultScreen` 仍将“保存到相册”作为主按钮展示，没有任何“已保存”状态提示；用户每点一次就会在 `Pictures/StarTrails` 下生成一份重复文件。
  2. 用户点击“导出文件”将作品另存到自定义目录后，`SaveResultUseCase` 会把 `job.outputUri` 覆盖为新导出的 SAF URI。之后若用户删除该作品，只会尝试删除导出文件，原相册副本成为孤儿文件。
* **修复建议**：
  * 明确产品语义：若自动保存相册，则 `ResultScreen` 展示“已保存到相册”状态（或仅在 `outputUri == null` 时显示“保存到相册”按钮）；另存导出（`exportTo`）不应覆盖原相册的 `outputUri`。

### 2.5 `AppNavigation` 强制将 `LocalContext.current` 转为 `Activity`
* **位置**：`app/src/main/java/com/startrails/ui/AppNavigation.kt`（第 114 行）
* **问题分析**：
  * `LocalContext.current as Activity` 属于不安全强转。在 Context 被 `ContextWrapper` 包装（如主题包裹、Hilt 注入容器或 Compose Preview）时会直接抛出 `ClassCastException` 崩溃。
  * 注意项目在 `core/designsystem/.../Theme.kt`（第 338 行）已正确实现了 `tailrec fun Context.findActivity()`，但 `AppNavigation.kt` 未复用。
* **修复建议**：
  * 统一使用 `findActivity()` 递归解包 `ContextWrapper`，或升级使用 `androidx.activity.compose.LocalActivity.current`。

---

## 三、中危问题（P2）

### 3.1 `ProcessingScreen` 的 `currentName` 从未更新
* **位置**：
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/StackerViewModel.kt`（第 47 行）
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/ProcessingScreen.kt`（第 152 行）
* **问题分析**：`StackerUiState.currentName` 全项目没有任何写入点，`StackWorker` 也未通过 `setProgress()` 或数据库字段上报当前处理的文件名，导致处理界面全程显示“正在准备照片…”。
* **修复建议**：在 `StackWorker` 中通过 `setProgress(workDataOf("current_name" to name))` 上报，并在 `StackerViewModel` 观察对应 `WorkInfo` 更新 `currentName`。

### 3.2 失败任务暂存文件泄漏与分享缓存未清理
* **位置**：
  * `core/domain/src/main/java/com/startrails/core/domain/StackWorker.kt`（第 63–74、94 行）
  * `core/data/src/main/java/com/startrails/core/data/ResultRepository.kt`（第 90–92 行）
* **问题分析**：
  * `StackWorker` 在第 63 行创建 `staging` 文件，但直到第 74 行构建 `finished` 时才把路径写入 `job.stagingPath`。若编码中途或 `saveToGallery()` 失败，进入 `fail(job, ...)` 时 `job.stagingPath` 仍为 `null`，导致损坏或半成品暂存文件永远残留在 `filesDir/staging/`。
  * `cacheDir/shares/` 下的分享副本没有任何过期清理机制。
* **修复建议**：将局部变量 `staging` 传入 `fail()` 并在异常分支显式删除；应用启动时定期清理过期的 `shares/` 缓存和孤儿 `staging/` 文件。

### 3.3 `PrepareStackUseCase.checkResources()` 检查路径与内存估算失真
* **位置**：`core/domain/src/main/java/com/startrails/core/domain/PrepareStackUseCase.kt`（第 128–138 行）
* **问题分析**：
  * 磁盘空间检查的是 `context.getExternalFilesDir(null)?.usableSpace`，但 `ResultRepository.stagingFile()` 实际写入内部存储 `context.filesDir`。
  * 内存估算使用 `AVERAGE = 15 B/px`、`其他 = 6 B/px`，远低于当前解码 + 累加器实际峰值（约 21–27 B/px），无法有效拦截 OOM。
* **修复建议**：改为检查 `context.filesDir.usableSpace`，并根据实际内存模型（优化解码后）校准 `bytesPerPixel` 系数。

### 3.4 `StackJobRepository` 多步数据库操作缺少事务保护
* **位置**：
  * `core/data/src/main/java/com/startrails/core/data/StackJobRepository.kt`（第 54–62 行）
  * `core/data/src/main/java/com/startrails/core/data/AppDatabase.kt`（第 32–45 行）
* **问题分析**：`saveFrames()` 先 `clearFrames` 再 `upsertFrames`，`deleteJob()` 先 `clearFrames` 再 `deleteJob`，均未加 `@Transaction`。Flow 监听方可能在两次调用之间收到空列表造成 UI 闪烁，进程异常中止还会导致数据半写。
* **修复建议**：在 `StackDao` 中定义 `@Transaction` 方法执行替换与删除，或在 `InputFrameEntity` 上配置 `@ForeignKey(..., onDelete = ForeignKey.CASCADE)`。

### 3.5 `GalleryCard` 预览回退失效、`ResultScreen` 主线程 I/O 与缩放重置缺陷
* **位置**：
  * `feature/gallery/src/main/java/com/startrails/feature/gallery/GalleryRoute.kt`（第 205–206 行）
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/ResultScreen.kt`（第 80、272 行）
* **问题分析**：
  1. `GalleryCard` 构造 `model` 时直接取 `item.job.stagingPath?.let { File(it) } ?: ...`，未判断 `File(it).exists()`。若暂存文件已缺失但 `outputUri` 有效，Coil 会加载不存在的 `File` 导致白屏/裂图。
  2. `ResultScreen` 第 80 行在 Composable 函数体中直接执行 `File(it).length()`，每次重组都会在主线程触发同步磁盘 I/O。
  3. `ZoomablePreview` 点击缩小按钮（第 272 行）将 `scale` 减回 `1f` 时，未将 `offset` 归零，导致图片缩回 100% 后仍偏离视口中心。
* **修复建议**：
  * `GalleryCard` 改为 `stagingPath?.let(::File)?.takeIf(File::exists) ?: outputUri?.let(Uri::parse)`。
  * 文件大小改由 ViewModel 在 `Dispatchers.IO` 读取并放入 `UiState`（同时兼容查询 `outputUri` 大小）。
  * 缩小按钮中增加 `if (scale <= 1f) offset = Offset.Zero`。

### 3.6 构建配置与 XML 资源问题
* **位置**：
  * `app/src/main/res/xml/data_extraction_rules.xml`（第 3–14 行）
  * `app/build.gradle.kts`（第 42–43、78 行）
* **问题分析**：
  1. `data_extraction_rules.xml` 中 `<exclude domain="root" />` 等标签缺少必填属性 `path="."`，不符合 Android 12+ 规范，在 Lint 或严格解析下会报错。
  2. `app/build.gradle.kts` 引用了 `proguard-rules.pro`，但项目中实际**不存在该文件**；同时 Release 构建开启了 `isMinifyEnabled = false` 且未配置 `isShrinkResources = true`，会导致发布包体积偏大且未混淆。
  3. `app/build.gradle.kts` 第 78 行声明了 `debugImplementation(libs.androidx.work.runtime.ktx)`，属于多余且作用域错误的依赖声明（`:core:domain` 已通过 `api` 暴露该依赖）。
* **修复建议**：为 `data_extraction_rules.xml` 补全 `path="."`；补充 `app/proguard-rules.pro`（含 Hilt/Room/WorkManager 规则）并在 release 启用代码与资源压缩；移除多余的 `debugImplementation(libs.androidx.work.runtime.ktx)`。

---

## 四、低危问题与代码质量改进建议（P3）

### 4.1 使用了已废弃的 WebP 编码常量
* **位置**：`core/imaging/src/main/java/com/startrails/core/imaging/ImageEncoder.kt`（第 35 行）
* **说明**：`Bitmap.CompressFormat.WEBP` 自 API 30 起已废弃（当前 `minSdk = 29, targetSdk = 35`）。建议在 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.R` 时使用 `WEBP_LOSSY`（或质量为 100 时使用 `WEBP_LOSSLESS`）。

### 4.2 全项目未使用生命周期感知的 `collectAsStateWithLifecycle()`
* **位置**：`MainActivity.kt`（第 39 行）、`StackerRoute.kt`（第 40 行）、`GalleryRoute.kt`（第 78 行）、`SettingsRoute.kt`（第 65 行）
* **说明**：所有 feature 模块均已引入 `androidx.lifecycle:lifecycle-runtime-compose`，但实际全部调用了 `collectAsState()`，应用切入后台后仍持续收集 Room/DataStore 流。建议统一替换为 `collectAsStateWithLifecycle()`。

### 4.3 硬编码版本号与展示格式化精度不足
* **位置**：
  * `feature/settings/src/main/java/com/startrails/feature/settings/SettingsRoute.kt`（第 179 行）：版本号 `"1.0.0"` 硬编码，建议从 `PackageManager` 或 `BuildConfig.VERSION_NAME` 动态读取。
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/ResultScreen.kt`（第 110、304 行）：耗时整除显示 `"0 秒"`、大小整除显示 `"1 MB"`（丢失小数），建议使用 `String.format("%.1f MB", ...)` 或 `Formatter.formatShortFileSize(context, bytes)`。
  * `feature/gallery/src/main/java/com/startrails/feature/gallery/GalleryViewModel.kt`（第 108 行）：文件大小仅按 KB 展示（如 `24576 KB`），且每次映射都新建 `SimpleDateFormat` 实例，建议统一使用 `DateTimeFormatter` 和通用文件大小格式化工具。

### 4.4 死代码与测试覆盖率严重不足
* **未使用的代码与枚举**：
  * `PrepareStackUseCase.fromDiscovered()`（未调用，且其内部 `checkResources(mode, 0, 0)` 为无效校验）
  * `ResultRepository.formatOf()`、`Controls.kt` 中的 `LabeledSlider`、`ReviewScreen.kt` 第 20 行未用导入 `Icons.Filled.Add`
  * `JobStatus.NEEDS_PERMISSION` 与 `ErrorCode.UNSUPPORTED_FORMAT` 从未被赋值或抛出
  * `ImageStacker.kt` 第 28–30 行捕获 `SecurityException` 为死代码（`ImageDecoder.decode` 内部已拦截并包装）
  * `FramePixels` 为包含 `ByteArray` 的 `data class`，未重写 `equals`/`hashCode`
* **测试覆盖**：整个 9 模块工程仅存在 1 个单元测试文件（`core/imaging/.../StackingAlgorithmsTest.kt`，5 个用例）。建议优先为 `StackWorker`（取消/恢复/失败清理）、`StartStackUseCase`（并发与状态校验）、`StackerViewModel`（追加与删帧重算）、`TiffWriter/BmpWriter` 与 `FrameSourceRepository`（自然排序与去重）补齐单元测试。

---

## 五、建议修复顺序

1. **第一阶段（止血，解决状态机与核心流程 Bug）**：修复 `JobStatus.isActive` 边界（1.2）→ 修复 `StackWorker` 取消异常与 `NonCancellable` 写库（1.1）→ 修复 `RecoverJobsUseCase` 与 WorkManager 竞态（1.3）→ 修复“继续添加”覆盖（1.4）与删帧重算校验（1.5）。
2. **第二阶段（稳定性与防崩溃）**：改造 `FrameMetadataReader` / `ImageDecoder` / `TiffWriter` / `BmpWriter` 流式低内存处理（2.1）→ 修复导航返回死循环与首页进度跳转（2.2、2.3）→ 修复 `LocalContext as Activity`（2.5）与重复保存相册逻辑（2.4）。
3. **第三阶段（体验与工程化打磨）**：补齐 `currentName` 进度上报、Room `@Transaction`、暂存文件清理、XML 与 ProGuard 配置（3.1–3.6），随后清理死代码并补充核心领域层单测（4.1–4.4）。