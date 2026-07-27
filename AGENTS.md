# AGENTS.md — Frp Tunnel (hundunfrp)

## 项目概述

跨平台 frp 隧道管理 GUI 客户端，支持 Android 和 Desktop。

| 项 | 值 |
|---|---|
| 语言 | Kotlin 2.1.0 |
| UI | Compose Multiplatform 1.8.0 |
| 构建 | Gradle 8.10.2 (Kotlin DSL) |
| 包名 | `cn.lemwood` |
| 目标 | Android (minSdk 24) + JVM Desktop (JVM 11) |
| 设计文档 | `.trae/rules/frp-ui-design-v2.md` |

---

## 关键路径

```
composeApp/src/
├── commonMain/kotlin/cn/lemwood/    ← 共享代码
│   ├── App.kt                       入口 composable
│   ├── model/                       数据模型 (@Serializable)
│   ├── state/AppStateHolder.kt      单例状态管理 (MutableStateFlow)
│   ├── ui/                          5个Screen composable
│   ├── components/                 11个通用组件
│   ├── navigation/                  Screen枚举 + NavigationType
│   ├── theme/                       Material3主题
│   ├── platform/FrpController.kt   expect桩 (frp后端桥接)
│   └── data/                        持久化抽象 (expect/actual)
├── androidMain/kotlin/cn/lemwood/   Android actual实现
└── desktopMain/kotlin/cn/lemwood/   Desktop actual实现
    └── 配置目录: D:\.config\frp-kmp\
```

---

## 构建命令

```bash
# 构建 Android Debug APK（需先设置 ANDROID_HOME 及 GRADLE_USER_HOME）
$env:GRADLE_USER_HOME = "E:\gradle-home"
$env:ANDROID_HOME = "E:\Android\Sdk"
./gradlew :composeApp:assembleDebug

# 运行桌面端
./gradlew :composeApp:run

# 清理构建
./gradlew clean
```

---

## 构建环境配置

### Gradle
- **User Home**: `E:\gradle-home`（见 `gradle.properties: org.gradle.user.home`）
- **Build Cache**: `E:\gradle-cache`（见 `gradle.properties: org.gradle.project.buildCacheDir`）
- **Distribution 镜像**: `mirrors.cloud.tencent.com/gradle`（见 `gradle-wrapper.properties`）
- **Maven 镜像**: settings.gradle.kts 按 `CI` 环境变量分流——CI 直连 google/mavenCentral（阿里云镜像间歇 502，Gradle 对 5xx 不 fallthrough），本地开发用 Aliyun + Huawei Cloud 镜像

### 运行构建前
```powershell
$env:GRADLE_USER_HOME = "E:\gradle-home"
$env:ANDROID_HOME = "E:\Android\Sdk"
```

### Android SDK
- **路径**: `E:\Android\Sdk`
- **SDK 35** + **Build-Tools 34** 已安装

---

## 架构约定

### 状态管理
- `AppStateHolder` 单例持有 `MutableStateFlow<AppState>`
- Screen 通过 `collectAsStateWithLifecycle()` 收集状态
- 所有 mutation 调用 `AppStateHolder.xxx()` → 自动持久化

### 跨平台模式
- `expect` 声明在 `commonMain/platform/` 和 `commonMain/data/`
- `actual` 实现在 `androidMain/` 和 `desktopMain/`
- Desktop 配置必须存 `D:` 盘（不用 C 盘）

### 组件规范
- 组件放在 `components/` 目录，每个文件一个组件
- 使用 Material3 + 自定义主题 token
- 遵循设计文档的颜色/尺寸/动效规范

### 数据流
```
用户操作 → AppStateHolder.action()
  → StateFlow更新 → collectAsState → UI重组
  → persist → 文件/DataStore
```

---

## 当前进度

### 已完成
- [x] UI基础框架（4个Screen + 11个组件）
- [x] 响应式导航（BottomNav / NavigationRail）
- [x] 亮暗主题系统
- [x] 持久化层（Android DataStore / Desktop JSON）
- [x] 导入导出
- [x] Mock数据用于UI预览
- [x] TunnelEditorScreen — 新增/编辑隧道表单（含验证）
- [x] SettingsRow / SectionHeader / ErrorLine 组件
- [x] TrafficChart — Canvas折线图
- [x] 页面过渡动画 — FadeThrough
- [x] 左滑删除 — SwipeToDismissBox + 撤销Snackbar
- [x] 构建环境配置（Gradle镜像/E盘缓存/Android SDK迁移）
- [x] 长按多选 — SelectionTopBar + 批量开关/删除
- [x] TunnelEditor 放弃修改确认弹窗
- [x] frp后端集成 — Desktop CLI子进程模式 + FrpConfigBuilder（Android 保持桩代码，需后续捆绑frpc二进制）
- [x] SettingsScreen「测试连接」按钮 — TCP Socket 连通性检测
- [x] Desktop CLI 模式 — `--headless` 标志无窗口运行
- [x] 自动连接 — autoStart 设置开启时启动即连
- [x] 前后端打通 — 移除全部 Mock 数据；AppStateHolder 持有 FrpController 并新增 connectServer/disconnectServer/updateTunnelStatus/updateTrafficTotals 等方法；FrpLogParser（日志事件解析）+ FrpAdminStatus（frpc admin API `/api/status` 轮询，端口 7400）回传真实隧道状态/流量/延迟；StatusScreen 流量图表与最近事件改状态驱动；SettingsScreen 新增连接/断开按钮；FrpConfigBuilder 修正为合法 frpc ini（段名=tunnel.id，含 admin 配置）；进程意外退出支持 autoReconnect 重连（最多5次）
- [x] Android frpc 二进制捆绑 — CI Go 交叉编译 v0.70.1 进 jniLibs（arm64-v8a），详见下方踩坑记录
- [x] GitHub Actions APK CI — push main 自动构建并上传 debug APK artifact
- [x] 消息通知（状态栏通知）— expect/actual `AppNotifier`（`platform/AppNotifier.kt`）；`AppStateHolder.sendNotification()` 在连接成功/断开/隧道错误时触发，遵守 `settings.notifications` 开关；Android 用 NotificationCompat + 渠道 `frp_status` + POST_NOTIFICATIONS（MainActivity 启动时 SDK 33+ 请求权限），Desktop 用 SystemTray 降级方案，两端均静默降级不抛异常
- [x] 初始化修复 — Android DataStore 漏持久化 `hasCompletedOnboarding` 导致二次启动必弹引导页，已补键；`SettingsStore`/`TunnelConfigStore` 新增 `hasData()`，`AppStateHolder.init()` 仅真正首启（store 无数据）才落盘默认值，不再每次启动重写配置
- [x] 电池优化适配 — expect/actual `BatteryOptimizer`（`platform/BatteryOptimizer.kt`）；Android：PowerManager 白名单检测 + `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 弹窗（Manifest 已加权限）+ 按厂商（小米/华为/OPPO/vivo/三星/魅族等）尝试自启动/后台管理 ComponentName、失败回退应用详情页；设置页「全局行为」新增「忽略电池优化」开关（1s 轮询刷新状态）和「后台运行设置」入口，仅 Android 显示
- [x] Android 前台服务保活 — `FrpForegroundService`（渠道 `frp_service` IMPORTANCE_LOW，START_STICKY，点击回主界面）+ expect/actual `ForegroundServiceController`；`AppStateHolder` 在连接成功启动服务、断开停止；Manifest 加 FOREGROUND_SERVICE + FOREGROUND_SERVICE_DATA_SYNC，service 声明 `foregroundServiceType="dataSync"`
- [x] Desktop 托盘与关窗行为 — `desktopMain/Tray.kt`：托盘菜单「显示主窗口/退出」，关窗隐藏到托盘（托盘不可用时直接断开退出）；所有退出路径（托盘退出/关窗/Ctrl+C hook）均 disconnect + 清理 TrayIcon（`AppNotifier.desktop.kt` companion 登记已建图标统一 remove）；headless 模式无启用隧道时提示退出，并修复 init 异步加载竞态（最多等 2s）
- [x] 动态取色 — expect/actual `dynamicColorScheme()`（Android SDK 31+ 用 material3 dynamic*ColorScheme，其余返回 null），`AppTheme` 内部消费 `settings.dynamicColor`，签名零改动
- [x] 引导页迁移 — Android SettingsStore `load()`：`has_completed_onboarding` 键缺失且 `server_addr` 非空时视为已完成引导（老版本升级用户不再重弹引导页）

### 未完成（按优先级）
1. **[低] 文件选择器（Desktop 导入导出路径手输）**
2. **[低] Android 端编译/真机验证**（Termux 无 SDK，依赖 CI assembleDebug；前台服务/通知/电池优化/动态取色的 androidMain 代码均只过了人工复查）

---

## 踩坑记录

### Android frpc 二进制捆绑（2026-07-27 完成）
- **方案**:`scripts/build-frpc-android.sh` 用 Go 交叉编译 frp v0.70.1 → `composeApp/src/androidMain/jniLibs/arm64-v8a/libfrpc.so`,CI 在 assembleDebug 前执行；产物已 gitignore，不入库。
- **仅 arm64-v8a**:Go 的 android 目标只有 arm64 支持纯 Go 内部链接，arm/amd64 报 "requires external (cgo) linking"（需 NDK)，不要再加其他 ABI。
- **web embed**:frp 源码 `web/frpc/embed.go` go:embed 的 dist 不在 git 里，构建前必须造占位 `web/frpc/dist/index.html`，否则编译失败（App 只用 admin API，不需要 dashboard)。
- **useLegacyPackaging**：保持 `false`（JNI 方案 `System.loadLibrary` 直接从 APK 加载 so，无需解压；旧的 exec 方案才需要解压到磁盘）。
- **加载路径**：frpc 由 `FrpcNative`（`System.loadLibrary("frpc_jni")`）进程内加载；targetSdk 29+ SELinux 禁止 exec 应用数据目录文件，**不要**改回复制二进制 exec 方案。
- **legacy ini 键名**：frp v0.70 ini 走 legacy 解析，日志键是 `log_file`/`log_way = file`（下划线），圆点键 `log.to` 是 toml 语法，写在 ini 里会被静默忽略（表现为 frpc.log 一直为空）。
- **cleartext loopback**:targetSdk 28+ 默认禁止明文 HTTP，**包括 127.0.0.1**；admin API 轮询（http://127.0.0.1:7400）依赖 `androidMain/res/xml/network_security_config.xml` 放开 loopback，删除会导致轮询全挂、反复误触发重连。

### frp v0.70 API 变更（2026-07-27 踩坑）
- `/api/status` 的代理 status 取值改为 `new / wait start / start error / running / check failed / closed`（旧版是 `online`/`offline`），按 `online` 判断会把运行中的隧道误判离线，与日志事件互相拉扯造成状态抖动。映射逻辑统一在 `FrpAdminStatus.toTunnelStatus()`。
- `/api/status` **不再返回流量字段**（today_traffic_in/out 已移除，流量只在 frps 端 prometheus），两端控制器对 0 值做了跳过处理，v0.70 下流量统计不可用属预期。
- `FrpLogParser` 的 ProxyClosed 只匹配 `proxy closed`，不能用裸 `closed`（工作连接关闭日志也含该词，会造成状态抖动）。

---

## 开发规范

1. 所有新文件包名统一 `cn.lemwood`
2. 数据模型必须 `@Serializable`
3. 组件参数末尾放 `modifier: Modifier = Modifier`
4. Desktop 配置路径为 `D:\.config\frp-kmp\`（不写 C 盘）
5. 构建前确保 Gradle 和 Go（frp-bridge）镜像代理可用
6. 功能完成后运行构建验证
