# Tasks

- [x] Task 1: 创建 KMP Compose Multiplatform 项目脚手架，包名统一为 `cn.lemwood`
  - [x] SubTask 1.1: 创建 `composeApp/src/commonMain/kotlin/cn/lemwood` 目录结构
  - [x] SubTask 1.2: 创建 `androidMain` 与 `desktopMain` 对应包目录
  - [x] SubTask 1.3: 配置 `build.gradle.kts` 启用 Compose Multiplatform、Android、Desktop 目标
  - [x] SubTask 1.4: 验证 Gradle sync 成功

- [x] Task 2: 实现主题系统（Theme / Color / Type / Shape / Dimen）
  - [x] SubTask 2.1: 定义亮/暗色 ColorScheme
  - [x] SubTask 2.2: 定义 Typography
  - [x] SubTask 2.3: 定义 Shapes（cardRadius=12dp, chipRadius=4dp, dialogRadius=16dp, buttonRadius=20dp）
  - [x] SubTask 2.4: 实现主题切换与跟随系统暗色

- [x] Task 3: 实现导航模型与响应式导航外壳
  - [x] SubTask 3.1: 定义 `Screen` 枚举（TunnelList, Status, Settings, Log）
  - [x] SubTask 3.2: 实现 `NavigationType` 断点判断（Compact < 600dp, Expanded ≥ 840dp）
  - [x] SubTask 3.3: 实现 `AppScaffold` 组合 BottomNav / NavRail 与主内容区

- [x] Task 4: 实现数据模型与 `AppStateHolder`
  - [x] SubTask 4.1: 定义 `TunnelUiState`, `ServerStatus`, `LogEntry`, `AppSettings`, `AppState`, `UIState`
  - [x] SubTask 4.2: 实现 `AppStateHolder` 单例与 `MutableStateFlow<AppState>`
  - [x] SubTask 4.3: 添加隧道 CRUD actions、服务器状态 actions、日志 actions
  - [x] SubTask 4.4: 提供 mock 数据便于 UI 预览

- [x] Task 5: 实现核心 UI 组件
  - [x] SubTask 5.1: `StatusBadge`
  - [x] SubTask 5.2: `TypeChip`
  - [x] SubTask 5.3: `TrafficRow`
  - [x] SubTask 5.4: `AddressLine`
  - [x] SubTask 5.5: `EmptyState`
  - [x] SubTask 5.6: `ErrorBanner`
  - [x] SubTask 5.7: `ConfirmDialog`
  - [x] SubTask 5.8: `SkeletonLoader`
  - [x] SubTask 5.9: `SearchBar`
  - [x] SubTask 5.10: `FilterChips`
  - [x] SubTask 5.11: `LogEntryRow`

- [x] Task 6: 实现隧道列表页（TunnelListScreen）
  - [x] SubTask 6.1: 实现 `TunnelCard` 组件（在线/离线/连接中/错误/多选状态）
  - [x] SubTask 6.2: 实现搜索栏与过滤芯片
  - [x] SubTask 6.3: 实现空状态与搜索无结果状态
  - [x] SubTask 6.4: 实现 FAB 跳转到编辑器占位
  - [x] SubTask 6.5: 实现下拉刷新占位（UI 反馈）

- [x] Task 7: 实现状态页、设置页、日志页
  - [x] SubTask 7.1: `StatusScreen` 基础布局（连接状态、流量统计、隧道活跃度占位）
  - [x] SubTask 7.2: `SettingsScreen` 分组列表（服务端、全局、外观、数据、关于占位）
  - [x] SubTask 7.3: `LogScreen` 日志列表与级别过滤

- [x] Task 8: 实现 Android 与 Desktop 入口
  - [x] SubTask 8.1: Android `MainActivity` 调用 `App()`
  - [x] SubTask 8.2: Desktop `Main.kt` 调用 `application { App() }`

- [x] Task 9: 构建验证与代码整理
  - [x] SubTask 9.1: 运行 `./gradlew :composeApp:assembleDebug`
  - [x] SubTask 9.2: 运行 `./gradlew :composeApp:run`
  - [x] SubTask 9.3: 清理未使用文件，确保无编译警告

- [x] Task 10: 修复验证阶段发现的 checklist 不符项
  - [x] SubTask 10.1: 在 `AppState` 中增加 `notifications: List<Notification>` 字段，并创建 `Notification` 数据类
  - [x] SubTask 10.2: 在 `TunnelUiState` 中增加 `traffic: Traffic` 字段（data class Traffic(up, down)），兼容现有 `trafficUp`/`trafficDown`
  - [x] SubTask 10.3: 将 `ConfirmDialog` 确认按钮改为红色 `FilledTonalButton`
  - [x] SubTask 10.4: 重新编译验证三端构建

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 2
- Task 4 depends on Task 1
- Task 5 depends on Task 2
- Task 6 depends on Task 4, Task 5
- Task 7 depends on Task 4, Task 5
- Task 8 depends on Task 3, Task 6, Task 7
- Task 9 depends on Task 8
- Task 10 depends on Task 4, Task 5
