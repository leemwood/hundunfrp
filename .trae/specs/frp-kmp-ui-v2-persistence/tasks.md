# Tasks

- [x] Task 1: 创建跨平台持久化抽象
  - [x] SubTask 1.1: 在 `commonMain/kotlin/cn/lemwood/data/` 创建 `SettingsStore.kt` expect class
  - [x] SubTask 1.2: 在 `commonMain/kotlin/cn/lemwood/data/` 创建 `TunnelConfigStore.kt` expect class
  - [x] SubTask 1.3: 创建 `ExportData.kt` 数据类封装导出格式（tunnels + settings）

- [x] Task 2: Android 持久化实现
  - [x] SubTask 2.1: 实现 `SettingsStore.android.kt`：使用 `DataStore<Preferences>` 保存 `AppSettings`
  - [x] SubTask 2.2: 实现 `TunnelConfigStore.android.kt`：使用应用私有文件保存隧道 JSON
  - [x] SubTask 2.3: 添加 Android DataStore 依赖

- [x] Task 3: Desktop 持久化实现
  - [x] SubTask 3.1: 实现 `SettingsStore.desktop.kt`：读写 `D:\.config\frp-kmp\settings.json`
  - [x] SubTask 3.2: 实现 `TunnelConfigStore.desktop.kt`：读写 `D:\.config\frp-kmp\tunnels.json`
  - [x] SubTask 3.3: 确保配置目录不存在时自动创建

- [x] Task 4: 集成 AppStateHolder 自动持久化
  - [x] SubTask 4.1: 在 `AppStateHolder` 构造函数/初始化中加载持久化数据
  - [x] SubTask 4.2: 在 `addTunnel/updateTunnel/deleteTunnel/toggleTunnel` 后自动保存隧道列表
  - [x] SubTask 4.3: 在 `updateSettings` 后自动保存设置
  - [x] SubTask 4.4: 提供 `resetToDefaults()` 清除持久化数据并恢复默认

- [x] Task 5: 实现导入导出逻辑
  - [x] SubTask 5.1: 实现 `ExportImportManager`（commonMain）序列化/反序列化 `ExportData`
  - [x] SubTask 5.2: 提供 `exportTo(path)` / `importFrom(path)` 方法并做基础校验
  - [x] SubTask 5.3: 处理导入失败时回滚到原数据

- [x] Task 6: SettingsScreen 连接真实操作
  - [x] SubTask 6.1: 将"导出"按钮连接到导出逻辑（先使用硬编码路径或文件选择器占位）
  - [x] SubTask 6.2: 将"导入"按钮连接到导入逻辑
  - [x] SubTask 6.3: 将"清除"按钮连接到 `AppStateHolder.resetToDefaults()` 并二次确认

- [x] Task 7: 验证与测试
  - [x] SubTask 7.1: 运行 `./gradlew :composeApp:assembleDebug`
  - [x] SubTask 7.2: 运行 `./gradlew :composeApp:run`，验证桌面端配置落盘
  - [x] SubTask 7.3: 验证重启后设置与隧道能够恢复

- [x] Task 8: 调整 Desktop 配置目录到 D 盘
  - [x] SubTask 8.1: 修改 Desktop `SettingsStore` 与 `TunnelConfigStore`，使用 `D:\.config\frp-kmp\`
  - [x] SubTask 8.2: 同步更新 SettingsScreen 默认导出路径
  - [x] SubTask 8.3: 重新运行 Desktop 验证，确认文件写入 D 盘

# Task Dependencies
- Task 2 depends on Task 1
- Task 3 depends on Task 1
- Task 4 depends on Task 2, Task 3
- Task 5 depends on Task 1
- Task 6 depends on Task 4, Task 5
- Task 7 depends on Task 6
- Task 8 depends on Task 3, Task 6, Task 7
