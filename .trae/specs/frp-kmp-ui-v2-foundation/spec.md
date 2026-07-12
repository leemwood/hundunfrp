# FRP KMP UI v2 基础实现 Spec

## Why
根据 `d:\project\hundunfrp\.trae\rules\frp-ui-design-v2.md` 设计文档，需要建立 Compose Multiplatform 跨端 UI 基础框架，实现隧道列表、状态、设置、日志四大核心页面及基础组件库，包名统一为 `cn.lemwood`。本次 Spec 聚焦基础框架与核心 UI，JNI / frp 后端集成在后续 Spec 中推进。

## What Changes
- 创建 KMP Compose Multiplatform 项目结构（commonMain / androidMain / desktopMain）
- 包名统一改为 `cn.lemwood.*`
- 实现主题系统（亮/暗色、颜色、字体、圆角），对齐设计文档第 10 章
- 实现响应式导航：手机 BottomNavigation（< 600dp）/ 桌面 NavigationRail（≥ 840dp）
- 实现 `AppStateHolder` 单例状态管理与数据模型
- 实现隧道列表页（TunnelListScreen）及核心组件 TunnelCard
- 实现状态页、设置页、日志页基础结构
- 实现公共组件库：StatusBadge、TypeChip、TrafficRow、LogEntryRow、EmptyState、ErrorBanner、ConfirmDialog、SkeletonLoader、SearchBar、FilterChips
- 实现 Android MainActivity 与 Desktop Main 入口

## Impact
- 新增 Android 与 Desktop 双端入口
- 新增共享 UI 模块与统一状态管理
- 为后续 JNI 桥接、frp 后端集成、持久化、托盘/通知等平台特性奠定基础

## ADDED Requirements

### Requirement: 项目结构与包名
The system SHALL use `cn.lemwood` as the root package for all Kotlin source files across commonMain, androidMain, and desktopMain.

#### Scenario: Success case
- **WHEN** inspecting source directories
- **THEN** all package declarations start with `cn.lemwood`

### Requirement: 主题系统
The system SHALL provide Material3-based light and dark themes with the tokens defined in chapter 10 of the design doc.

#### Scenario: Light theme
- **WHEN** the app uses light theme
- **THEN** primary is `#1565C0`, secondary `#00897B`, online `#4CAF50`, error `#E53935`, surface `#FFFFFF`

#### Scenario: Dark theme
- **WHEN** the app uses dark theme
- **THEN** primary is `#64B5F6`, secondary `#4DB6AC`, surface `#1A1C23`

### Requirement: 响应式导航
The system SHALL switch navigation UI based on window width: BottomNavigation for compact (< 600dp) and NavigationRail for expanded (≥ 840dp).

#### Scenario: Phone
- **WHEN** the app runs on a phone
- **THEN** a 4-tab BottomNavigation is shown (Tunnels, Status, Settings, Logs)

#### Scenario: Desktop
- **WHEN** the app runs on desktop
- **THEN** a NavigationRail is shown on the left with the same 4 tabs

### Requirement: 状态管理
The system SHALL provide a singleton `AppStateHolder` exposing a `MutableStateFlow<AppState>` with tunnels, serverStatus, logs, settings, uiState, and notifications.

#### Scenario: Toggle tunnel
- **WHEN** the user toggles a tunnel switch
- **THEN** `AppState.tunnels` updates and the UI recomposes

### Requirement: 隧道列表页
The system SHALL display tunnels as cards in a LazyColumn, each showing name, status badge, protocol chip, address line, traffic row, and enable/disable switch.

#### Scenario: Online tunnel
- **WHEN** a tunnel is online
- **THEN** the card shows a green left border, green status badge, and live traffic numbers

#### Scenario: Offline tunnel
- **WHEN** a tunnel is offline
- **THEN** the card uses a muted surface, shows gray status badge, and displays last-seen placeholder

#### Scenario: Empty state
- **WHEN** no tunnels exist
- **THEN** an EmptyState with "还没有隧道" message and FAB guidance is shown

### Requirement: 组件库
The system SHALL provide reusable components matching chapter 6 of the design doc.

#### Scenario: Components in use
- **WHEN** screens use `TunnelCard`, `StatusBadge`, `TypeChip`, `TrafficRow`, `LogEntryRow`, `EmptyState`, `ErrorBanner`, `ConfirmDialog`, `SkeletonLoader`, `SearchBar`, `FilterChips`
- **THEN** they render according to the documented specs (colors, sizing, typography, states)

### Requirement: 其他页面
The system SHALL provide `StatusScreen`, `SettingsScreen`, and `LogScreen` shells reachable from navigation.

#### Scenario: Navigation
- **WHEN** the user taps each navigation tab
- **THEN** the corresponding screen is displayed with its top-level title and placeholder content

### Requirement: 平台入口
The system SHALL provide `MainActivity` for Android and `Main` for Desktop that both launch the shared `App` composable.

#### Scenario: Build
- **WHEN** running Gradle build
- **THEN** both `:composeApp:assembleDebug` (Android) and `:composeApp:run` (Desktop) succeed

## MODIFIED Requirements
None.

## REMOVED Requirements
None.
