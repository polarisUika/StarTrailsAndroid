# StarTrails Android MD3 规范与界面美观性改善方案

## 1. 改造目标

当前项目已具备较好的 Material Design 3（MD3）基础架构（完整的语义色阶、动态取色、Edge-to-Edge、自适应导航栏与中文排版体系）。本方案旨在解决以下两个维度的不足：

1. **补齐 MD3 交互与无障碍规范细节**：完善滚动联动、触摸拦截、语义分组、动效一致性与触控热区标准。
2. **提升摄影类应用的视觉精致度**：围绕“星轨合成”主题强化视觉识别度，精简信息噪音，突出照片作品本身。

---

## 2. MD3 设计规范补齐方案

### 2.1 `StarTrailsTopBar` 接入滚动联动（ScrollBehavior）
* **涉及文件**：
  * `core/designsystem/src/main/java/com/startrails/core/designsystem/component/StarTrailsTopBar.kt`
  * `feature/stacker/`、`feature/gallery/`、`feature/settings/` 下各页面 `Scaffold`
* **现状问题**：顶栏配置了 `scrolledContainerColor`，但未接收 `TopAppBarScrollBehavior`，页面滚动时顶栏背景色无任何变化。
* **改造方案**：
  * 为 `StarTrailsTopBar` 增加可选参数 `scrollBehavior: TopAppBarScrollBehavior? = null` 并透传给 `TopAppBar`。
  * 在各列表/表单页创建 `val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()`，并在 `Scaffold` 上挂载 `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`。

### 2.2 补齐单选组无障碍语义
* **涉及文件**：`feature/settings/src/main/java/com/startrails/feature/settings/SettingsRoute.kt`
* **现状问题**：`ColorSchemePicker` 子项使用了 `Modifier.selectable(role = Role.RadioButton)`，但父容器缺少单选组声明。
* **改造方案**：在 `ColorSchemePicker` 外层 `Row` 添加 `Modifier.selectableGroup()`，确保 TalkBack 等读屏工具正确播报单选状态与总数。

### 2.3 加载遮罩拦截底层触摸事件
* **涉及文件**：`feature/stacker/src/main/java/com/startrails/feature/stacker/StackerRoute.kt`
* **现状问题**：`state.busy` 触发的全屏半透明遮罩未消费点击事件，用户可穿透点击底层按钮引发重复操作。
* **改造方案**：为遮罩 `Box` 添加无水波纹的点击拦截修饰符：
  ```kotlin
  .clickable(
      interactionSource = remember { MutableInteractionSource() },
      indication = null,
      onClick = {}
  )
  ```

### 2.4 统一条件区块过渡动效
* **涉及文件**：`feature/settings/src/main/java/com/startrails/feature/settings/SettingsRoute.kt`
* **现状问题**：关闭“动态配色”时，“配色方案”选择器直接通过 `if` 硬切显示，与 `HomeScreen`、`StackSettingsScreen` 的动效体验割裂。
* **改造方案**：使用 `AnimatedVisibility(visible = !prefs.useDynamicColor)` 包裹配色方案选择区域，保持与全局一致的展开/收起过渡。

### 2.5 规范触控热区与操作栏层级
* **涉及文件**：
  * `feature/gallery/src/main/java/com/startrails/feature/gallery/GalleryRoute.kt`
  * `feature/stacker/src/main/java/com/startrails/feature/stacker/ReviewScreen.kt`
* **现状问题**：
  * `GalleryCard` 右上角菜单按钮视觉与点击容器为 `40.dp`，小于 MD3 推荐的 `48.dp` 最小触控热区。
  * `ReviewScreen` 底部栏并排 3 个按钮（清空、继续添加、下一步），在 `360.dp` 窄屏下布局拥挤。
* **改造方案**：
  * `GalleryCard` 菜单按钮保持 `48.dp` 触控热区（或使用默认 `IconButton` 尺寸配合内部视觉圆形底板）。
  * `ReviewScreen` 将“清空”操作移至 `TopAppBar` 的 `actions` 区域（使用 `TextButton` 或带文字提示的图标按钮）；底部栏仅保留 `FilledTonalButton("继续添加")` 与 `Button("下一步")`，拉开主次层级。

---

## 3. 用户界面美观性升级方案

### 3.1 首页 `HeroPanel`：强化同心星轨主题视觉
* **涉及文件**：`feature/stacker/src/main/java/com/startrails/feature/stacker/HomeScreen.kt`
* **现状问题**：当前背景为 `primaryContainer -> secondaryContainer` 线性渐变加两条低对比度细弧线，浅色模式下视觉辨识度偏弱。
* **视觉改造**：
  * 呼应应用图标 `ic_launcher_foreground.xml` 的同心弧线语言，在卡片右上角绘制 3–4 条同心星轨圆弧。
  * 使用 `Brush.sweepGradient` 为每条圆弧赋予从透明到高亮的拖尾渐变效果，并在弧线端点点缀微小的星点。
  * 保持文字区与装饰区的安全留白，避免图案干扰主副标题阅读对比度。

### 3.2 结果页 `ResultScreen`：作品优先，精简表单感
* **涉及文件**：`feature/stacker/src/main/java/com/startrails/feature/stacker/ResultScreen.kt`
* **现状问题**：预览区高度写死 `340.dp`；下方 6 行竖排 `InfoRow` + 底部 5 个按钮导致页面冗长，注意力被表单和按钮分散。
* **视觉改造**：
  1. **增强预览主体**：将预览容器改为自适应高度（支持横竖屏及宽屏比例），支持点击预览图进入**全屏沉浸式查看模式**（隐藏系统栏与文字信息，仅保留缩放与退出按钮）。
  2. **参数信息网格化**：将 6 行竖排 `InfoRow` 重构为紧凑的 **2×3 参数网格**或单行横向胶囊摘要（如 `4000×3000` · `48 帧` · `最大值` · `JPEG` · `12.4 MB` · `3.2 秒`），降低垂直高度与视觉噪音。
  3. **精简操作区**：
     * 底部仅保留核心操作（如 `Button("分享")` + `FilledTonalButton("导出文件")`；若未自动保存相册则提供保存入口，已保存则以轻量状态标签展示）。
     * 将低频且具破坏性的“删除”操作移至 `TopAppBar` 右上角菜单或操作栏中，减少误触。

### 3.3 设置页 `ColorSchemePicker`：升级为 MD3 三色拼接调色盘
* **涉及文件**：`feature/settings/src/main/java/com/startrails/feature/settings/SettingsRoute.kt`
* **现状问题**：目前每个主题仅展示一个 `24.dp` 单色圆点，无法直观体现配色的主辅色与背景色搭配效果。
* **视觉改造**：
  * 参照 Android 系统“壁纸与样式”的调色盘控件，用 `Canvas` 绘制 `44.dp` 圆形三色调色盘：
    * 上半圆（`180°`）：`primary`
    * 左下四分之一圆（`90°`）：`secondaryContainer`
    * 右下四分之一圆（`90°`）：`tertiaryContainer`
  * 选中态在外圈绘制 `2.dp` 的 `primary` 描边环，并在圆心叠加一个小尺寸白色/反色 `Icons.Rounded.Check` 徽标。

### 3.4 作品页 `GalleryCard`：修复异常态观感并增加摄影元数据角标
* **涉及文件**：`feature/gallery/src/main/java/com/startrails/feature/gallery/GalleryRoute.kt`
* **现状问题**：当作品文件丢失（`!item.available`）时，底部的黑色渐变遮罩仍覆盖在占位背景上，导致卡片下半部显得灰暗脏乱。
* **视觉改造**：
  * 仅在 `item.available && model != null` 时绘制底部渐变文字遮罩；不可用状态采用干净的 `surfaceContainerHigh` 卡片底色配居中提示图标与文字。
  * 在有效作品卡片左上角增加一个半透明胶囊角标（`Color.Black.copy(alpha = 0.45f)`），展示格式或帧数（如 `TIFF · 60 帧`），增强作品集的专业质感。

### 3.5 处理页 `ProcessingScreen`：增强长耗时过程的可控感
* **涉及文件**：`feature/stacker/src/main/java/com/startrails/feature/stacker/ProcessingScreen.kt`
* **现状问题**：仅展示大号环形进度条与居中文字，缺少阶段反馈与实时进度细节。
* **视觉改造**：
  * 在环形进度下方增加三段式阶段指示胶囊：**`解码校验` → `星轨合成` → `编码保存`**，高亮当前所处阶段。
  * 接通当前处理帧文件名（如 `IMG_2026_0042.JPG`）与已耗时显示，降低用户等待焦虑。

### 3.6 公共底部操作栏 `BottomActionBar`：强化层级边界
* **涉及文件**：`core/designsystem/src/main/java/com/startrails/core/designsystem/component/CommonSurfaces.kt`
* **改造方案**：在 `BottomActionBar` 顶部增加 `HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))`，在滚动内容经过底部栏上方时形成清晰、克制的视觉分割线。

---

## 4. 推荐实施优先级

| 阶段 | 改造项 | 投入成本 | 视觉/体验收益 |
| :--- | :--- | :---: | :---: |
| **第一阶段（快速见效）** | `StarTrailsTopBar` 滚动联动、`BottomActionBar` 分割线、`ColorSchemePicker` 三色调色盘、加载遮罩防穿透、无障碍 `selectableGroup` | 低 | 高 |
| **第二阶段（核心页面重构）** | `ResultScreen` 预览放大与参数网格化、`ReviewScreen` 底部栏减负、`GalleryCard` 异常态修复与角标 | 中 | 极高 |
| **第三阶段（品牌与细节打磨）** | `HeroPanel` 同心星轨渐变动效绘制、`ProcessingScreen` 三段式阶段指示、全屏沉浸式看图模式 | 中 | 高 |