# Frp Tunnel (hundunfrp)

跨平台 frp 隧道管理 GUI 客户端，基于 Kotlin Compose Multiplatform，支持 Android 与 Desktop。

[![Android APK](https://github.com/leemwood/hundunfrp/actions/workflows/android-apk.yml/badge.svg)](https://github.com/leemwood/hundunfrp/actions/workflows/android-apk.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.8.0-4285F4)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green)](LICENSE)

## 功能

- **隧道管理** — 新增/编辑/删除隧道（TCP/UDP/HTTP/HTTPS/STCP/XTCP），表单校验、左滑删除 + 撤销、长按多选批量开关/删除
- **实时监控** — 服务器连接状态、延迟、运行时长；每隧道在线状态与上下行流量；流量趋势图表
- **真实 frpc 后端** — 以子进程方式运行 frpc，解析日志事件 + 轮询 admin API（`/api/status`）回传状态，非 Mock 数据
- **自动重连** — frpc 意外退出时自动重连（最多 5 次，可在设置中开关）
- **日志中心** — frpc 输出分级展示（INFO/WARN/ERROR），支持清空
- **配置管理** — 隧道与设置持久化，支持导入/导出 JSON
- **跨平台 UI** — Material3 主题、亮暗模式、响应式导航（手机 BottomNav / 宽屏 NavigationRail）

## 快速开始

### 下载

在 [Actions](https://github.com/leemwood/hundunfrp/actions/workflows/android-apk.yml) 页面最新运行的 Artifacts 中下载 `hundunfrp-debug-apk`。

### 从源码构建

前置依赖：JDK 17+、Android SDK 35（构建 APK 时）。

```bash
git clone https://github.com/leemwood/hundunfrp.git
cd hundunfrp

# Android Debug APK（产物在 composeApp/build/outputs/apk/debug/）
./gradlew :composeApp:assembleDebug

# 运行桌面端
./gradlew :composeApp:run
```

### 使用

1. 打开「设置」，填入 frps 服务器地址、端口与令牌，点「测试连接」确认可达后点「连接」
2. 在「隧道」页点右下角 + 添加隧道（本地地址/端口、远程端口）
3. 开关隧道即自动重启 frpc 使配置生效，状态与流量实时回显

## 平台说明

| 平台 | 状态 | 说明 |
|------|------|------|
| Desktop (JVM) | 可用 | 需自备 `frpc` 二进制：放入 PATH 或 `D:\.config\frp-kmp\` 目录 |
| Android | 可用（arm64） | APK 内置 frpc v0.70.1（CI 用 Go 交叉编译，`jniLibs/arm64-v8a/libfrpc.so`），开箱即用；仅支持 arm64-v8a 设备 |

桌面端支持 `--headless` 无窗口 CLI 模式。

## 技术栈

| 层 | 技术 |
|----|------|
| 语言 | Kotlin 2.1.0 |
| UI | Compose Multiplatform 1.8.0 (Material3) |
| 状态 | StateFlow 单例（`AppStateHolder`），UI `collectAsState` 收集 |
| 后端桥接 | expect/actual `FrpController`，frpc 子进程 + admin API 轮询 |
| 持久化 | Android DataStore / Desktop JSON |
| 构建 | Gradle 8.10.2 (Kotlin DSL)，AGP 8.6.1 |

## 路线图

- [x] Android frpc 二进制捆绑进 APK（jniLibs，CI Go 交叉编译，arm64-v8a）
- [ ] Android 前台服务 / 常驻通知
- [ ] Desktop 系统托盘
- [ ] 动态取色（Material You）

## 贡献

欢迎 Issue 与 PR。开发约定见 [AGENTS.md](AGENTS.md)。

## License

[MIT](LICENSE) © 2026 leemwood
