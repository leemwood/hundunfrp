# FRP KMP UI v2 持久化与配置管理 Spec

## Why
基础 UI 框架已完成，但当前所有数据均为内存 mock，应用重启后丢失。需要引入持久化层保存隧道配置与应用设置，并打通 UI 与数据层的读写，使配置变更真正可留存。

## What Changes
- 新增跨平台持久化 expect/actual 抽象（`SettingsStore`、`TunnelConfigStore`）
- Android 使用 DataStore / SharedPreferences 保存设置，内部文件保存隧道 JSON
- Desktop 使用 `D:\\.config\\frp-kmp\\settings.json` 与 `D:\\.config\\frp-kmp\\tunnels.json`（遵守工作区 D 盘规则，不使用 C 盘）
- 启动时自动加载持久化数据到 `AppStateHolder`
- 设置变更、隧道增删改后自动保存
- 实现导入/导出配置入口（UI 占位 + 实际 JSON 序列化）
- 保持包名 `cn.lemwood`，不影响现有 UI 与状态管理公共 API

## Impact
- 受影响模块：`cn.lemwood.state.AppStateHolder`、`cn.lemwood.model.*`、新增 `cn.lemwood.data.*`
- 为后续 frp 后端集成提供真实配置来源
- 为后续 SettingsScreen 的“导出/导入/清除”提供实际能力

## ADDED Requirements

### Requirement: 跨平台持久化抽象
The system SHALL provide `expect class SettingsStore` and `expect class TunnelConfigStore` in commonMain with Android/Desktop actual implementations.

#### Scenario: Success case
- **WHEN** commonMain code calls `settingsStore.save(settings)` or `tunnelStore.save(tunnels)`
- **THEN** the corresponding platform-specific store persists the data

### Requirement: 设置持久化
The system SHALL persist `AppSettings` across app restarts.

#### Scenario: Launch
- **WHEN** the app starts
- **THEN** it loads the last saved `AppSettings` into `AppStateHolder`

#### Scenario: Change
- **WHEN** the user changes a setting in `SettingsScreen`
- **THEN** the new settings are saved within 1 second and survive app restart

### Requirement: 隧道配置持久化
The system SHALL persist the tunnel list as JSON and reload it on startup. On Desktop, files are stored under `D:\.config\frp-kmp\` to comply with the workspace D-drive rule.

#### Scenario: Launch
- **WHEN** the app starts
- **THEN** it loads saved tunnels into `AppStateHolder`, replacing mock data

#### Scenario: CRUD
- **WHEN** a tunnel is added, updated, or deleted
- **THEN** the tunnel list is serialized to JSON and saved

### Requirement: AppStateHolder 集成持久化
The system SHALL update `AppStateHolder` so that all mutating actions persist changes automatically.

#### Scenario: Toggle tunnel
- **WHEN** `AppStateHolder.toggleTunnel(id)` is called
- **THEN** the tunnel list is saved after the state update

### Requirement: 导入导出配置
The system SHALL provide functions to export all tunnels and settings to a JSON file and import them back.

#### Scenario: Export
- **WHEN** the user taps "导出" in SettingsScreen
- **THEN** a JSON file containing tunnels + settings is written to a user-chosen location

#### Scenario: Import
- **WHEN** the user taps "导入" in SettingsScreen
- **THEN** a JSON file is parsed and replaces current tunnels/settings, with validation

### Requirement: 清除数据
The system SHALL provide a "清除" action that removes all persisted tunnels and settings and resets to defaults.

#### Scenario: Clear
- **WHEN** the user confirms clear in SettingsScreen
- **THEN** all persisted data is deleted and `AppStateHolder` resets to default state

## MODIFIED Requirements
None.

## REMOVED Requirements
None.
