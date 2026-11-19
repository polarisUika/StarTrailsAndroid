# StarTrails Android App 开发指导文档

> 基于现有 Python 星轨照片叠加项目，设计原生 Android 应用。
> UI 遵循 **Material Design 3（MD3）**，核心算法与桌面版保持一致，图像处理完全在本地执行。
>
> 文档版本：1.0  
> 创建日期：2026-11-16  
> 源项目：`d:/workspace/Star_trail`

## 1. 项目概述

### 1.1 产品定位

**名称**：StarTrails 星轨  
**一句话描述**：将固定机位拍摄的连续星空照片合成为星轨作品。

**目标用户**：

- 星空摄影爱好者。
- 需要快速检查星轨效果的摄影师。
- 不希望把原始照片上传到云端的用户。

**核心价值**：

1. 手机上即可完成选图、叠加、预览、保存和分享。
2. 保留桌面版最大值、滤色、平均三种算法。
3. 原片只读，不覆盖、不上传。
4. 大尺寸任务后台执行，支持进度显示和取消。

### 1.2 现有能力迁移

| 现有模块 | 现有能力 | Android 对应设计 |
| --- | --- | --- |
| `discovery.py` | 文件发现、自然排序、去重、递归目录 | `FrameSourceRepository` |
| `imaging.py` | EXIF 方向、透明通道、格式转换、原子写入 | `ImageCodec`、`ResultExporter` |
| `stacker.py` | lighten、screen、average | `StackingEngine` |
| `jobs.py` | 输入数量、尺寸及输出冲突校验 | `PrepareStackJob` |
| `gui/app.py` | 后台任务、进度、取消、预览 | Compose 页面、ViewModel、WorkManager |

不建议通过 Python 套壳直接迁移界面。应采用 **Kotlin + Jetpack Compose Material 3** 重写应用层，并通过像素对照测试保证算法一致。

## 2. 产品范围

### 2.1 V1 功能

- 多选照片或选择照片目录。
- 自动自然排序：`IMG_2` 位于 `IMG_10` 前。
- 多来源去重，目录可选递归扫描。
- 显示缩略图、数量、尺寸和异常状态。
- 支持三种叠加模式：
  - **最大值**：标准星轨合成。
  - **滤色**：柔和叠加。
  - **平均**：降噪。
- 输出 JPEG、PNG、TIFF、WebP、BMP。
- JPEG/WebP 质量设置，范围 1～100，默认 95。
- 后台叠加、实时进度、通知和协作式取消。
- 结果预览、缩放、保存到相册、导出和系统分享。
- 作品历史、重新分享和删除。
- 浅色、深色、跟随系统及动态配色。
- 中文界面，无账号、无云端依赖。

### 2.2 暂不包含

- RAW 解码。
- 星点对齐、自动跟踪或防抖。
- 地景与天空分别蒙版。
- 视频生成、延时摄影。
- 云同步、账号、在线社区。
- 任务断点续算。进程被终止后可以重新执行，但不承诺从中间帧恢复。

## 3. 信息架构与用户流程

### 3.1 顶级导航

手机使用 MD3 `NavigationBar`，包含三个平级入口：

1. **叠加**：新建任务、当前草稿和运行中任务。
2. **作品**：历史结果。
3. **设置**：主题、默认参数、存储与关于。

适配规则：

- 紧凑宽度：`NavigationBar`。
- 中等宽度：`NavigationRail`。
- 展开宽度：可使用永久 `NavigationDrawer` 或双栏布局。
- 三种尺寸下的目的地名称和顺序保持一致。

### 3.2 主流程

```text
选择照片/目录
    ↓
检查照片列表与分辨率
    ↓
设置叠加模式和输出参数
    ↓
开始叠加
    ↓
后台处理与进度显示
    ↓
结果预览
    ↓
保存 / 分享 / 新建任务
```

### 3.3 页面设计

#### A. 叠加首页

- 顶部栏：`StarTrails`。
- 空状态：星轨插图、说明“选择固定机位拍摄的连续照片”。
- 主按钮：`选择照片`。
- 次按钮：`选择文件夹`。
- 有草稿时显示“继续上次编辑”卡片。
- 有运行任务时优先显示任务进度卡片。

#### B. 照片检查页

- 顶部显示“已选择 36 张”。
- 使用缩略图列表，展示文件名、序号及尺寸。
- 支持移除单张照片、清空和继续添加。
- 不将拖拽排序作为必要操作；V1 保持自然排序。
- 异常照片直接显示原因，例如“尺寸与其他照片不同”。
- 少于两张时禁用“下一步”，并显示文字说明。

#### C. 参数设置页

- 使用 MD3 单选分段按钮选择“最大值 / 滤色 / 平均”。
- 当前模式下方显示用途说明。
- 文件夹来源存在时显示“包含子文件夹”开关。
- 输出格式使用下拉菜单。
- JPEG/WebP 显示质量滑块和当前数值；其他格式隐藏该设置。
- 底部使用固定的高强调 `FilledButton`：“开始叠加”。

#### D. 处理页

- 使用确定型 `LinearProgressIndicator`。
- 显示“正在处理 18/60”及当前文件名。
- 提供“取消任务”按钮。
- 离开页面不停止任务，底部导航仍可使用。
- 后台运行时显示系统通知。
- 禁止同时启动第二个高内存任务。

#### E. 结果页

- 大图预览，支持双击和双指缩放，同时提供缩放按钮作为无障碍替代。
- 显示尺寸、帧数、模式、耗时和文件大小。
- 主操作：“保存到相册”。
- 次操作：“导出文件”“分享”“新建任务”。
- 删除结果必须确认，不影响任何原始照片。

#### F. 作品页

- 手机采用双列网格，平板根据宽度增加列数。
- 每项显示缩略图、日期、模式和尺寸。
- 支持打开、分享、导出和删除。
- 权限失效或文件被外部删除时显示“文件不可用”，而不是空白页面。

## 4. Material Design 3 规范

### 4.1 主题

通过统一的 `MaterialTheme` 管理：

- `ColorScheme`
- `Typography`
- `Shapes`

颜色不得散落在各页面中硬编码。

#### 颜色策略

- 品牌方向：深夜蓝搭配星光青，体现星空摄影属性。
- Android 12 及以上默认开启动态配色，用户可关闭。
- 不支持动态配色时使用品牌静态主题。
- 深色主题不是简单反色，避免大面积高饱和色。
- 图片预览区域使用中性深色表面，避免主题色影响星轨观感。

组件必须正确使用颜色角色：

| 场景 | 颜色角色 |
| --- | --- |
| 主按钮 | `primary` / `onPrimary` |
| 选中卡片或模式 | `primaryContainer` / `onPrimaryContainer` |
| 普通页面 | `surface` / `onSurface` |
| 次级信息 | `onSurfaceVariant` |
| 错误 | `error` / `onError` |
| 边界 | `outline`、`outlineVariant` |

### 4.2 字体

使用系统字体及 MD3 默认类型比例，中文保证 Noto Sans CJK 回退。

| 用途 | 样式 |
| --- | --- |
| 页面标题 | `titleLarge` |
| 区块标题 | `titleMedium` |
| 正文 | `bodyLarge` / `bodyMedium` |
| 辅助说明 | `bodySmall` |
| 按钮与标签 | `labelLarge` / `labelMedium` |

字体单位使用 `sp`，布局单位使用 `dp`，适配系统大字体。

### 4.3 形状、间距和布局

- 使用 4dp 基础间距网格。
- 紧凑屏幕页面左右边距 16dp，较宽屏幕使用 24dp。
- 卡片圆角采用 `medium`，弹窗和底部面板采用大圆角。
- 列表项间距以 8dp、12dp、16dp 为主。
- 所有触控命中区域至少为 `48dp × 48dp`。
- 支持边到边布局，但内容必须正确处理状态栏、导航栏和手势区域。

### 4.4 状态与反馈

- 空状态：插图、说明和明确操作。
- 加载状态：进度条或骨架屏，不使用无限旋转掩盖确定进度。
- 成功反馈：页面状态和 Snackbar。
- 输入错误：字段附近显示错误文本。
- 破坏性操作：确认对话框。
- 状态不能只靠颜色表达，必须配合文字或图标。

### 4.5 无障碍

- 普通文本对比度至少 4.5:1，大文本和必要图形至少 3:1。
- 所有功能性图标提供 TalkBack 语义。
- 装饰图片从无障碍树中排除。
- 进度朗读应节流，不能每处理一帧都打断用户。
- 支持字体放大、显示缩放、键盘和系统返回手势。
- 适配预测返回。
- 非必要动画尊重系统“移除动画”设置。
- 浅色、深色、动态配色分别执行对比度检查。

## 5. 技术架构

### 5.1 技术选型

| 分类 | 方案 |
| --- | --- |
| 语言 | Kotlin |
| 最低系统 | Android 10，`minSdk 29` |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose / Material 3 Adaptive |
| 状态 | ViewModel + StateFlow |
| 异步 | Kotlin Coroutines + Flow |
| 后台任务 | WorkManager 长任务 + 前台通知 |
| 本地数据库 | Room |
| 偏好设置 | DataStore Preferences |
| 图片选择 | Storage Access Framework |
| 图片导出 | MediaStore / SAF |
| 缩略图 | Coil |
| 依赖注入 | Hilt |
| 图像计算 | Kotlin 接口 + C++/NDK 实现 |
| 构建 | Gradle Version Catalog，锁定依赖版本 |

`targetSdk` 和 `compileSdk` 使用开发时的最新稳定版本，不在文档中固定过时数值。

### 5.2 模块划分

```text
:app
├── 应用入口、导航、DI、主题
│
├── :core:model
│   └── Job、Frame、BlendMode、OutputFormat
│
├── :core:data
│   └── Room、DataStore、SAF、MediaStore
│
├── :core:domain
│   └── 准备任务、启动、取消、导出、删除
│
├── :core:imaging
│   └── 解码、EXIF、叠加、编码、JNI
│
├── :feature:stacker
├── :feature:gallery
└── :feature:settings
```

UI 不直接访问数据库、文件系统或 JNI，统一通过 UseCase 调用。

## 6. 图像处理设计

### 6.1 算法一致性

所有算法均按 RGB 通道独立计算。

**最大值**

\[
result_c = \max(frame_{1,c}, frame_{2,c}, \ldots, frame_{n,c})
\]

**滤色**

\[
screen(a,b)=255-\left\lfloor\frac{(255-a)(255-b)+127}{255}\right\rfloor
\]

使用整数中间值，避免 8 位溢出。

**平均**

使用 32 位浮点累加，最终裁剪到 0～255。为匹配 NumPy `rint`，边界舍入应采用“就近舍入、中点取偶数”，不能随意替换为普通四舍五入。

### 6.2 解码规范

1. 根据 EXIF Orientation 转正。
2. 以第一帧转正后的尺寸作为基准。
3. 后续照片尺寸不一致时终止，并报告两个文件名及尺寸。
4. 透明像素合成到黑色背景。
5. 统一为 8 位 RGB。
6. 广色域输入在 V1 中转换到 sRGB，不静默改变输出分辨率。
7. 只保留当前帧和累加结果，禁止一次性解码全部照片。

### 6.3 编解码实现

Android 平台不能被视为完整支持全部桌面格式：

- JPEG、PNG、WebP、BMP：使用经过验证的平台或原生编解码器。
- TIFF：必须引入 `libtiff` 或等价组件，不能假设 `BitmapFactory` 可用。
- JPEG 4:4:4 输出：使用 `libjpeg-turbo`，避免平台压缩器擅自进行色度抽样。
- TIFF：使用 LZW 无损压缩。
- 输出保留 `ImageDescription = StarTrails output` 标记。

### 6.4 内存控制

使用 Native Direct Buffer 和逐帧处理：

- 最大值/滤色的基础像素缓冲约为每像素 6 字节。
- 平均模式使用 float32 累加时约为每像素 15 字节。
- 上述数值不包含解码器和缩略图开销。

任务开始前只读取全部图片的尺寸元数据，并检查：

- 可用内存。
- 输出所需存储空间。
- 图片尺寸一致性。
- URI 权限是否有效。

内存不足时明确提示用户减少数量或降低源图分辨率，不自动压缩原图。

## 7. 数据与任务模型

### 7.1 `StackJob`

| 字段 | 说明 |
| --- | --- |
| `id` | UUID |
| `status` | 当前任务状态 |
| `createdAt` / `updatedAt` | 时间戳 |
| `blendMode` | LIGHTEN / SCREEN / AVERAGE |
| `recursive` | 是否递归目录 |
| `outputFormat` | 输出格式 |
| `quality` | JPEG/WebP 质量 |
| `frameCount` | 总帧数 |
| `processedCount` | 已处理帧数 |
| `width` / `height` | 输出尺寸 |
| `outputUri` | 已导出的结果，可为空 |
| `stagingPath` | 应用私有临时结果 |
| `errorCode` | 稳定错误码 |
| `workId` | WorkManager 任务 ID |

### 7.2 `InputFrame`

保存：

- `jobId`
- `uri`
- `authority + documentId`
- `displayName`
- `sourceOrder`
- `frameOrder`
- `mimeType`
- `width`、`height`
- `sizeBytes`

Room 只保存元数据，不保存完整图片。缩略图放入可清理缓存。

### 7.3 状态机

```text
DRAFT
  ↓
VALIDATING
  ↓
QUEUED
  ↓
RUNNING
  ↓
SAVING
  ↓
SUCCEEDED
```

其他状态：

- `CANCELLING → CANCELLED`
- `FAILED`
- `NEEDS_PERMISSION`

已完成任务不可直接修改参数；调整参数时创建新任务。

### 7.4 后台与取消

- 使用唯一 WorkManager 任务防止重复启动。
- 通过 Room 和 Flow 驱动 UI，不依赖通知消息恢复页面状态。
- 每帧结束及每个大行块结束时检查取消标记。
- 取消后释放 Native 缓冲并清理临时文件。
- 进程恢复后如发现异常中断任务，标记为失败并提供“重新执行”。
- 通知仅用于任务状态，不用于传递核心数据。

## 8. 文件、权限与隐私

### 8.1 文件访问

- 使用 `OpenMultipleDocuments` 多选照片。
- 使用 `OpenDocumentTree` 选择目录。
- 对需要跨重启使用的 URI 调用持久授权。
- 不申请全文件管理权限。
- 不申请 `READ_MEDIA_IMAGES` 作为 MVP 的必要条件。
- 后台处理期间保持文件描述符有效，失效时返回明确错误。

### 8.2 原片保护

- 输入文件始终以只读方式打开。
- 输出先写入应用私有临时文件。
- 编码成功后才发布到 MediaStore 或用户指定位置。
- MediaStore 使用 `IS_PENDING`，失败时删除未完成项。
- SAF 导出失败时删除本次新建的不完整文件。
- 默认创建新文件，不覆盖已有照片。
- 不提供桌面版“强制覆盖原片”入口。

### 8.3 隐私

- 不声明 `INTERNET` 权限。
- 不上传照片及元数据。
- 日志不记录完整 URI、文件路径或图片内容。
- 分享通过系统 `ACTION_SEND` 和临时 URI 授权完成。
- 云备份不迁移已失效的本机 URI 和临时图片。

## 9. 错误设计

| 错误码 | 用户提示与操作 |
| --- | --- |
| `NOT_ENOUGH_FRAMES` | 至少选择两张照片 |
| `UNSUPPORTED_FORMAT` | 指出不支持的文件及格式 |
| `DECODE_FAILED` | 指出无法读取的照片，允许移除 |
| `DIMENSION_MISMATCH` | 显示两张照片的具体尺寸 |
| `URI_PERMISSION_LOST` | 引导重新选择照片或目录 |
| `INSUFFICIENT_MEMORY` | 建议减少照片或使用较小源图 |
| `INSUFFICIENT_STORAGE` | 提示清理空间后重试 |
| `OUTPUT_FAILED` | 保留任务参数，允许重新导出 |
| `CANCELLED` | 显示已取消，不显示为系统故障 |

用户界面只显示可理解的信息；技术堆栈仅进入受控调试日志。

## 10. 测试方案

### 10.1 算法测试

复用现有 Python 测试思路：

- 最大值逐通道结果。
- `screen(128,128)=192`。
- 平均值及中点舍入。
- 进度顺序。
- 尺寸不一致。
- 空输入及非法模式。
- EXIF 旋转。
- 透明像素黑底合成。

增加跨平台 Golden Test：

1. 使用无损 PNG 小图作为固定输入。
2. Python 生成参考 RGB 像素结果。
3. Android Native 引擎读取相同输入。
4. 对三种模式逐字节比较。
5. JPEG 等有损输出只比较解码容差，不比较文件字节。

### 10.2 Android 测试

- Room 查询、迁移和进程恢复。
- WorkManager 启动、唯一性、取消和异常中断。
- URI 授权撤销、文件被移动、存储不足。
- Compose 页面状态、导航和参数校验。
- 手机、平板、横屏和字体放大。
- TalkBack 与对比度人工验收。
- 低端设备内存压力测试。
- 长时间运行及重复取消测试。

## 11. V1 验收标准

### 功能

- 三种算法与 Python 参考结果一致。
- 支持原项目的全部输入输出格式。
- 自然排序、去重、递归、EXIF 和透明通道行为正确。
- 后台处理时 UI 不冻结，可离开页面并返回。
- 取消和失败不会破坏原片或已有结果。
- 结果可以预览、保存、导出和分享。
- 应用重启后作品历史与设置仍然可用。

### MD3 与体验

- 使用 Material 3 组件、颜色角色、字体和形状系统。
- 支持动态配色、静态回退及深浅主题。
- 紧凑、中等和展开窗口均有合理导航布局。
- 所有触控目标不小于 48dp。
- 不仅靠颜色表示错误、成功和选中状态。
- 大字体下无文字裁切或关键功能不可达。

### 性能与稳定性

- 不一次性加载全部原图。
- 内存不足在任务开始前被识别并给出提示。
- Native 内存在成功、失败、取消路径均能释放。
- 相同任务不能并发重复执行。
- 不依赖网络即可完成全部核心功能。

## 12. 推荐实施顺序

| 阶段 | 交付内容 |
| --- | --- |
| M1 | Compose 工程、MD3 主题、导航和页面空状态 |
| M2 | SAF 选图、目录扫描、自然排序、元数据检查 |
| M3 | Native 三种算法、EXIF、透明通道及 Golden Test |
| M4 | WorkManager、进度、通知、取消和异常恢复 |
| M5 | 编码导出、结果预览、分享、作品历史 |
| M6 | 全格式支持、无障碍、性能、内存及发布验收 |

建议先用 JPEG/PNG 小图完成算法闭环，再接入 TIFF、JPEG 4:4:4 和大图压力测试，避免同时调试 UI、后台任务与 Native 编解码问题。

## 13. UI 重构说明（Material 3 落地）

> 本轮只调整视觉与交互表现，选图、校验、叠加、保存、分享、历史等功能逻辑保持不变。

### 13.1 新增 `:core:designsystem` 模块

| 文件 | 作用 |
| --- | --- |
| `theme/Color.kt` | 午夜靛蓝 / 星光青 / 暖金光三组色调 + 冷调中性色 + 深色表面 token |
| `theme/Theme.kt` | 完整浅色 / 深色 ColorScheme（含 `tertiary`、`surfaceContainer*`、`inverse*`），并同步系统栏图标明暗 |
| `theme/Type.kt` | 15 级字阶，中文字距收紧、标题统一 Medium/SemiBold |
| `theme/Shape.kt` | 8 / 12 / 16 / 24 / 32dp 圆角阶梯 |
| `theme/Spacing.kt` | 4dp 网格间距、表单与内容最大宽度、按钮高度 |
| `theme/Insets.kt` | 页面内容安全区，避免与 TopAppBar、导航外壳重复留白 |
| `component/*` | `StarTrailsTopBar`、`SectionCard`、`SectionHeader`、`InfoRow`、`BottomActionBar`、`EmptyState`、`SwitchRow`、`LabeledSlider`、`SegmentedChoice` |

页面只允许使用 `MaterialTheme.colorScheme` 的语义角色与 `Spacing` / `Layout` token，禁止再出现散落的 `Color(0xFF…)` 和随手 dp 值。

### 13.2 导航与响应式

- 紧凑宽度：`NavigationBar`。
- 中等宽度，或展开宽度但高度为 Compact（横屏手机）：`NavigationRail` + 品牌标识。
- 其余展开宽度：`PermanentNavigationDrawer`（300dp，抽屉内含品牌头与导航项）。
- 底部系统栏仅由导航外壳统一预留；状态栏由各页面 `TopAppBar` 处理，消除了旧实现的顶部双倍留白。
- `NavHost` 增加 220–280ms 的淡入 + 轻微位移动画；叠加流程用 `AnimatedContent` 按阶段前后滑动，进度与数值使用 `animateFloatAsState` 平滑过渡。

### 13.3 页面要点

| 页面 | 主要变化 |
| --- | --- |
| 首页 | 容器色渐变主视觉 + 星轨装饰；主/次操作按钮 56dp；草稿与进行中任务卡片；三步流程说明 |
| 照片检查 | 缩略图卡片（12dp 圆角）、异常行整卡错误色提示、底部操作栏分级按钮 |
| 叠加参数 | 分区卡片 + 分段按钮 + 下拉菜单 + 质量滑块（切换格式时带展开动画） |
| 处理进度 | 220dp 环形进度 + 居中百分比，操作固定于底部操作栏 |
| 结果页 | 漂浮缩放工具条的圆角预览区、键值信息卡、层级化操作按钮、删除确认对话框 |
| 作品 | 自适应网格 + 照片底部渐变标题、右上角半透明菜单、统一空状态 |
| 设置 | 分段主题选择、动态配色开关行、参数卡片、带图标隐私说明 |

### 13.4 构建、部署与真机验证

前置：JDK 17、Android SDK（`local.properties` 中的 `sdk.dir`）、设备开启「开发者选项 → USB 调试」。

```bash
# 1. 确认设备已连接（serial 以实际为准）
adb devices -l

# 2. 构建 Debug APK
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 3. 安装（保留数据覆盖安装）
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 启动与查看崩溃
adb -s <serial> shell am start -n com.startrails/.MainActivity
adb -s <serial> logcat -s AndroidRuntime

# 5. 抓图验收
adb -s <serial> shell screencap -p /sdcard/ui.png
adb -s <serial> pull /sdcard/ui.png artifacts/verify/ui.png

# 6. 单元测试与静态检查
./gradlew testDebugUnitTest
./gradlew :app:lintDebug
```

Release 包需在 `app/build.gradle.kts` 中补齐 `signingConfigs` 后执行 `./gradlew :app:assembleRelease`。

### 13.5 真机验收清单

1. 冷启动无白闪（`windowBackground` 与主题 surface 一致），状态栏图标与主题明暗一致。
2. 首页：主视觉、两个 CTA、隐私说明在 360dp 宽与平板宽度下均无裁切。
3. 选择 2 张及以上照片 → 检查页展示文件名、序号、尺寸；尺寸不一致时整行变红并禁用「下一步」。
4. 参数页：切换格式时质量滑块展开/收起；仅 JPEG/WebP 显示。
5. 开始叠加后离开页面再返回，进度不中断；底部导航仍可用。
6. 结果页：双指缩放/双击缩放、悬浮工具条、保存/导出/分享/删除均可用；删除需二次确认。
7. 作品页：空状态引导、网格自适应列数、菜单打开/分享/导出/删除。
8. 设置：系统 / 浅色 / 深色切换时系统栏图标同步；动态配色开关在 Android 12+ 可切换。
9. 系统字体放大到 1.3–2.0 后无文字截断、按钮可点。
10. 横屏与平板：导航从底部栏切换为侧栏 / 抽屉，页面内容宽度受限并居中。

### 13.6 设备限制备忘（小米 / Redmi，MIUI）

- 默认拒绝 `adb shell input`（`SecurityException: INJECT_EVENTS`），需在「开发者选项」开启 **USB 调试（安全设置）**；开启后仍建议重启 adb 服务。
- `settings put` 与 `wm size/density` 需要 `WRITE_SECURE_SETTINGS`，非 root 的 shell 会被拒绝；响应式布局建议用平板模拟器验证。
- 息屏 60s 后 `screencap` 得到全黑图；可临时 `adb shell svc power stayon true` 保持常亮，验收结束后改回 `false`。

## 14. 参考资料

- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [MD3 Color roles](https://m3.material.io/styles/color/roles)
- [MD3 Accessible design](https://m3.material.io/foundations/accessible-design/overview)
