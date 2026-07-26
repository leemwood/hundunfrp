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
- **Maven 镜像**: settings.gradle.kts 配置了 Aliyun + Huawei Cloud 镜像（Google、Maven Central、Gradle Plugin Portal、JetBrains）

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

### 未完成（按优先级）
1. **[中] Android frpc 二进制捆绑** — 将 frpc 编译进 APK 的 jniLibs
2. **[低] Android前台服务 / 通知**
3. **[低] Desktop系统托盘 / CLI模式**
4. **[低] 引导页 / 文件选择器 / 动态取色**

---

## 开发规范

1. 所有新文件包名统一 `cn.lemwood`
2. 数据模型必须 `@Serializable`
3. 组件参数末尾放 `modifier: Modifier = Modifier`
4. Desktop 配置路径为 `D:\.config\frp-kmp\`（不写 C 盘）
5. 构建前确保 Gradle 和 Go（frp-bridge）镜像代理可用
6. 功能完成后运行构建验证
