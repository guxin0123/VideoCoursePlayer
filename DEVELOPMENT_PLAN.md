# VideoCoursePlayer 开发计划

> 关联文档：[README](README.md)  
> 版本：1.0（已实施）  
> 变更：视频标签支持多标签；MVP 已实现并完成 Release 构建。  
> 当前阶段：MVP 已交付，Phase 6 的旧版本兼容性与自动化 UI 测试待补。

## 0. 实施摘要（2026-11-09）

| 项目 | 结果 |
| --- | --- |
| Phase 0 工程基线 | 已完成。阿里云 Maven / 腾讯 Gradle 镜像接入，Compose 与 Kotlin 2.2.10 工具链跑通 |
| Phase 1 数据层 | 已完成。MediaStore 相册/视频查询、DataStore 偏好、多标签 JSON、自然排序 |
| Phase 2 权限与相册 | 已完成。完整授权、Android 14+ 受限授权提示、拒绝引导、相册选择与持久化 |
| Phase 3 主界面与列表 | 已完成。横竖屏双区域布局、缩略图内存缓存、空状态与错误状态 |
| Phase 4 播放 | 已完成。ExoPlayer、顺序连播、单曲循环、旋转续播、退后台暂停、屏幕常亮 |
| Phase 5 多标签与筛选 | 已完成。多选标签面板、标签摘要、单选标签筛选、筛选失效回退 |
| Phase 6 测试与交付 | 部分完成。16 个单元测试、Lint、Debug/Release 构建和 API 36 手工测试通过；Compose 仪器测试与 API 30/33 未覆盖 |
| 交付物 | [`dist/VideoCoursePlayer-1.0.apk`](dist/VideoCoursePlayer-1.0.apk)，使用开发证书签名的 Release 构建；主界面支持双击返回退出 |

### 实施中的偏差

1. **单一 ViewModel**：实际使用 `AppViewModel` 统一管理权限、相册、视频、标签、筛选与播放状态，未拆成 `AlbumPickerViewModel` 和 `MainViewModel`。页面状态较少，单一状态源更容易保证一致性。
2. **列表标签展示**：列表行使用文本标签摘要（前 2 个 + `+N`），标签 chip 主要用于编辑面板和筛选条，节省窄屏横向空间。
3. **R8 规则**：AGP 9 的包级优化会触发 `kotlin.sequences` 访问错误；已在 `app/src/main/keepRules/rules.keep` 中保留该包，Release 已实测可启动和播放。
4. **测试范围**：单元测试、Lint 和 API 36 手工测试完成；没有在 API 30/33 上实际运行，也未添加 Compose 仪器测试。
5. **发布签名**：`dist/` APK 使用本机调试证书签名，仅用于本地安装验证，不是应用商店发布包。

## 1. 项目目标

构建一个轻量、离线、专注本地课程视频的 Android 播放器，形成完整闭环：

```text
首次启动 → 授予视频访问权限 → 选择一个本地相册 → 浏览视频列表
        → 给视频打标签 → 按标签筛选 → 顺序连播或单曲循环
```

“完成”不仅是功能可点，还包括：相册和标签在重启后保留、横竖屏布局合理、播放中旋转不中断、权限和媒体异常可恢复、Release 构建可产出。

### 成功标准

- 冷启动后，已选相册仍有效时直接进入主界面，不出现长时间白屏。
- 竖屏与横屏都能完整看到播放区、筛选条和列表，不出现横向溢出或按钮被遮挡。
- 从列表点击视频后可正常播放，顺序模式按列表顺序前进，单曲循环不前进。
- 多个标签保存后持久化，筛选结果和播放队列一致。
- 核心流程在 API 30、33 和 34+ 上可用，覆盖完整权限、受限权限和拒绝权限。

## 2. 产品范围与假设

### 2.1 MVP 功能清单

| 编号 | 功能 | 验收标准 |
| --- | --- | --- |
| F-01 | 首次启动引导 | 无权限时显示用途说明；授权后进入相册选择；未选择相册不能进入主界面 |
| F-02 | 相册选择 | 列出本地视频相册及视频数量；点击后保存选择并进入主界面 |
| F-03 | 记住相册 | 重启应用直接进入主界面；已选相册不存在或权限撤销时回到相册选择并提示 |
| F-04 | 响应式布局 | 竖屏上下布局；横屏左右双栏；列表独立滚动；旋转不崩溃 |
| F-05 | 视频列表 | 缩略图、文件名、时长、标签摘要可见；按文件名自然升序；点击播放 |
| F-06 | 视频播放 | 播放/暂停、进度拖动、时间显示；播放失败有可读提示 |
| F-07 | 播放模式 | “单曲循环”和“顺序播放”可切换；顺序模式最后一节结束后停止 |
| F-08 | 标签设置 | 每个视频可设置多个标签；支持多选已有标签、新建标签、移除标签；列表行展示标签摘要 |
| F-09 | 标签筛选 | 全部 / 未标记 / 单个标签；带多个标签的视频出现在其任一所属标签下；筛选无结果有引导；筛选决定新播放队列 |
| F-10 | 持久化 | 相册、标签、播放模式重启后恢复；标签数据损坏时降级为空数据且不崩溃 |
| F-11 | 异常状态 | 无相册、空相册、权限拒绝、受限权限、视频不存在、播放错误均有明确状态 |
| F-12 | 基础质量 | 单元测试覆盖核心逻辑；Debug/Release 均可构建；无已知崩溃 |

### 2.2 明确不在 MVP 内

- 标签颜色、标签重命名/删除/合并管理页、多标签组合筛选（AND/OR）。
- 搜索、排序设置、播放进度续播、收藏。
- 后台播放、画中画、锁屏控制、通知栏控制。
- 字幕、倍速、音轨/字幕选择、手势调亮度/音量。
- 视频转码、下载、编辑、文件管理。
- SAF 目录选择和云盘/云相册接入。

### 2.3 产品假设

以下决策用于控制 MVP 范围；如与预期不符，应在 Phase 1 数据模型定稿前调整。

| 编号 | 假设 | 理由 |
| --- | --- | --- |
| A-01 | 相册来源为本地 `MediaStore.Video` | 最贴近“系统相册”语义；无需文件管理器 |
| A-02 | 每个视频可有多个标签，MVP 不设数量硬上限 | 课程可按学科、章节、状态等多个维度归类；列表只展示摘要 |
| A-03 | 默认按文件名自然升序 | 课程通常以“第1课、第2课、第10课”命名，字典序会错位 |
| A-04 | 顺序播放到列表末尾即停止 | “按列表顺序播放”默认不循环整个列表 |
| A-05 | 播放队列是点击时筛选结果的快照 | 播放中改筛选不打断当前连播，行为可预测 |
| A-06 | 退后台暂停、不后台播放 | 课程播放器无后台服务需求，降低权限与生命周期复杂度 |
| A-07 | 播放模式默认顺序播放 | 更符合连续学习场景 |
| A-08 | 标签为自由文本，单个标签最长 20 字符 | 不预设业务标签，保持灵活；限制长度避免布局破坏 |
| A-09 | 标签筛选为单选（选择一个标签） | MVP 足够；多标签组合会引入 AND/OR 语义，留待后续 |

## 3. 技术基线

### 3.1 现有工程

| 项目 | 当前值 |
| --- | --- |
| 包名 / applicationId | `com.guxins.videocourseplayer` |
| Gradle | 9.6.0（Wrapper） |
| Android Gradle Plugin | 9.5.1 |
| compileSdk / targetSdk | 37 / 37 |
| minSdk | 30（Android 11） |
| Java 编译目标 | 11 |
| UI | 尚未接入 Compose，只有 Material Components XML 主题 |
| 启动入口 | 尚未创建 `MainActivity` |
| SDK | 本机 `F:\DevelopCache\AndroidSdk`，已安装 Platform 37 |
| 设备 | 当前无 ADB 设备连接 |
| 依赖缓存 | 尚无 AndroidX/Compose 依赖缓存，首次构建必须联网 |

### 3.2 计划引入的依赖

版本统一放入 [gradle/libs.versions.toml](gradle/libs.versions.toml)，Compose 使用 BOM 管理。

| 依赖 | 用途 |
| --- | --- |
| `androidx.compose:compose-bom` | 统一 Compose 版本 |
| `androidx.compose.ui:ui`、`ui-tooling-preview` | Compose 基础与预览 |
| `androidx.compose.material3:material3` | Material 3 组件 |
| `androidx.compose.material:material-icons-core` | 常用图标，避免引入体积较大的 extended 包 |
| `androidx.activity:activity-compose` | Compose Activity |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | ViewModel 与 Compose 集成 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 生命周期感知状态收集 |
| `androidx.media3:media3-exoplayer` | 视频播放内核 |
| `androidx.media3:media3-ui` | `PlayerView` 控制器 |
| `androidx.datastore:datastore-preferences` | 相册、标签、模式持久化 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 异步与 Flow（显式依赖） |
| `junit:junit`、`kotlinx-coroutines-test` | 单元测试 |
| `androidx.compose.ui:ui-test-junit4`、`ui-test-manifest` | Compose UI 测试 |

不引入：Hilt、Room、Navigation Compose、Coil、Retrofit、Firebase、任何广告/统计 SDK。

### 3.3 Phase 0 必须验证的工具链问题

当前工程是 AGP 9.5.1 新式工程，**没有显式 Kotlin 插件**。AGP 9.x 提供内置 Kotlin 支持，但 Compose Compiler 的启用方式必须按实际 AGP 版本文档确认。

Phase 0 要求：

1. 先只创建一个最小 Compose 界面并构建成功，再添加业务代码。
2. 不盲目同时加入 `org.jetbrains.kotlin.android` 和内置 Kotlin，避免插件冲突。
3. 确认 Compose Compiler 与工程实际 Kotlin 版本匹配。
4. 确认 `compileSdk = 37`、现有 `optimization { ... }` Release 配置与 R8 可正常工作。
5. 若 Android Studio 与命令行 Gradle 使用不同 JDK，统一到 JDK 17–21 的稳定组合；本机 `java` 为 23，出现工具链错误时切到 IDE 自带 JBR。

## 4. 关键设计决策

### D-01：使用 MediaStore 而不是 SAF

**选择**：查询 `MediaStore.Video`，按 `VOLUME_NAME + BUCKET_ID` 聚合相册。

**原因**：

- 首次选择体验是“相册列表”，不是“文件目录树”。
- 可直接获取桶名称、视频数量、时长、修改时间等元数据。
- 播放 URI 可直接交给 ExoPlayer，不需要长期保存 SAF 授权。

**代价/边界**：

- 需要 `READ_MEDIA_VIDEO`（或旧版权限）；Android 14+ 存在受限访问。
- 仅覆盖 MediaStore 已索引的本地视频；云端相册不支持。
- 相册名可能重复，因此内部 key 必须包含 `VOLUME_NAME`，UI 可在重名时显示路径提示。

### D-02：单 Activity + 状态驱动页面

页面只有权限引导、相册选择、主界面三类，不引入 Navigation Compose：

```kotlin
sealed interface AppScreen {
    data object Loading : AppScreen
    data object Permission : AppScreen
    data object AlbumPicker : AppScreen
    data class Main(val album: Album) : AppScreen
}
```

实际实现可由 `AppUiState` 推导，避免导航栈和媒体权限状态双份维护。

### D-03：播放器由 ViewModel 持有

- `AppViewModel` 通过 Factory 接收 `PlaybackController`。
- `PlaybackController` 持有唯一 ExoPlayer 实例，`onCleared()` 时释放。
- 旋转屏幕时 ViewModel 存活，ExoPlayer 和播放进度保留；`PlayerView` 在 Compose 中重新绑定。
- 不在 Composable 中直接创建 ExoPlayer，避免重组导致实例泄漏。

### D-04：播放队列快照

点击视频时：

1. 从当前筛选后的列表生成 `List<VideoItem>`；
2. 构造 Media3 `MediaItem` 列表；
3. `setMediaItems(items, startIndex, 0)`；
4. 设置模式：顺序 `REPEAT_MODE_OFF`，单曲 `REPEAT_MODE_ONE`；
5. `prepare()` + `play()`。

播放中列表筛选变化不修改队列；点击新视频才重建队列。这样自动连播不会因为界面筛选而突然改变。

### D-05：DataStore + JSON，而不是 Room

MVP 只需保存一个相册 key、一个“视频 key → 标签列表”映射和一个播放模式。引入 Room 会增加 KSP、Schema、迁移与构建复杂度，收益不足。

- 相册：`selected_album_key: String?`
- 标签：`tags_json: String`，格式 `{"external_primary:123":["重点","待复习"]}`，带 `schema_version`
- 模式：`play_mode: "SEQUENTIAL" | "SINGLE_LOOP"`
- JSON 解析失败：记录日志、回退空映射，不崩溃。
- 标签写入通过 `DataStore.edit` 原子完成，一次提交某个视频的完整标签集合。

若后续要标签颜色、全局标签管理或大量记录，再迁移 Room，并在 Phase 1 预留 codec 层。

### D-06：视频 key 与相册 key

```kotlin
albumKey = "$volumeName:$bucketId"
videoKey = "$volumeName:$_id"
```

`MediaStore._ID` 在正常媒体更新中相对稳定，但媒体库重建/文件移动后可能变化；MVP 接受标签丢失风险，并在 README 中记录。不能只用 `BUCKET_ID`，因为不同存储卷可能重复；不能只用文件名，因为可能重名。

### D-07：缩略图不引入 Coil

`minSdk = 30`，可直接使用 `ContentResolver.loadThumbnail(uri, Size(320, 180), null)`：

- 在 `Dispatchers.IO` 加载；
- 内存 `LruCache`，key 包含 `videoKey + lastModified`；
- 并发信号量限制（建议 4），避免大量列表项同时解码；
- 失败时显示视频占位图，不阻塞列表。

## 5. 架构与数据模型

### 5.1 分层

```text
UI (Compose)
  ├─ AlbumPickerScreen / MainScreen / 通用组件
  └─ UiState + 事件回调
        ↓
ViewModel
  ├─ AlbumPickerViewModel
  └─ MainViewModel（筛选、播放状态、标签操作）
        ↓
Repository / Controller
  ├─ MediaRepository  → ContentResolver / MediaStore
  ├─ TagRepository    → Preferences DataStore
  └─ PlaybackController → ExoPlayer
```

原则：

- Compose 不直接查询 MediaStore，也不直接读写 DataStore。
- ViewModel 不持有 Activity Context；使用 Application Context。
- Repository 接口化，便于用 Fake 做测试。
- UI 事件一次性消息通过 `Channel`/`SharedFlow` 发送（Snackbar、错误），持久状态放 `StateFlow`。

### 5.2 核心模型

```kotlin
data class Album(
    val key: String,            // volumeName:bucketId
    val volumeName: String,
    val bucketId: String,
    val name: String,
    val relativePathHint: String?,
    val videoCount: Int,
    val latestModified: Long,
)

data class VideoItem(
    val key: String,            // volumeName:_id
    val mediaId: Long,
    val uri: Uri,
    val albumKey: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val mimeType: String?,
    val lastModified: Long,
    val tags: List<String>,
)

enum class PlayMode { SINGLE_LOOP, SEQUENTIAL }

sealed interface VideoFilter {
    data object All : VideoFilter
    data object Untagged : VideoFilter
    data class ByTag(val tag: String) : VideoFilter
}
```

### 5.3 主要状态

```kotlin
data class MainUiState(
    val album: Album? = null,
    val allVideos: List<VideoItem> = emptyList(),
    val visibleVideos: List<VideoItem> = emptyList(),
    val tags: List<String> = emptyList(),
    val filter: VideoFilter = VideoFilter.All,
    val currentVideoKey: String? = null,
    val playMode: PlayMode = PlayMode.SEQUENTIAL,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val permissionState: MediaPermissionState = MediaPermissionState.Unknown,
    val message: String? = null,
)
```

派生数据（可见列表、标签统计、当前索引）在 ViewModel 中计算，UI 只渲染状态。

### 5.4 计划文件

```text
app/src/main/java/com/guxins/videocourseplayer/
├─ VideoCoursePlayerApp.kt
├─ MainActivity.kt
├─ AppContainer.kt
├─ data/
│  ├─ model/Models.kt
│  ├─ media/MediaRepository.kt
│  ├─ media/MediaStoreRepository.kt
│  ├─ media/ThumbnailLoader.kt
│  ├─ prefs/AppPreferences.kt
│  ├─ prefs/TagJsonCodec.kt
│  └─ prefs/TagRepository.kt
├─ playback/
│  ├─ PlaybackController.kt
│  └─ PlaybackQueue.kt
├─ ui/
│  ├─ AppRoot.kt
│  ├─ theme/Theme.kt
│  ├─ theme/Color.kt
│  ├─ theme/Type.kt
│  ├─ album/AlbumPickerScreen.kt
│  ├─ album/AlbumPickerViewModel.kt
│  ├─ main/MainScreen.kt
│  ├─ main/MainViewModel.kt
│  ├─ main/VideoListPane.kt
│  ├─ main/PlayerPane.kt
│  ├─ main/TagFilterBar.kt
│  └─ components/TagEditSheet.kt
└─ util/
   ├─ NaturalOrder.kt
   └─ Format.kt
```

测试目录对应放置 `*Test.kt`；UI 测试放 `app/src/androidTest`。

## 6. 里程碑与任务拆分

> 以下复选框保留原始计划拆分，实际完成状态以第 0 节“实施摘要”为准。  
> 工作量为单人开发的相对估算，不含环境下载等待、设计评审和需求返工。

| 阶段 | 目标 | 估算 | 依赖 |
| --- | --- | --- | --- |
| Phase 0 | 可构建、可启动的 Compose 基线 | 0.5–1 天 | 无 |
| Phase 1 | 数据层与持久化 | 0.5–1 天 | Phase 0 |
| Phase 2 | 权限流程与相册选择 | 1 天 | Phase 1 |
| Phase 3 | 响应式主界面与视频列表 | 1–1.5 天 | Phase 2 |
| Phase 4 | 播放与模式 | 1–1.5 天 | Phase 3 |
| Phase 5 | 多标签设置与标签筛选 | 1–1.5 天 | Phase 4 |
| Phase 6 | 测试、打磨、Release | 1–1.5 天 | Phase 5 |
| **合计** |  | **6–9 天** |  |

### Phase 0：工程基线（已完成）

**任务**

- [ ] P0-1 配置依赖镜像；保留官方源兜底；按需使用代理。
- [ ] P0-2 在版本目录加入 Compose、Lifecycle、Media3、DataStore 等依赖。
- [ ] P0-3 确认 AGP 9.5.1 内置 Kotlin 与 Compose Compiler 配置，创建最小 Compose 界面。
- [ ] P0-4 创建 `VideoCoursePlayerApp`、`MainActivity`，补全 Manifest 的 launcher 入口。
- [ ] P0-5 将 XML 主题改为无 ActionBar 的 Material 3 风格，接入 Compose 主题、亮/暗色和 edge-to-edge。
- [ ] P0-6 构建 Debug APK，在模拟器/真机启动，旋转一次。

**完成标准**

- `.\gradlew.bat :app:assembleDebug` 成功。
- 安装后能看到 Compose 空白/占位页面，无崩溃。
- 不依赖直接访问 Google/GitHub 完成构建。
- 若构建失败，先解决工具链，不在未跑通的基线上继续业务开发。

### Phase 1：数据层（已完成）

**任务**

- [ ] P1-1 定义 `Album`、`VideoItem`、`PlayMode`、`VideoFilter`。
- [ ] P1-2 实现 `MediaRepository` 接口及 `MediaStoreRepository`。
- [ ] P1-3 相册查询：只取视频；过滤 `IS_PENDING=0`、`IS_TRASHED=0`；按 `VOLUME_NAME + BUCKET_ID` 聚合；统计数量和最近修改时间。
- [ ] P1-4 视频查询：按相册 key 过滤；投影名称、时长、大小、MIME、修改时间；按文件名自然排序。
- [ ] P1-5 实现 `NaturalOrder` 比较器（数字段按数值比较，大小写不敏感，稳定兜底）。
- [ ] P1-6 实现 `AppPreferences` 与 `TagJsonCodec`；以数组存储每个视频的标签集合，处理空值、损坏 JSON、未知 schema。
- [ ] P1-7 实现 `TagRepository`：读取映射、添加/移除/替换某个视频的标签集合、聚合标签并统计使用次数。
- [ ] P1-8 实现媒体权限检测工具，区分完整、受限、拒绝、未请求。
- [ ] P1-9 为测试添加 Fake Repository。

**完成标准**

- 能在日志/调试界面正确列出相册和视频，不依赖 UI。
- 重启后已保存的相册 key 和标签仍在。
- 单元测试通过：自然排序、JSON 编解码、标签聚合。
- 空相册、权限为空、游标为空均不崩溃。

### Phase 2：权限引导与相册选择（已完成）

**任务**

- [ ] P2-1 Manifest 声明 `READ_EXTERNAL_STORAGE`（maxSdk 32）、`READ_MEDIA_VIDEO`、`READ_MEDIA_VISUAL_USER_SELECTED`。
- [ ] P2-2 首次启动权限说明页；使用 Activity Result API 请求权限。
- [ ] P2-3 完整权限：加载相册列表；受限权限：加载可见相册并显示提示条；拒绝：说明 + 打开系统设置。
- [ ] P2-4 相册选择 UI：名称、视频数、重名路径提示；空状态。
- [ ] P2-5 选择后写入 DataStore，进入主界面。
- [ ] P2-6 冷启动跳过逻辑：已选相册仍可访问则直接进入主界面，否则回选择页并提示。
- [ ] P2-7 主界面“切换相册”入口和返回行为。

**完成标准**

- 全新安装：权限 → 相册 → 主界面流程可走通。
- 已选相册：重启直接进入主界面。
- 权限撤销或相册删除：回相册选择页，不白屏、不崩溃。
- 竖屏/横屏切换不重复申请权限、不丢失选择。

### Phase 3：主界面与视频列表（已完成）

**任务**

- [ ] P3-1 实现 `AppRoot` 状态机：Loading / Permission / AlbumPicker / Main。
- [ ] P3-2 主界面骨架：TopAppBar、相册名、切换相册入口、刷新入口（可选）。
- [ ] P3-3 响应式布局：竖屏 Column；横屏 Row；列表宽度限制并独立滚动。
- [ ] P3-4 视频列表项：缩略图、文件名（最多两行）、时长、标签摘要（最多 2 个 chip + `+N`）、标签按钮、选中态。
- [ ] P3-5 `ThumbnailLoader`：IO 加载、LruCache、并发限制、失败占位。
- [ ] P3-6 空相册、加载中、读取失败状态。
- [ ] P3-7 点击列表项先回调选中；播放能力在 Phase 4 接入。
- [ ] P3-8 列表使用稳定 key，滚动位置在旋转后尽量保留。

**完成标准**

- 相册中的视频按自然顺序显示，滚动流畅。
- 多标签视频在列表中展示最多 2 个标签 chip 和 `+N`，不挤压标题。
- 竖屏/横屏布局不溢出，播放区域比例正确。
- 视频很多时（至少 200 条）滚动无明显卡顿和 OOM。
- 缩略图加载失败不显示破图或崩溃。

### Phase 4：播放与模式（已完成）

**任务**

- [ ] P4-1 实现 `PlaybackController`：创建/释放 ExoPlayer，音频焦点、耳机拔出处理。
- [ ] P4-2 `PlayerPane` 通过 `AndroidView` 承载 `PlayerView`；设置媒体控制器和适配模式。
- [ ] P4-3 点击视频生成队列并播放；队列遵循当前筛选和排序。
- [ ] P4-4 顺序模式：`REPEAT_MODE_OFF`，自动前进，最后一项停止。
- [ ] P4-5 单曲循环：`REPEAT_MODE_ONE`，不前进。
- [ ] P4-6 播放模式切换 UI 和持久化。
- [ ] P4-7 播放状态回传：当前视频、播放/暂停、错误；列表高亮当前项。
- [ ] P4-8 生命周期：退后台暂停；旋转保留进度；离开主界面释放播放器。
- [ ] P4-9 播放中保持屏幕常亮。
- [ ] P4-10 视频不存在/解码失败提示，不影响继续选择其他视频。

**完成标准**

- 点击任意视频从 0 开始播放；播放中旋转不回到 0。
- 顺序模式按列表顺序播放，最后一节结束停止。
- 单曲循环反复播放当前视频。
- 退后台暂停；返回后可继续。
- 播放错误可见且可恢复。
- 连续切换相册/视频 20 次无明显内存增长或播放器叠加。

### Phase 5：多标签与筛选（已完成）

**任务**

- [ ] P5-1 标签筛选条：全部 / 未标记 / 当前相册已有标签；横向滚动。
- [ ] P5-2 多标签编辑底部面板：打开时载入当前视频的全部标签，已有标签以可勾选 chip 展示，支持同时选中多个。
- [ ] P5-3 自定义标签输入：输入后加入当前草稿选择；支持逐个取消和一键清空。
- [ ] P5-4 标签规范化：trim、压缩空白、单个标签最大 20 字符、同一视频内去重、保留名称校验。
- [ ] P5-5 保存时一次性提交该视频的完整标签集合，立即刷新列表、筛选项和标签摘要。
- [ ] P5-6 筛选逻辑：全部 / 未标记 / 单个标签；带多个标签的视频在任一所属标签下出现；无结果显示“清除筛选”引导。
- [ ] P5-7 若当前筛选标签因移除而不再存在，自动回退到“全部”并提示，避免停留在不可见的筛选项。
- [ ] P5-8 筛选条标签数量较多时排序稳定（使用次数降序、名称升序）。
- [ ] P5-9 标签集合持久化及重启恢复。

**完成标准**

- 同一视频可同时保存多个标签，列表以最多 2 个 chip + `+N` 展示，重启后全部保留。
- 选择任一所属标签都能筛到该视频；移除其中一个标签不影响其他标签。
- 移除最后一个使用某标签的关联后，该标签从筛选条消失；若正在筛选它，则回退“全部”并提示。
- 筛选“未标记”只显示没有任何标签的视频。
- 播放中改筛选不改变当前队列；下一次点击使用新筛选结果。
- 无标签/无匹配结果均有可操作空状态。
- 多标签组合筛选（AND/OR）不在本阶段范围内。

### Phase 6：测试、打磨与交付（部分完成）

**任务**

- [ ] P6-1 补齐单元测试和 Compose UI 测试。
- [ ] P6-2 按测试矩阵在 API 30 / 33 / 34+ 验证权限和布局。
- [ ] P6-3 统一中文文案、图标、亮暗色、按钮可点击区域和 contentDescription。
- [ ] P6-4 检查大字体、窄屏、横屏、刘海/手势导航下的布局。
- [ ] P6-5 检查空相册、文件删除、无解码器、DataStore 损坏等异常。
- [ ] P6-6 构建 Release APK，确认 R8 规则和 Media3 正常工作。
- [ ] P6-7 更新 README 的当前状态、构建说明和已知问题。
- [ ] P6-8 输出可安装 APK 和变更说明。

**完成标准**

- 所有单元测试通过；关键 UI 测试通过。
- Debug/Release 均可安装启动。
- 测试矩阵无 P0/P1 缺陷。
- README 与实际功能一致，不把计划写成已完成。

## 7. 界面与交互细化

### 7.1 竖屏

结构从上到下：

1. `TopAppBar`：相册名（可点击切换）、刷新/更多操作。
2. 播放区：16:9 黑色容器，内含 `PlayerView`；下方为模式切换按钮。
3. 标签筛选条：`LazyRow`，`FilterChip`；选中项明显。
4. 列表：`LazyColumn`，占满剩余高度，独立滚动。

### 7.2 横屏

- 整体 `Row`：左侧播放区 `weight(1f)`，右侧列表固定宽度（建议 320–380dp，随可用宽度收敛）。
- 右侧顶部放标签筛选条，下面列表独立滚动。
- 播放区和列表同时可见；不把播放器放进滚动容器。
- 使用 `BoxWithConstraints` 判断：`maxWidth > maxHeight` 时双栏。不要只依赖 `Configuration.orientation`，以实际可用宽度为准。

### 7.3 列表项

```text
[缩略图 96×54]  第01课：环境准备.mp4   [重点][待复习][+1]
                 12:36 · 320 MB              [标签图标]
```

- 整行点击播放。
- 标签按钮单独点击，不触发行播放。
- 列表行最多展示 2 个标签 chip，更多标签以 `+N` 表示；点击任意标签 chip 或标签按钮都打开编辑面板。
- 当前播放项有背景/边框高亮。
- 文件名最多两行，超出省略；长标签也省略。
- 点击标签按钮打开底部面板，不进入详情页。

### 7.4 多标签底部面板

- 标题：“设置标签”，副标题显示当前已选标签数量。
- 打开时把当前视频的全部标签载入为草稿，已有标签以可勾选 chip 展示；再次点击取消选择。
- 输入框用于新建标签；点击“添加”后加入草稿，并自动去重。
- MVP 不设每个视频的标签数量硬上限；面板内容可滚动，选中过多时仍能操作。
- 提供“清空全部”清除草稿选择，不立即写入。
- “保存”将完整标签集合一次性写入 DataStore；“取消”或下滑关闭丢弃未保存更改。
- 单个标签校验长度、空白和保留名称；错误在输入框下提示。
- 保存后关闭面板并刷新列表、标签摘要和筛选条。

选择多个标签是“视频同时拥有这些标签”，不是组合筛选条件；筛选仍按单个标签执行。

### 7.5 空与错误状态

| 场景 | 展示 |
| --- | --- |
| 权限未授予 | 全屏说明 + “授予权限” + “打开设置” |
| 受限权限 | 顶部提示条 + 管理权限入口；相册可用但提示可能不完整 |
| 无任何视频相册 | 全屏空状态 + 刷新 + 打开设置 |
| 相册内无视频 | 空状态 + 切换相册 |
| 筛选无结果 | 空状态 + “清除筛选” |
| 当前视频已删除 | Snackbar/错误条 + 自动移出队列或停止 |
| 解码失败 | 播放区错误信息 + 文件名 + 重试/选择其他视频 |
| 数据读取失败 | 可重试错误页，不崩溃 |

## 8. 权限流程

### 8.1 状态机

```text
Unknown
  ├─ 完整权限 → Ready(full)
  ├─ 受限权限 → Ready(limited)
  ├─ 拒绝 → Denied(canAskAgain=true/false)
  └─ 未请求 → Request
```

### 8.2 规则

- API 30–32：请求 `READ_EXTERNAL_STORAGE`。
- API 33：请求 `READ_MEDIA_VIDEO`。
- API 34+：请求 `READ_MEDIA_VIDEO`；Manifest 同时声明 `READ_MEDIA_VISUAL_USER_SELECTED` 以接收受限授权。
- 申请前展示用途说明，不在冷启动第一帧直接弹系统对话框。
- 用户拒绝且可再次请求：按钮再次请求。
- 用户拒绝且不可再请求：引导打开应用设置。
- 从设置返回或应用回到前台时重新检查权限并刷新数据。
- 受限权限不是错误：应用可用，但顶部提示“仅能访问部分视频”，提供管理入口。
- 权限变化导致当前相册不可用时，停止播放、回相册选择页。

### 8.3 隐私

- 不申请网络权限。
- 不上传、不备份媒体；DataStore 保留在应用私有目录。
- 不申请 `MANAGE_EXTERNAL_STORAGE`。
- 不接入统计、崩溃上报或广告 SDK。

## 9. 播放实现细节

### 9.1 队列与模式

```text
点击视频 V(index=i)
  → queue = 当前 visibleVideos
  → mediaItems = queue.map { it.uri }
  → player.setMediaItems(mediaItems, i, 0)
  → player.repeatMode = when(mode)
        SEQUENTIAL  -> Player.REPEAT_MODE_OFF
        SINGLE_LOOP -> Player.REPEAT_MODE_ONE
  → prepare(); play()
```

- 顺序模式依靠 ExoPlayer 播放列表自动前进；最后一项触发 `STATE_ENDED` 后停止。
- 单曲循环用 `REPEAT_MODE_ONE`，不改队列。
- 切换模式只改变 `repeatMode`，不重新 prepare。
- `onMediaItemTransition` 更新当前项；`onIsPlayingChanged` 更新 UI。
- 首次进入主界面不自动播放，等待用户选择。

### 9.2 生命周期

| 事件 | 行为 |
| --- | --- |
| 旋转 | ViewModel 和 ExoPlayer 保留；`PlayerView` 解绑/重绑；进度连续 |
| 应用退后台 | `ON_STOP` 暂停并记录位置；不启动前台服务 |
| 返回前台 | 保持暂停，等待用户继续 |
| 切换相册 | 停止并清空队列；释放旧媒体项 |
| 离开主界面 | `PlayerView` 解绑；播放器由 ViewModel 生命周期管理 |
| ViewModel 销毁 | `ExoPlayer.release()`，清空监听器 |
| 耳机拔出 | 暂停（`setHandleAudioBecomingNoisy(true)`） |
| 音频焦点丢失 | 按 ExoPlayer 默认策略暂停/ duck |
| 屏幕常亮 | 播放中 `keepScreenOn = true`，暂停/离开时关闭 |

### 9.3 异常

- `Player.Listener.onPlayerError`：转换为可读错误，保留列表。
- 视频 URI 失效：提示并允许选择其他视频；可选从当前队列移除。
- 不支持的编码：提示“设备无法播放该视频格式”，不循环重试。
- 不在错误回调中直接操作 Compose 状态，通过 ViewModel 事件发送。

## 10. 测试计划

### 10.1 单元测试

| 测试 | 覆盖点 |
| --- | --- |
| `NaturalOrderTest` | `第2课 < 第10课`、大小写、纯数字、中文、前缀相同、稳定性 |
| `TagJsonCodecTest` | 单标签/多标签/空数组读写、非法 JSON、未知字段、空字符串、超长/空白标签 |
| `TagRepositoryTest` | 添加、移除、替换整个标签集合、去重、聚合、保留名称拒绝 |
| `VideoFilterTest` | 全部、未标记、指定标签、多标签视频命中任一所属标签、标签不存在、无结果 |
| `PlaybackQueueTest` | 点击索引、首项/末项、顺序下一项、末项停止 |
| `MainViewModelTest` | 筛选切换、标签增删后列表刷新、筛选标签消失时回退、模式持久化、错误状态 |
| `MediaStoreRepositoryTest`（仪器） | 查询投影、空游标、相册聚合、重名相册 key |

### 10.2 Compose UI 测试

- 无相册时显示空状态。
- 点击相册进入主界面。
- 点击标签按钮打开面板，勾选多个标签并保存后列表显示标签摘要。
- 再次打开面板取消其中一个标签，其他标签仍保留。
- 标签筛选条切换后列表项数量正确；多标签视频在任一所属标签下都能筛到。
- 无筛选结果时点击“清除筛选”恢复全部。
- 横屏/竖屏可通过测试 Activity 或设备旋转验证核心元素存在。

### 10.3 手工测试矩阵

| 场景 | 步骤 | 期望 |
| --- | --- | --- |
| API 30 权限 | 全新安装并授权 | 相册和视频可读，播放正常 |
| API 33 权限 | 只授予视频权限 | 不请求图片/音频权限 |
| API 34+ 完整 | 选择“允许所有” | 所有相册可见 |
| API 34+ 受限 | 选择“仅选择部分” | 显示提示，仅显示已授权视频，可去设置 |
| 拒绝权限 | 拒绝并勾选不再询问 | 显示设置引导，不反复弹窗 |
| 首次启动 | 安装后不选相册 | 无法进入主界面；选择后持久化 |
| 重启 | 选择相册后杀进程重开 | 直接进入主界面 |
| 相册删除 | 系统删除相册后重开 | 回相册选择并提示 |
| 旋转播放中 | 播放 30 秒后旋转 | 画面继续，进度不重置 |
| 退后台 | 播放中按 Home | 暂停；返回不自动播放 |
| 顺序播放 | 选中间一节，等待结束 | 依次播放后续，末项停止 |
| 单曲循环 | 选短片段开启循环 | 反复播放当前项 |
| 末项顺序 | 从最后一节播放到结束 | 停止，不跳回第一项 |
| 多标签设置 | 给同一视频勾选 3 个以上标签并保存 | 列表显示前 2 个和 `+N`，编辑面板显示全部 |
| 标签持久化 | 设置多个标签后重启 | 所有标签仍显示 |
| 单标签筛选 | 选择视频所属的任一标签 | 该视频都会出现 |
| 移除标签 | 在多标签面板取消其中一个 | 其他标签保留，筛选结果同步更新 |
| 筛选标签消失 | 筛选“重点”，再移除相册内最后一个“重点”标签 | 自动回退“全部”并提示 |
| 播放中筛选 | 播放时切到其他标签 | 当前播放不中断 |
| 空相册 | 选择无视频目录对应的相册 | 空状态可操作 |
| 无筛选结果 | 选择一个没有视频的标签 | 清除筛选入口 |
| 大相册 | 200+ 视频滚动 | 无明显卡顿/OOM |
| 文件删除 | 播放前/播放中删除文件 | 可读错误提示，不崩溃 |
| 拔耳机 | 播放中拔有线耳机 | 暂停 |
| 大字体 | 系统字体放大 | 文本不重叠，按钮可点 |
| 深色模式 | 系统切换深色 | 颜色正常，无不可读文本 |

## 11. 风险与应对

| 风险 | 影响 | 概率 | 应对 |
| --- | --- | --- | --- |
| AGP 9.5.1 + Compose Compiler 配置不稳定 | 无法构建 | 中 | Phase 0 最小工程验证；优先沿用 AGP 内置 Kotlin 方案；必要时调整插件版本 |
| 官方 Maven 不可访问、镜像缺包 | 依赖下载失败 | 中高 | 阿里云镜像优先、官方兜底；必要时用 127.0.0.1:7890 代理 |
| 首次 Gradle 下载过大/超时 | Phase 0 延迟 | 中 | 使用腾讯 Gradle 镜像；保留校验和；网络恢复后重试 |
| Android 14+ 受限权限导致相册不完整 | 用户困惑 | 高 | 明确提示 + 管理权限入口；完整权限作为主路径测试 |
| MediaStore 相册桶名重复/跨卷 | 选择错误 | 中 | key 使用 volume + bucket；UI 显示路径提示 |
| 视频格式无解码器 | 播放失败 | 中 | 捕获 `onPlayerError`；提示格式问题，不崩溃 |
| 播放中旋转导致创建多个播放器 | 音频叠加/内存泄漏 | 中 | 播放器由 ViewModel 持有；Composable 只绑定；旋转连续测试 |
| 缩略图导致内存压力 | 滚动卡顿/OOM | 中 | LruCache、并发限制、合适尺寸、失败占位 |
| 标签关联 ID 变化 | 标签丢失 | 低中 | 文档说明；后续可加按文件名+大小的恢复策略 |
| 大相册全量查询 | 首屏慢 | 低 | MVP 接受；后续分页/增量加载 |
| 工作区无 Git 仓库 | 无法按提交回滚 | 低 | 开工前初始化 Git（若用户同意）并分阶段提交 |
| 无已连接设备 | 无法安装验收 | 高 | 启动模拟器或连接设备；先保证构建通过 |

## 12. 完成定义（Definition of Done）

一个阶段只有满足以下条件才可标记完成：

- [ ] 代码已实现，无临时 TODO 阻塞主流程。
- [ ] 相关单元测试/UI 测试通过。
- [ ] Debug 构建通过；涉及播放器/Release 配置时 Release 构建也通过。
- [ ] 在至少一个 API 30+ 设备/模拟器上实际运行。
- [ ] 竖屏、横屏各验证一次。
- [ ] 异常路径（权限拒绝、空数据、播放失败）不崩溃。
- [ ] README/计划中的行为与实际一致。
- [ ] 没有把密钥、代理、本机 SDK 路径提交到版本库。

项目整体完成时额外要求：

- 所有 F-01 至 F-12 验收通过。
- 测试矩阵无 P0/P1 缺陷，P2 有明确记录。
- 最终 APK 可安装，版本号更新。
- 标签、相册、播放模式经过“杀进程重启”验证；多标签全部保留。
- 最终交付说明包含构建方式、测试结果、已知限制。

## 13. 后续增强（非 MVP）

按价值排序，完成 MVP 后视需要再做：

1. 标签颜色、全局标签管理（重命名/合并/删除、使用统计）。
2. 多标签组合筛选（AND/OR）与标签快捷视图。
3. 播放进度记忆与“继续观看”。
4. 搜索、排序方式（文件名/时间/时长）。
5. 列表循环、随机播放、倍速。
6. 批量打标签、学习状态统计。
7. 画中画/后台播放（需前台服务与通知权限）。
8. SAF 目录接入，覆盖 MediaStore 未索引的文件。
9. 平板/折叠屏专用布局和键盘快捷键。

## 14. 开工前检查点

正式进入 Phase 1 前，确认以下默认决策无需调整：

- 相册来自本地 MediaStore，不支持云相册。
- 每个视频可设置多个标签；列表行展示 2 个 + `+N`。
- 标签筛选一次选择一个标签，多标签组合筛选不在 MVP。
- 默认文件名自然升序。
- 顺序播放到末尾停止。
- 退后台暂停。

这些决策只影响产品细节，不影响 Phase 0 的工程基线搭建；如有变化，在 Phase 1 数据模型和 Phase 5 标签 UI 前调整即可。
