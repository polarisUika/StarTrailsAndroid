# StarTrails Android 界面文字（UI Text）索引

> 扫描日期：2026-11-19
> 扫描范围：`app`、`core:model`、`core:designsystem`、`core:data`、`core:domain`、`core:imaging`、`feature:stacker`、`feature:gallery`、`feature:settings` 的源码与资源文件（排除 `build/`、`artifacts/`、测试代码与 `docs/`）。
> 扫描口径：Compose 文本组件与 `contentDescription`、Snackbar/通知文案、`strings.xml`/Manifest、错误与状态文案映射、由数据派生的日期/尺寸/文件名文本。共定位 **22 个文字相关模块（T1–T22）**，覆盖 34 个文件；源码中扫描到 **212 处中文字符串字面量**（含注释、内部异常与技术标记）。
> 行号为扫描时行号，后续改动可能漂移；路径均相对于仓库根目录。

## 1. 总览

### 1.1 文字层级

| 层级 | 职责 | 对应索引 |
| --- | --- | --- |
| 资源与清单层 | 系统读取的应用名、通知渠道名 | T1 |
| 文案定义层 | 稳定枚举值 → 中文文案的唯一映射 | T2、T3 |
| 文本渲染层 | 统一标题、说明、信息行、空状态、控件文字的样式与布局 | T4、T5 |
| 页面展示层 | 各页面可见文案、按钮、对话框、无障碍描述 | T6–T11、T14、T16 |
| 运行时更新层 | Snackbar、进度、动态标题、作品条目文本 | T12、T13、T15、T17 |
| 领域与平台层 | 异常提示、通知内容、相册/分享文件名 | T18–T22 |

### 1.2 文字流转链路

```text
ErrorCode / BlendMode / OutputFormat
        │ userMessage() / displayName() / description()
        ▼
Composable 页面 ──Text(...) ──> 屏幕
        ▲
        │ state.message / progress / frames / job
StackerViewModel / GalleryViewModel
        ▲
        │ errorMessage / 进度回调 / 任务状态
StackWorker、DomainFailure、ImagingException、Room
        │
        └──> Snackbar、系统通知、相册与分享文件名
```

### 1.3 索引约定

- **定义**：产生或集中维护文案；**展示**：把文案渲染到界面；**更新**：运行时改写文案。
- `T*` 编号在全文交叉引用。
- 行号区间表示该文字块的起止范围（含参数与容器）。

## 2. 章节索引总表

| 编号 | 章节/模块 | 索引位置（文件:行） | 职责 | 功能说明 |
| --- | --- | --- | --- | --- |
| T1 | 资源与清单 | `app/src/main/res/values/strings.xml:1-5`；`app/src/main/AndroidManifest.xml:15,56` | 定义 | 应用名 `StarTrails 星轨`、通知渠道名；Manifest 引用 |
| T2 | 错误/模式/格式文案映射 | `core/model/src/main/java/com/startrails/core/model/ErrorMessages.kt:4-35` | 定义 | 错误码、叠加模式、输出格式的中文名与说明 |
| T3 | 稳定枚举与失败模型 | `ErrorCode.kt:4-25`；`BlendMode.kt:4-11`；`OutputFormat.kt:4-18` | 定义 | 持久化 key 与错误码，决定文案选择 |
| T4 | 通用文字渲染组件 | `StarTrailsTopBar.kt:27-74`；`CommonSurfaces.kt:50-154`；`EmptyState.kt:28-70`；`Controls.kt:37-208` | 展示 | 标题/副标题、分区、信息行、说明、空状态、开关、滑块、分段按钮 |
| T5 | 字体与排版 | `core/designsystem/.../theme/Type.kt:8-122` | 定义 | 15 级 MD3 字阶、中文字距、行高 |
| T6 | 导航外壳 | `app/src/main/java/com/startrails/ui/AppNavigation.kt:69-80,133-140,163-169,194-217,232-247` | 展示 | 三个入口名称与品牌标识 |
| T7 | 叠加首页 | `feature/stacker/.../HomeScreen.kt:80-110,128-145,180-216,222-267,312-445` | 展示 | 主视觉、CTA、三步流程、草稿/运行中卡片、隐私说明 |
| T8 | 照片检查页 | `feature/stacker/.../ReviewScreen.kt:64-114,119-135,195-240` | 展示 | 动态张数标题、校验提示、照片行信息与异常原因 |
| T9 | 叠加参数页 | `feature/stacker/.../StackSettingsScreen.kt:64-199` | 展示 | 模式说明、格式菜单、质量滑块、递归开关、任务摘要 |
| T10 | 处理进度页 | `feature/stacker/.../ProcessingScreen.kt:55-170` | 展示 | 状态标题、百分比、帧计数、当前文件、提示语 |
| T11 | 结果页 | `feature/stacker/.../ResultScreen.kt:82-307` | 展示 | 作品信息、保存/导出/分享/删除、预览工具条、删除确认 |
| T12 | 叠加状态与提示更新 | `feature/stacker/.../StackerViewModel.kt:35-50,94-104,156-163,308-405` | 更新 | `message`、来源标签、失败/取消/成功文案 |
| T13 | 叠加流程装配 | `feature/stacker/.../StackerRoute.kt:62-77,99-147` | 更新/展示 | Snackbar、系统分享选择器、阶段切换 |
| T14 | 作品页 | `feature/gallery/.../GalleryRoute.kt:74-348` | 展示 | 空状态、网格卡片文本、菜单、删除确认、分享/导出 |
| T15 | 作品条目派生文本 | `feature/gallery/.../GalleryViewModel.kt:32-44,67-127` | 更新 | 日期、模式、大小、失效判断与提示消息 |
| T16 | 设置页 | `feature/settings/.../SettingsRoute.kt:61-245` | 展示 | 外观、默认参数、隐私说明、关于 |
| T17 | 设置状态 | `feature/settings/.../SettingsViewModel.kt:17-44` | 更新 | 偏好写入（本身不含文案） |
| T18 | 领域错误文案 | `core/domain/.../DomainFailure.kt:5-18`；`StartStackUseCase.kt:29-36`；`PrepareStackUseCase.kt:47-77`；`ResultUseCases.kt:19-58` | 定义 | 权限、内存、存储、帧数、忙碌、导出失败等提示 |
| T19 | 图像异常文案 | `core/imaging/.../ImagingException.kt:6-35`；`ImageStacker.kt:18-38`；`ImageEncoder.kt:36-43` | 定义 | 解码、尺寸不一致、权限丢失、内存、编码失败 |
| T20 | 通知与文件名 | `core/domain/.../StackWorker.kt:42-143`；`core/data/.../ResultRepository.kt:29-106` | 定义/更新 | 通知渠道、标题、进度、取消动作、相册/暂存文件名 |
| T21 | 进程恢复提示 | `core/data/.../StackJobRepository.kt:66-80` | 更新 | 中断任务标记与提示文案 |
| T22 | 照片显示名派生 | `core/data/.../FrameSourceRepository.kt:57-69,93-121` | 展示 | 从 SAF 查询 `DISPLAY_NAME`，作为列表与进度中的文件名 |

## 3. 分模块说明

### 3.1 资源与清单层（T1）

| 文案 | 位置 | 说明 |
| --- | --- | --- |
| `StarTrails 星轨` | `app/src/main/res/values/strings.xml:3` | `app_name`，启动器与应用信息中的名称 |
| `叠加任务` | `strings.xml:4` | `stack_notification_channel`，**当前未被任何代码引用** |
| `@string/app_name` | `AndroidManifest.xml:15,56` | 应用与 `MainActivity` 标签 |

**备注**

- 全仓库 Kotlin 代码中 `stringResource` / `R.string` 命中数为 **0**；除 `app_name` 外所有界面文案均为硬编码字面量。
- 通知渠道名在 `StackWorker.kt:117` 另有一份硬编码 `"叠加任务"`，与资源重名但未复用。
- 无 `values-zh` / `values-en` 等语言目录，默认资源即中文，不存在语言回退链。

### 3.2 文案定义与映射层（T2、T3）

`ErrorMessages.kt` 是唯一集中式文案映射，四个扩展函数：

| 函数 | 行号 | 用途 | 调用方 |
| --- | --- | --- | --- |
| `ErrorCode.userMessage()` | `4-15` | 错误码 → 用户可读提示 | `ReviewScreen:227`、`StackerViewModel:96,402`、`GalleryViewModel:123` |
| `BlendMode.displayName()` | `17-21` | 叠加模式短名 | `HomeScreen:411`、`StackSettingsScreen:113`、`ResultScreen:108`、`SettingsRoute:130`、`GalleryViewModel:107` |
| `BlendMode.description()` | `23-27` | 模式用途说明 | `StackSettingsScreen:116` |
| `OutputFormat.displayName()` | `29-35` | 输出格式短名 | `StackSettingsScreen:132,151`、`ResultScreen:109`、`SettingsRoute:222,236`、`GalleryViewModel:108` |

错误码完整映射：

| 错误码 | 文案 | 行号 |
| --- | --- | --- |
| `NOT_ENOUGH_FRAMES` | 至少选择两张照片才能叠加 | `5` |
| `UNSUPPORTED_FORMAT` | 包含不支持的图片格式 | `6` |
| `DECODE_FAILED` | 有照片无法读取，请移除后重试 | `7` |
| `DIMENSION_MISMATCH` | 照片尺寸不一致，请移除异常照片 | `8` |
| `URI_PERMISSION_LOST` | 照片访问权限已失效，请重新选择 | `9` |
| `INSUFFICIENT_MEMORY` | 图片过大，请减少数量或使用较小原图 | `10` |
| `INSUFFICIENT_STORAGE` | 存储空间不足，请清理后重试 | `11` |
| `OUTPUT_FAILED` | 保存失败，原片未受影响，可重试导出 | `12` |
| `CANCELLED` | 任务已取消 | `13` |
| `UNKNOWN` | 出现未知问题，请重试 | `14` |

叠加模式与输出格式：

| 枚举 | 名称 | 说明（仅模式） | 行号 |
| --- | --- | --- | --- |
| `LIGHTEN` | 最大值 | 逐通道取最大值，标准星轨合成，保留每帧最亮的星点。 | `18,24` |
| `SCREEN` | 滤色 | 滤色混合，叠加更柔和，但天空会逐渐变亮。 | `19,25` |
| `AVERAGE` | 平均 | 逐像素求平均，适合降噪，不会形成分离的星轨。 | `20,26` |
| `JPEG/PNG/TIFF/WEBP/BMP` | `JPEG/PNG/TIFF/WebP/BMP` | — | `30-34` |

**备注**

- `ErrorCode.key`、`BlendMode.key`、`OutputFormat.key` 会写入 Room 与 WorkManager，属于**持久化标识**；修改会破坏历史数据（`fromKey` 会静默回退到 `UNKNOWN` / `LIGHTEN` / `JPEG`）。
- `JobStatus`（`JobStatus.kt:4-21`）只有状态值，没有 `displayName`；状态文案在页面与 ViewModel 中各自硬编码（见 T10、T12）。
- `ThemeMode`（`UserPreferences.kt:4-12`）同样没有 `displayName`，三档主题名内联在设置页 `SettingsRoute.kt:96-100`，与 `BlendMode`/`OutputFormat` 的处理方式不一致。

### 3.3 通用文字渲染组件（T4、T5）

| 组件 | 位置 | 文字参数与行为 |
| --- | --- | --- |
| `StarTrailsTopBar` | `StarTrailsTopBar.kt:27-74` | `title:28`、`subtitle:30`；标题 `titleLarge` 单行省略 `37-42`，副标题 `bodySmall` 单行省略 `43-50`；返回按钮内容描述固定为 `"返回"` `60` |
| `SectionCard` | `CommonSurfaces.kt:50-82` | 可选 `title` 渲染为 `titleSmall` + `onSurfaceVariant` `73-79` |
| `SectionHeader` | `CommonSurfaces.kt:87-108` | `title` 主色 `titleSmall` `96-100`，`description` 次色 `bodySmall` `101-107` |
| `InfoRow` | `CommonSurfaces.kt:113-135` | 左标签 `bodyMedium` 次色、右值 `titleSmall` 主色并右对齐 `122-133` |
| `SupportText` | `CommonSurfaces.kt:139-154` | 弱化说明 `bodySmall`；`emphasis=true` 时切换为 `error` 色 `148-152` |
| `EmptyState` | `EmptyState.kt:28-70` | `title` 居中 `titleMedium` `54-58`，`description` 居中 `bodyMedium`、最大宽 320dp `59-65` |
| `SwitchRow` | `Controls.kt:37-85` | `title` `bodyLarge`、`description` `bodySmall`，禁用时标题降为次色 `61-76` |
| `LabeledSlider` | `Controls.kt:92-130` | `title` `bodyLarge`；右侧当前值 `"$value$valueSuffix"` `labelLarge` 主色 `106-111`；支持说明 `122-128` |
| `SegmentedChoice` | `Controls.kt:184-208` | 选项文字由 `label(T)` 注入，`labelLarge` 且强制单行 `198-204` |
| `StarTrailsTypography` | `Type.kt:16-122` | 15 级字阶；中文标题 Medium/SemiBold、字距收紧，全部 `sp` 支持系统字体缩放 |

**备注**

- 组件层几乎不持有业务文案，唯一内嵌文本是返回按钮的 `"返回"`；文案内容由各页面注入，改文案时只需改页面/映射层。
- `SupportText` 同时承担“隐私说明”和“表单错误提示”两种语义，错误提示仅靠颜色区分（`emphasis=true`），不符合“状态不能只靠颜色表达”的规范，建议后续补图标或 `Modifier.semantics`。
- `StarTrailsTopBar` 的标题与副标题均 `maxLines = 1`，长文案或大字体下会被省略，需按 `DEVELOPMENT_GUIDE.md` §13.5 第 9 条验收。

### 3.4 导航外壳（T6）

| 文案块 | 位置 | 说明 |
| --- | --- | --- |
| 入口名称 `叠加` / `作品` / `设置` | `AppNavigation.kt:76-80` | `NavItem.label`，三种布局共用同一列表 |
| 底部栏标签 | `AppNavigation.kt:138` | `NavigationBarItem` 的 `label`，`alwaysShowLabel = true` |
| 侧栏标签 | `AppNavigation.kt:168` | `NavigationRailItem` |
| 抽屉标签 | `AppNavigation.kt:205-211` | `NavigationDrawerItem`，`labelLarge` 单行省略 |
| 品牌标题 `StarTrails` / `星轨合成` | `AppNavigation.kt:239,241` | 仅 `PermanentNavigationDrawer` 头部显示 |
| 图标内容描述 | `AppNavigation.kt:135,165,202` | 均为 `null`，语义依赖可见文字标签 |

**备注**

- 三种响应式布局复用同一 `items`，目的地名称和顺序一致；新增入口只需改 `76-80`。
- 图标 `contentDescription = null` 对“图标+文字”导航项是合理的，TalkBack 会读取 `label`；若未来出现仅图标模式，需要补描述。

### 3.5 叠加首页（T7）

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 标题 `StarTrails` / 副标题 `星轨合成` | `HomeScreen.kt:82-83` | 页面顶部品牌 |
| 徽标 `本地处理` | `HomeScreen.kt:101-105` | 顶部操作区的隐私暗示 |
| 主视觉标题 `把星空叠成光轨` | `HomeScreen.kt:255-259` | `headlineSmall` |
| 主视觉正文 | `HomeScreen.kt:260-264` | 说明选片方式、本机计算、原片只读 |
| `选择照片` / `选择文件夹` | `HomeScreen.kt:136,144` | 两个 CTA，主按钮 56dp、次按钮 52dp |
| 流程卡 `三步完成` | `HomeScreen.kt:183` | `SectionCard` 标题 |
| 三步标题与说明 | `HomeScreen.kt:184-186` | “选择照片/检查与参数/合成与保存”及各自说明 |
| 步骤序号 / 标题 / 说明 | `HomeScreen.kt:199-213` | `WorkflowStep` 内部渲染 |
| `继续上次编辑` | `HomeScreen.kt:333-337` | 草稿卡片标题 |
| `{n} 张照片 · {sourceLabel ?: "未命名来源"}` | `HomeScreen.kt:338-342` | 草稿摘要，来源为空时显示“未命名来源” |
| `继续` | `HomeScreen.kt:345-348` | 草稿卡片按钮 |
| `叠加任务进行中` | `HomeScreen.kt:387-392` | 运行中卡片标题 |
| `{progress}/{total}` | `HomeScreen.kt:393-397` | 右上角计数 |
| `{模式} · 已合成 {n}%` | `HomeScreen.kt:410-415` | 进度说明；百分比由浮点截断 |
| `查看进度` | `HomeScreen.kt:416-419` | 跳转处理页 |
| 隐私说明 | `HomeScreen.kt:438-443` | “不申请网络权限……” |

**备注**

- 文案同时承担产品定位、操作引导与隐私承诺，是首页信息密度最高的区域。
- 草稿来源文案依赖 `StackerViewModel` 写入的 `sourceLabel`（T12），并非用户输入的文件夹名（见风险 1、3）。

### 3.6 照片检查页（T8）

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 动态标题 `已选择 {n} 张` | `ReviewScreen.kt:67` | 随 `state.frames.size` 更新 |
| 副标题 `检查尺寸与顺序后继续` | `ReviewScreen.kt:68` | 固定引导 |
| `有照片存在问题，请移除后再继续` | `ReviewScreen.kt:75-78` | `hasIssue` 时以错误色显示 |
| `至少需要两张照片` | `ReviewScreen.kt:80` | 数量不足时显示 |
| `清空` / `继续添加` / `下一步` | `ReviewScreen.kt:93,101,110` | 底部操作栏；`下一步` 受 `validFrames` 控制 |
| 空状态 `还没有照片` + 说明 + `选择照片` | `ReviewScreen.kt:128-131` | 无帧时占位 |
| 缩略图描述 `第 {n} 张缩略图` | `ReviewScreen.kt:197` | 无障碍文本 |
| 文件名 | `ReviewScreen.kt:205-210` | `frame.displayName`，单行省略 |
| `第 {n} 张 · {宽×高}` 或 `读取中` | `ReviewScreen.kt:211-216` | 尺寸未知时显示“读取中” |
| 异常原因 | `ReviewScreen.kt:226-230` | 优先级：`issueDetail` → `issueCode.userMessage()` → `尺寸与其他照片不同` |
| `移除 {文件名}` | `ReviewScreen.kt:237` | 删除图标的内容描述 |

**备注**

- `hasIssue` / `validFrames`（`61-62`）同时控制提示文案与按钮可用性，文案与业务校验强耦合。
- 异常原因由 `PrepareStackUseCase.kt:74-76` 生成，包含基准尺寸与本图尺寸，是页面中信息量最大的动态文案。

### 3.7 叠加参数页（T9）

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 标题 `叠加参数` / 副标题 `第 2 步 · 共 3 步` | `StackSettingsScreen.kt:71-72` | 流程位置提示 |
| `开始叠加` | `StackSettingsScreen.kt:85` | 主操作，受帧数与异常状态控制 |
| `合成期间可在其他页面继续使用，任务不会中断。` | `StackSettingsScreen.kt:87` | 底部辅助说明 |
| 分区 `叠加模式` + `不同算法适合不同的星轨效果` | `StackSettingsScreen.kt:104-107` | 区块标题与说明 |
| 模式按钮文字 | `StackSettingsScreen.kt:113` | 来自 `displayName()` |
| 当前模式说明 | `StackSettingsScreen.kt:115-119` | 来自 `description()`，随选择切换 |
| 分区 `输出设置` | `StackSettingsScreen.kt:123` | 区块标题 |
| 格式字段值 / 标签 / 菜单项 | `StackSettingsScreen.kt:132,136,151` | 值来自 `displayName()` |
| `输出质量` + 支持说明 | `StackSettingsScreen.kt:167,170` | 仅 JPEG/WebP 可见 |
| `包含子文件夹` + 说明 | `StackSettingsScreen.kt:176-179` | 仅文件夹来源时显示 |
| 分区 `本次任务` | `StackSettingsScreen.kt:185` | 区块标题 |
| `照片帧数` / `输出尺寸` | `StackSettingsScreen.kt:189-197` | 值含单位“张”“×”；尺寸未就绪显示 `待检测` |
| `原片始终以只读方式访问，不会被覆盖。` | `StackSettingsScreen.kt:198` | 隐私说明 |

**备注**

- **高危耦合**：`StackSettingsScreen.kt:65` 用 `state.job?.sourceLabel == "文件夹来源"` 判断是否显示递归开关；该字符串由 `StackerViewModel.kt:162` 写入。任何文案改动都会直接改变功能可见性。
- 质量滑块未传 `valueSuffix`，界面只显示 1–100 的裸数字；`LabeledSlider` 默认 `valueSuffix = ""`（`Controls.kt:97`）。
- 输出尺寸分支在 `job.width == 0` 时统一显示“待检测”，与检查页的“读取中”是两套措辞。

### 3.8 处理进度页（T10）

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 标题 `正在叠加` / `任务已结束` | `ProcessingScreen.kt:58` | 由 `stopped` 切换 |
| 副标题 `第 3 步 · 共 3 步` | `ProcessingScreen.kt:59` | 仅运行中显示 |
| `{percent}%` | `ProcessingScreen.kt:124-128` | 大号百分比，成功时强制 100% |
| `{progress}/{total} 帧` | `ProcessingScreen.kt:129-133` | 帧计数 |
| 状态标题 | `ProcessingScreen.kt:141-149` | 结束态优先 `state.message`，否则“任务已结束”；运行中固定“正在合成星轨” |
| 当前文件名 | `ProcessingScreen.kt:151-158` | 空值回退“正在准备照片…”；两行省略 |
| 结束/运行提示 | `ProcessingScreen.kt:160-169` | “可以重新执行，或返回检查照片。” / “逐帧读取、逐帧合成……” |
| `重新执行` | `ProcessingScreen.kt:72` | 结束态主操作 |
| `任务停止后不会修改任何原始照片。` | `ProcessingScreen.kt:74-77` | 结束态说明 |
| `取消任务` | `ProcessingScreen.kt:85` | 运行中操作 |
| 后台运行说明 | `ProcessingScreen.kt:87-90` | 说明离开页面不中断、通知栏显示进度 |

**备注**

- `succeeded` 通过 `state.job?.status?.name == "SUCCEEDED"` 判断（`46`），属于字符串比较枚举，重命名枚举会静默失效，建议改为 `JobStatus.SUCCEEDED`。
- 失败原因最终来自 `state.message`，由 `StackerViewModel` 的任务状态收集逻辑写入（`94-104`）；页面自身不调用 `userMessage()`，文案更新点集中在 T12。
- 进度与文件名来自 `StackWorker` 的 `onProgress` 回调（`StackWorker.kt:55-57`），每帧刷新一次。

### 3.9 结果页（T11）

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 标题 `星轨作品` / 副标题 `合成完成` | `ResultScreen.kt:85-86` | 结果页固定头部 |
| 卡片标题 `作品信息` | `ResultScreen.kt:105` | 信息分区 |
| `尺寸` / `帧数` / `模式` / `格式` / `耗时` / `大小` | `ResultScreen.kt:106-111` | 键值信息；模式与格式走 `displayName()` |
| `保存到相册` | `ResultScreen.kt:123` | 主操作 |
| `导出文件` / `分享` | `ResultScreen.kt:132,142` | 次操作 |
| `新建任务` / `删除结果` | `ResultScreen.kt:154,171` | 文本按钮，删除为错误色 |
| 删除对话框标题/正文 | `ResultScreen.kt:188-189` | “删除这个作品？”/“只删除合成结果，不会影响任何原始照片。” |
| `删除` / `取消` | `ResultScreen.kt:200,204` | 确认与取消 |
| 预览图描述 `星轨作品大图，可双指缩放` | `ResultScreen.kt:243` | 无障碍文本 |
| `预览不可用` | `ResultScreen.kt:255-260` | 无输出 URI/文件时占位 |
| `缩小` / `放大` | `ResultScreen.kt:275,288` | 缩放按钮内容描述 |
| `{scale}%` | `ResultScreen.kt:277-282` | 当前缩放比例 |
| `重置` | `ResultScreen.kt:297` | 恢复 100% |
| 文件大小格式化 | `ResultScreen.kt:303-307` | `B` / `KB` / `MB` |

**备注**

- “耗时”用整数秒（`elapsedMs / 1000`），不足 1 秒显示 `0 秒`。
- `formatSize` 按 1024 进制换算却标注 `KB/MB`，实际口径为 `KiB/MiB`，且向下取整。
- 删除确认文案与作品页完全重复但各自维护（见风险 3）。

### 3.10 运行时文案更新：叠加流程（T12、T13）

`StackerViewModel`：

| 更新点 | 位置 | 文案/行为 |
| --- | --- | --- |
| UI 状态字段 | `StackerViewModel.kt:48` | `message: String?` 作为一次性提示 |
| 失败/取消/成功映射 | `94-104` | `FAILED → errorCode.userMessage() ?: errorMessage`；`CANCELLED → "任务已取消"`；成功进入结果页时清空 |
| 选片来源标签 | `158` | `"已选择 {n} 张照片"` 写入 `sourceLabel` |
| 文件夹来源标签 | `162` | 固定字面量 `"文件夹来源"`（见风险 1） |
| 取消提示 | `313` | `"任务已取消"` |
| 保存成功 | `326` | `"已保存到相册"` |
| 导出成功 | `341` | `"文件已导出"` |
| 删除成功 | `368` | `"作品已删除"` |
| 异常兜底 | `399-405` | `DomainFailure.message` → 否则 `code.userMessage()`；未知异常用 `UNKNOWN` 文案 |
| 消费提示 | `373` | `consumeMessage()` 清空，避免重复 Snackbar |
| 返回首页清空 | `258-260` | 同时清空 `message` |

`StackerRoute`：

| 更新点 | 位置 | 文案/行为 |
| --- | --- | --- |
| Snackbar 展示 | `73-77` | 读取 `state.message`，展示后立即消费 |
| 系统分享选择器标题 | `69` | `"分享星轨作品"` |
| 阶段切换 | `99-147` | 按 `StackerStage` 渲染五个页面，无额外文案 |

**备注**

- `errorCode?.userMessage() ?: errorMessage` 中 `userMessage()` 恒返回非空字符串，所以只要 `errorCode != null`，`errorMessage` 永远不会显示。这符合“只显示可理解信息”的设计，但也导致 `StackJobRepository` 的中断提示（T21）和包含文件名的详细异常无法到达界面。
- `message` 同时用于 Snackbar 与处理页状态标题，同一字段承担两种展示语义；失败时 Snackbar 与页面文案可能重复出现。

### 3.11 作品页（T14、T15）

`GalleryRoute`：

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 标题 `作品` | `GalleryRoute.kt:111` | 固定 |
| 副标题 `合成结果会保存在这里` / `共 {n} 个作品` | `GalleryRoute.kt:112` | 空列表与有数据两种 |
| 空状态 `还没有星轨作品` + 说明 + `去叠加照片` | `GalleryRoute.kt:129-132` | 引导返回叠加 |
| 删除对话框 | `GalleryRoute.kt:172-173,184,188` | 与结果页同义但不共享 |
| 卡片图描述 `{模式} 星轨作品` | `GalleryRoute.kt:229` | 无障碍文本 |
| `文件不可用` | `GalleryRoute.kt:249-253` | 文件缺失/URI 失效占位 |
| 日期 | `GalleryRoute.kt:276-280` | `item.dateText` |
| `{模式} · {大小}` | `GalleryRoute.kt:281-287` | 单行省略 |
| `更多操作` | `GalleryRoute.kt:304` | 菜单按钮描述 |
| 菜单 `打开` / `分享` / `导出` / `删除` | `GalleryRoute.kt:310,319,328,337` | 前三项受 `available` 控制，删除不受限 |
| 分享选择器标题 | `GalleryRoute.kt:97` | `"分享星轨作品"` |
| 导出默认文件名 | `GalleryRoute.kt:151` | `star-trails.{extension}`，显示在系统创建文件对话框 |

`GalleryViewModel`：

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| `GalleryItem` 文本字段 | `GalleryViewModel.kt:32-38` | `dateText` / `modeText` / `sizeText` 预格式化 |
| 条目映射 | `99-110` | 日期 `yyyy-MM-dd HH:mm`；模式走 `displayName()`；大小 `{n} KB` 或格式名 |
| 导出成功提示 | `71` | `"文件已导出"` |
| 删除成功提示 | `93` | `"作品已删除"` |
| 异常兜底 | `121-127` | `DomainFailure.message` → 否则 `"操作失败"` |

**备注**

- `dateText` 每次重建列表都会新建 `SimpleDateFormat`，且格式固定不随 Locale 变化；中文环境下显示为 `2026-11-19 21:30`。
- `sizeText` 仅处理 `KB`，大于 1MB 时仍显示大数字（如 `15360 KB`），且 0 字节时回退为格式名（`108`）。
- 删除菜单项未像其它项一样检查 `available`（`336-343`），失效作品也可触发删除确认。

### 3.12 设置页（T16、T17）

| 文案块 | 位置 | 功能说明 |
| --- | --- | --- |
| 标题 `设置` / 副标题 `外观、默认参数与隐私` | `SettingsRoute.kt:70-71` | 页面头部 |
| 分区 `外观` | `SettingsRoute.kt:88` | 区块标题 |
| `主题` | `SettingsRoute.kt:90` | 设置行标题 |
| `跟随系统` / `浅色` / `深色` | `SettingsRoute.kt:96-100` | 主题分段按钮，内联 `when` |
| `动态配色` + 两种说明 | `SettingsRoute.kt:106-116` | 支持时“跟随系统壁纸取色……”；不支持时“需要 Android 12 或更高版本” |
| 分区 `新建任务默认参数` + 说明 | `SettingsRoute.kt:120-121` | “每次开始新任务时自动套用” |
| `叠加模式` | `SettingsRoute.kt:125` | 设置行标题 |
| 模式按钮 | `SettingsRoute.kt:126-131` | 来自 `displayName()` |
| `默认输出格式` | `SettingsRoute.kt:226` | 下拉字段标签 |
| 格式菜单项 | `SettingsRoute.kt:236` | 来自 `displayName()` |
| `JPEG / WebP 质量` | `SettingsRoute.kt:142-146` | 滑块标题，当前值由组件渲染 |
| `文件夹默认包含子文件夹` | `SettingsRoute.kt:150-154` | 开关行 |
| 分区 `存储与隐私` | `SettingsRoute.kt:157-160` | 区块标题 |
| 四条隐私说明 | `SettingsRoute.kt:162-177` | 原片只读、私有目录暂存、无网络、持久授权 |
| 分区 `关于` | `SettingsRoute.kt:180-183` | 区块标题 |
| `版本` / `1.0` | `SettingsRoute.kt:185` | 硬编码版本号 |
| `合成算法` / `最大值 · 滤色 · 平均` | `SettingsRoute.kt:186` | 与枚举名称重复维护 |
| 算法说明 | `SettingsRoute.kt:187` | “算法与桌面版保持一致，全部在设备本地执行。” |

**备注**

- `SettingsViewModel`（`17-44`）只负责读写 DataStore，不产生文案；页面通过 `collectAsState` 直接刷新，没有“已保存”提示。
- 版本号 `1.0` 硬编码，`app/build.gradle.kts:18` 另有一份 `versionName = "1.0"`，二者无程序化关联，后续升版需手动同步。

### 3.13 领域与图像层文案（T18、T19、T21）

`DomainFailure` 工厂：

| 工厂 | 文案 | 位置 |
| --- | --- | --- |
| `permission(name)` | `无法访问：{name}，请重新选择照片` | `DomainFailure.kt:12` |
| `memory()` | `图片过大，请减少照片数量或使用较低分辨率的原图` | `13` |
| `storage()` | `存储空间不足，请清理后重试` | `14` |
| `notEnoughFrames()` | `至少需要 2 张照片才能叠加` | `15` |
| `busy()` | `已有一个叠加任务在运行，请等待完成或取消` | `16` |

其他领域文案：

| 文案 | 位置 | 触发场景 |
| --- | --- | --- |
| `任务不存在` | `StartStackUseCase.kt:29`、`ResultUseCases.kt:20` | 任务 ID 失效 |
| `存在尺寸不一致的照片，请先移除` | `StartStackUseCase.kt:35` | 启动前校验 |
| `读取照片失败` | `PrepareStackUseCase.kt:53` | 元数据读取异常时的兜底 |
| `基准 {w}×{h}，本图 {w}×{h}` | `PrepareStackUseCase.kt:74-76` | 检查页 `issueDetail` |
| `临时结果已丢失，请重新执行任务` | `ResultUseCases.kt:23` | 保存时暂存文件缺失 |
| `导出失败` / `结果文件不可用` | `ResultUseCases.kt:35,52,55` | 导出与分享失败 |
| `任务被中断，可以重新执行` | `StackJobRepository.kt:75` | 进程被杀后的恢复提示（当前不可见，见风险 4） |

`ImagingException`：

| 异常 | 文案 | 位置 |
| --- | --- | --- |
| `Decode` | `无法读取图像：{name}` | `ImagingException.kt:12-13` |
| `DimensionMismatch` | `图像尺寸不一致：{a} 为 {w×h}，{b} 为 {w×h}` | `15-23` |
| `PermissionLost` | `无法访问照片：{name}，请重新选择` | `25-26` |
| `Cancelled` | `任务已取消` | `28` |
| `Memory` | `内存不足，图像过大` | `30-31` |
| `Output` | 透传调用方消息 | `33-34` |

其他图像层文案：

| 文案 | 位置 | 说明 |
| --- | --- | --- |
| `(无输入)` | `ImageStacker.kt:18` | 空帧列表时作为文件名占位 |
| 尺寸不一致消息拼装 | `ImageStacker.kt:32-38` | 传入两个文件名与尺寸，构造 `DimensionMismatch` |
| `不支持的输出格式：{key}` | `ImageEncoder.kt:36` | 编码格式兜底 |
| `图像编码失败` | `ImageEncoder.kt:39,43` | 编码失败 |

**备注**

- 这些文案是界面文案的“第二来源”，与 `ErrorMessages.kt` 存在同义多版本（如“至少…两张”“任务已取消”“权限失效”）。
- `ImagingException.DimensionMismatch` 的详细信息比 `ErrorCode.userMessage()` 更具体，但因 `errorCode` 非空，最终通常只显示通用文案。
- 这些失败经 `StackerViewModel.report()`（`399-405`）或 `GalleryViewModel.report()`（`121-127`）进入 Snackbar，属于用户可见文本；当前消息只含文件名与尺寸，不含完整路径，符合隐私要求。

### 3.14 通知与平台可见文本（T20、T22）

`StackWorker`：

| 文案/文本 | 位置 | 说明 |
| --- | --- | --- |
| `准备中` | `StackWorker.kt:42` | 前台服务初始通知 |
| `开始处理` | `50` | 任务启动通知 |
| `叠加完成` | `80` | 完成通知 |
| 通知渠道名 `叠加任务` | `116-119` | 硬编码；`strings.xml:4` 的同名资源未使用 |
| 通知标题 `正在叠加星轨` | `125` | 固定 |
| 通知正文 `正在处理 {processed}/{total} · {name}` | `126` | `total=0` 时只显示 `name` |
| 动作 `取消任务` | `130` | 系统通知按钮 |
| 失败提示 | `85,87,89` | 权限失效、内存不足、叠加失败 |

`ResultRepository` 与 `FrameSourceRepository`：

| 文本 | 位置 | 说明 |
| --- | --- | --- |
| 暂存/相册文件名 `star-trails-{id8}.{ext}` | `StackWorker.kt:62`、`ResultRepository.kt:31` | 用户可在相册看到 |
| 相册目录 `Pictures/StarTrails` | `ResultRepository.kt:35` | MediaStore 相对路径 |
| `无法创建相册条目` | `39,41` | 保存失败 |
| `保存到相册失败` | `50` | 保存失败 |
| `导出文件失败` | `61` | 导出失败 |
| `无法写入目标位置` | `105` | 输出流为空 |
| 照片显示名 | `FrameSourceRepository.kt:59,101,116` | 从 SAF `DISPLAY_NAME` 取得，进入检查页/进度/通知 |

**备注**

- 通知渠道名、应用名、相册目录名是三类不同的“用户可见标识”，分别由硬编码、`strings.xml`、硬编码维护，没有统一命名来源。
- 分享临时文件名 `share-{id8}.{ext}`（`ResultUseCases.kt:45`）通常只在分享目标应用中可见。

## 4. 运行时文字更新链路

| 触发 | 写入/格式化位置 | 展示位置 | 说明 |
| --- | --- | --- | --- |
| 选择照片/文件夹 | `StackerViewModel.kt:158,162` | `HomeScreen.kt:338-342` | `sourceLabel` 作为草稿摘要 |
| 任务失败 | `StackerViewModel.kt:94-97` | `ProcessingScreen.kt:141-149`、Snackbar | 通用错误文案 |
| 任务取消 | `StackerViewModel.kt:98,313` | 处理页标题 + Snackbar | 两处可能同时出现 |
| 保存/导出/删除 | `StackerViewModel.kt:326,341,368`；`GalleryViewModel.kt:71,93` | Snackbar | 一次性提示，展示后清空 |
| 叠加进度 | `StackWorker.kt:55-57,110-131` | 首页进度卡、处理页、系统通知 | 每帧刷新；百分比由 UI 截断 |
| 作品列表变化 | `GalleryViewModel.kt:99-110` | `GalleryRoute.kt:112,277,282` | 日期/模式/大小预格式化 |
| 设置变更 | `SettingsViewModel.kt:29-39` | `SettingsRoute.kt` 全页 | 无确认提示，直接刷新 |
| 进程恢复 | `StackJobRepository.kt:67-79` | 任务状态/结果页 | 提示文案被 `UNKNOWN` 映射覆盖（风险 4） |

## 5. 备注、风险与维护建议

1. **[高] 文案与业务逻辑耦合**：`StackSettingsScreen.kt:65` 用 `sourceLabel == "文件夹来源"` 决定是否显示“包含子文件夹”。修改 `StackerViewModel.kt:162` 的文案会直接导致开关消失。建议改为 `sourceKind` 枚举或 `isFolderSource: Boolean`。
2. **[高] 无字符串资源化 / 无国际化**：Kotlin 中 `stringResource`、`R.string` 命中为 0，全部文案硬编码；`strings.xml` 仅 `app_name` 生效，`stack_notification_channel` 未被引用。多语言、批量校对、文案审查都缺少统一入口。
3. **[中] 同义文案多版本**：
   - “至少两张”：`ErrorMessages.kt:5`、`ReviewScreen.kt:80`、`DomainFailure.kt:15`、`StartStackUseCase.kt:33`。
   - “任务已取消”：`ErrorMessages.kt:13`、`ImagingException.kt:28`、`StackerViewModel.kt:98,313`。
   - “权限失效”：`ErrorMessages.kt:9`、`StackWorker.kt:85`。
   - 删除确认：`ResultScreen.kt:188-189` 与 `GalleryRoute.kt:172-173`。
   - “文件已导出/作品已删除”：`StackerViewModel.kt:341,368` 与 `GalleryViewModel.kt:71,93`。
4. **[中] 错误详细信息不可达**：`StackerViewModel.kt:94-97` 的 `errorCode?.userMessage() ?: errorMessage` 中，`userMessage()` 恒非空，导致 `StackJobRepository.kt:75` 的“任务被中断，可以重新执行”以及 `ImagingException` 中的文件名/尺寸细节不会显示：中断任务退化为“出现未知问题，请重试”，图像异常退化为对应错误码的通用文案。
5. **[中] 状态判断使用字符串**：`ProcessingScreen.kt:46` 用 `status.name == "SUCCEEDED"`，重命名枚举会静默失效；`JobStatus` 也没有集中文案映射，状态标题分散在页面与 ViewModel。
6. **[中] 单位与精度**：`ResultScreen.kt:110` 的“耗时”取整到秒，短任务显示 `0 秒`；`ResultScreen.kt:303-307` 与 `GalleryViewModel.kt:108` 使用 1024 进制却标注 `KB/MB`；作品页大于 1MB 仍显示 `xxxx KB`。
7. **[中] 通知渠道名不可本地化**：`StackWorker.kt:117` 硬编码“叠加任务”，系统通知设置中不会随语言或资源切换。
8. **[低] 持久化 key 与展示文案需分离**：`ErrorCode.key`、`BlendMode.key`、`OutputFormat.key`、`ThemeMode.key` 已写入 Room/WorkManager；改文案应只改 `userMessage()` / `displayName()`，不要改 `key`。
9. **[低] 无障碍文本维护**：装饰图标使用 `contentDescription = null` 是正确的；导航图标依赖可见 `label`；进度百分比、缩放百分比、处理页帧计数缺少 `stateDescription`，TalkBack 只能读静态文本。`SupportText` 的错误态仅靠颜色区分。
10. **[低] 长文案与大字体**：`StarTrailsTopBar` 标题/副标题、检查页文件名、分段按钮、作品卡片副标题均为单行省略；文案变长或系统字体 1.3–2.0 倍时需按 `DEVELOPMENT_GUIDE.md` §13.5 第 9 条回归。
11. **[低] 版本号与算法名重复**：`SettingsRoute.kt:185-186` 的 `1.0` 与“最大值 · 滤色 · 平均”为手写副本；前者与 `app/build.gradle.kts:18` 的 `versionName` 需手动同步，后者与 `BlendMode` 枚举可能失同步。
12. **[提示] 无 UI 文案测试**：当前仅 `core/imaging` 有算法单元测试，没有 Compose UI 测试或截图基线；文案改动只能靠人工验收。

**维护建议**

- 新增或修改文案时，先判断是否属于 `ErrorMessages.kt` 的映射范畴；页面内只保留场景化短句。
- 改动任何“来源/状态”文案前，先 `grep` 该字符串是否被用于条件判断（当前至少 `"文件夹来源"` 一处）。
- 若后续引入国际化：优先抽取 `strings.xml`，把 `Text("...")` 替换为 `stringResource`，并将 `sourceLabel` 改为结构化字段，避免文案影响逻辑。
- 文案长度调整后，至少回归：首页主视觉、检查页异常行、参数页分段按钮、结果页信息卡、设置页隐私条目。

## 6. 常见文案快速定位

| 关键词 | 全部出现位置 |
| --- | --- |
| 至少…两张照片 | `ErrorMessages.kt:5`、`ReviewScreen.kt:80`、`DomainFailure.kt:15`、`StartStackUseCase.kt:33` |
| 任务已取消 | `ErrorMessages.kt:13`、`ImagingException.kt:28`、`StackerViewModel.kt:98,313` |
| 照片访问权限已失效 | `ErrorMessages.kt:9`、`StackWorker.kt:85` |
| 选择照片 | `HomeScreen.kt:136,184`、`ReviewScreen.kt:131` |
| 删除这个作品？ | `ResultScreen.kt:188`、`GalleryRoute.kt:172` |
| 取消 | `ResultScreen.kt:204`、`GalleryRoute.kt:188`、`StackWorker.kt:130`（取消任务）、`ProcessingScreen.kt:85`（取消任务） |
| 文件已导出 | `StackerViewModel.kt:341`、`GalleryViewModel.kt:71` |
| 作品已删除 | `StackerViewModel.kt:368`、`GalleryViewModel.kt:93` |
| 不申请网络权限 / 不上传 | `HomeScreen.kt:261,439`、`SettingsRoute.kt:172` |
| 原片只读 / 原始照片不受影响 | `HomeScreen.kt:261`、`StackSettingsScreen.kt:198`、`ResultScreen.kt:189`、`SettingsRoute.kt:164` |
