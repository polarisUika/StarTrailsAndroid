[readme.md](https://github.com/user-attachments/files/35096686/readme.md)
# StarTrails Android

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android_10%2B_(API_29%2B)-3DDC84.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack_Compose-7F52FF.svg)](https://kotlinlang.org)
[![Material 3](https://img.shields.io/badge/UI-Material_Design_3-6750A4.svg)](https://m3.material.io)

**StarTrails Android** 是一款专为星空摄影打造的**纯本地、离线运行**的 Android 星轨叠加工具。项目采用 Modern Android Development（MAD）技术栈与多模块 Clean Architecture 架构构建，支持后台队列合成、多格式无损/有损导出以及 Material Design 3 自适应界面。

---

## 核心特性

* **纯本地处理，隐私优先**：所有照片读取、像素合成与编码均在设备端离线完成，不依赖网络，不上传任何用户影像。
* **多模式星轨合成**：内置星轨叠加引擎，支持多种混合模式（如最大值/变亮、平均值等），自动处理 EXIF 方向与分辨率一致性校验。
* **丰富的输出格式**：支持导出为 **JPEG**、**PNG**、**WebP**，并内置纯 Kotlin 实现的 **TIFF** 与 **BMP** 无损编码器，支持自定义输出画质。
* **后台可靠执行**：基于 `WorkManager` 执行耗时叠加任务，支持进度追踪、任务取消与异常状态恢复。
* **作品集与导出管理**：内置本地作品库，支持大图缩放预览、一键保存至系统相册（MediaStore）、通过 SAF 导出至指定目录以及系统分享。
* **Material Design 3 与自适应布局**：
  * 支持 Android 12+ 动态取色（Material You）、多套内置配色方案及深浅色模式切换。
  * 基于 `WindowSizeClass` 自动适配手机、折叠屏与平板（自动切换底部导航栏、侧边导航轨与常驻侧边抽屉）。

---

## 使用流程

1. **选择照片**：从系统相册或文件夹批量导入多张连续拍摄的星空照片。
2. **检查与校验**：自动读取照片尺寸与方向元数据，支持剔除异常帧或继续增删。
3. **参数配置**：选择混合模式、输出格式与压缩质量。
4. **后台合成**：在进度页实时查看处理状态，可随时安全取消。
5. **预览与分享**：查看合成结果、缩放检查星轨细节，并保存、导出或分享作品。

---

## 技术栈

| 类别 | 技术选型 |
| :--- | :--- |
| **语言与并发** | Kotlin、Kotlin Coroutines、Flow |
| **UI 框架** | Jetpack Compose、Material Design 3、Navigation Compose |
| **依赖注入** | Hilt (`hilt-android`, `hilt-work`) |
| **数据持久化** | Room（任务与帧记录）、Preferences DataStore（用户偏好） |
| **后台调度** | WorkManager |
| **图像处理** | Android Bitmap / ExifInterface、Coil、自定义 TIFF/BMP Writer |

---

## 模块架构

项目按职责划分为 9 个 Gradle 模块，保持单向依赖与清晰的边界：

```text
StarTrailsAndroid/
├── app/                     # 应用入口、全局导航 (AppNavigation) 与主题装配
├── core/
│   ├── model/               # 领域模型、枚举（BlendMode, OutputFormat, JobStatus 等）
│   ├── designsystem/        # MD3 主题、配色方案、排版、圆角与公共基础组件
│   ├── data/                # Room 数据库、DataStore 偏好设置、相册与文件仓储
│   ├── domain/              # 核心用例 (UseCases) 与后台合成 Worker (StackWorker)
│   └── imaging/             # 图像元数据读取、解码、星轨累加引擎与多格式编码器
└── feature/
    ├── stacker/             # 主流程界面（首页、照片检查、参数设置、处理进度、结果页）
    ├── gallery/             # 历史作品集管理界面
    └── settings/            # 主题配色与默认参数设置界面
```

---

## 开发与构建指南

### 环境要求

* **Android Studio**：Ladybug (2024.2) 或更高版本
* **JDK**：17
* **Android SDK**：`minSdk = 29` (Android 10)，`compileSdk / targetSdk = 35` (Android 15)

### 本地构建

1. 克隆仓库到本地：
   ```bash
   git clone https://github.com/polarisUika/StarTrailsAndroid.git
   cd StarTrailsAndroid
   ```

2. 使用 Gradle 编译 Debug 安装包：
   ```bash
   ./gradlew assembleDebug
   ```

3. 运行单元测试：
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. 在 Android Studio 中直接打开项目根目录，同步 Gradle 后即可运行至真机或模拟器。

---

## 相关文档

* 开发规范与架构说明：[`docs/DEVELOPMENT_GUIDE.md`](docs/DEVELOPMENT_GUIDE.md)

---

## 开源协议

本项目基于 **Apache License 2.0** 协议开源。

```text
Copyright 2026 polarisUika

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

完整协议条款请参阅仓库根目录下的 [LICENSE](LICENSE) 文件。
