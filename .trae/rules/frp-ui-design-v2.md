# Frp KMP — 前端 UI 设计 v2 (第1~8章)

---

## 1. 设计原则

### 1.1 核心原则

| 原则 | 说明 |
|:----|:-----|
| **一套代码，三端运行** | Compose Multiplatform 共享全部 UI 逻辑 |
| **Mobile-first** | 以手机交互为基准，向上适配桌面/平板 |
| **隧道即卡片** | 每条隧道一张可操作卡片，直观清晰 |
| **状态可见** | 连接状态、流量、错误全部一眼可知 |
| **操作可逆** | 开关、删除等操作可撤销/二次确认 |

### 1.2 设计语言

- **色调体系**：科技蓝 `#1565C0` + 青绿 `#00897B` → 稳定可靠
- **卡片化 UI**：每条隧道一张 Card，信息 + 操作为一体
- **克制用色**：彩色仅用于状态指示，信息层级靠黑白灰表达
- **圆角语言**：12dp 大圆角卡片柔和，4dp 小圆角标签干脆
- **图标体系**：Material Icons，每个操作配图标增强辨识

### 1.3 交互哲学

```
轻量操作 → 即时响应         (开关隧道、切换Tab)
重要操作 → 二次确认         (删除隧道、断开服务器)
批量操作 → 长按进入选择模式   (批量开关/删除)
数据更新 → 下拉刷新 + 自动轮询 (实时更新)
配置变更 → 保存即生效        (无需重启)
```

---

## 2. 整体架构

### 2.1 页面层级

```
App
├── 🏠 TunnelListScreen          ← 默认首页
│   ├── SearchBar + FilterChips
│   ├── LazyColumn { TunnelCard* }
│   └── FAB → 跳转 TunnelEditorScreen
│
├── 📊 StatusScreen              ← 全局监控仪表盘
│   ├── 服务端连接状态卡片
│   ├── 今日流量统计 (上行/下行/峰值)
│   ├── 隧道活跃度仪表 (进度条)
│   └── 每条隧道快速状态列表
│
├── ⚙️ SettingsScreen            ← 配置中心
│   ├── 服务端连接 (地址/端口/令牌/测试)
│   ├── 全局行为 (自启/重连/通知/超时)
│   ├── 外观 (主题/动态色)
│   ├── 数据管理 (导入/导出/清除)
│   └── 关于 (版本/核心/许可)
│
├── 📋 LogScreen                 ← 日志查看器
│   ├── 级别过滤 (All/INFO/WARN/ERROR)
│   ├── LazyColumn 虚拟列表
│   └── 自动/手动滚动切换
│
├── 📝 TunnelEditorScreen        ← 隧道编辑 (全屏)
│   ├── 基本信息 (名称/协议)
│   ├── 网络配置 (地址/端口)
│   ├── 安全选项 (加密/压缩/TLS)
│   └── 高级设置 (自定义域名/HTTP认证) 可折叠
│
├── 🗑️ DeleteConfirmDialog       ← 删除确认
├── 📤 ImportExportDialog        ← 导入导出
├── 🔔 SnackbarHost              ← 全局轻提示
└── ⚠️ ErrorBanner               ← 全局错误横幅
```

### 2.2 状态管理架构

```
AppStateHolder (单例)
├── MutableStateFlow<AppState>
│   ├── tunnels: List<TunnelUiState>      # 隧道列表
│   ├── serverStatus: ServerStatus        # 服务端状态
│   ├── logs: List<LogEntry>              # 日志 (环形缓冲)
│   ├── settings: AppSettings             # 设置
│   ├── uiState: UIState                  # idle/loading/error
│   └── notifications: List<Notification>
│
├── Actions: tunnel CRUD / server / logs / settings / ui
│
└── CoroutineScope
    ├── 自动轮询 (3s 间隔拉取状态)
    ├── 日志管道 (frp 事件推送)
    └── 自动重连 (断线重试)
```

### 2.3 数据流

```
用户操作 → AppStateHolder.action()
  → StateFlow 更新 → collectAsState() → UI 重组
  → FrpController.actual() (JNI/CLI)
  → frp 后端执行 → 回调 → 状态更新 → UI 刷新
```

### 2.4 持久化

- 隧道配置 → JSON 文件 (Desktop) / SharedPreferences (Android)
- 设置 → DataStore / Preferences
- 日志 → 内存环形缓冲 (不持久化)

---

## 3. 导航方案

### 3.1 手机 (< 600dp)

```
┌─────────────────────────┐
│  🔌 Frp Tunnel   🟢    │ ← TopAppBar
├─────────────────────────┤
│    [页面内容区域]         │
├─────────────────────────┤
│ 🏠  │  📊  │  ⚙️  │  📋  │ ← BottomNav
└─────────────────────────┘
```
编辑页全屏推入，隐藏 BottomNav。

### 3.2 平板/桌面 (≥ 840dp)

```
┌──────────┬──────────────────────┐
│ 🏠 隧道   │ 🔌 Frp Tunnel  🟢   │
│ 📊 状态   ├──────────────────────┤
│ ⚙️ 设置   │  [主内容区域]         │
│ 📋 日志   │                      │
│ ───────  │                      │
│ [+ 新增]  │                      │
└──────────┴──────────────────────┘
```
NavigationRail 72dp + 编辑页右侧面板 480dp。

### 3.3 二级导航流

```
TunnelList → 点击卡片 → Editor (编辑)
TunnelList → 长按卡片 → 多选模式 (批量开关/删除)
TunnelList → 左滑卡片 → 删除确认
TunnelList → 点击 FAB → Editor (新增)
Editor → 保存 → Snackbar → 回列表
Settings → 测试连接 → Dialog 结果
```

---

## 4. 页面详细设计

### 4.1 TunnelListScreen — 隧道列表

#### 布局

```
┌── TopAppBar ────────────────────────┐
│  🔌 Frp Tunnel          🟢 已连接   │
├── SearchBar ────────────────────────┤
│  🔍 搜索名称/端口...                │
├── FilterChips (水平滚动) ──────────┤
│  [全部] [在线 3] [离线 2] [TCP]     │
├── LazyColumn ──────────────────────┤
│  ┌ TunnelCard (在线) ────────────┐ │
│  │ [ON] mc-server     🟢 TCP     │ │
│  │ 127.0.0.1:25565 → :25565     │ │
│  │ ↑ 1.2 MB  ↓ 3.5 MB           │ │
│  └───────────────────────────────┘ │
│  ┌ TunnelCard (离线) ───────────┐ │
│  │ [OFF] ssh-dev     ⚪ TCP     │ │
│  │ 127.0.0.1:22 → :7022         │ │
│  │ 上次在线: 2小时前             │ │
│  └───────────────────────────────┘ │
├── FAB ─────────────────────────────┤
│           [+ 新增]                 │
└────────────────────────────────────┘
```

#### 6 种状态

| 状态 | 表现 |
|:----|:-----|
| 正常列表 | 卡片 + Switch + 流量 |
| 加载中 | shimmer 骨架屏动画 |
| 空 (无隧道) | 📡 插画 + "还没有隧道" |
| 空 (搜索无结果) | 🔍 插画 + "没有匹配" + [清除过滤] |
| 错误 (服务端断) | ErrorBanner + 重连计数 + 操作按钮 |
| 多选模式 | TopAppBar 切换 + checkbox + 批量操作按钮 |

#### 交互矩阵

| 手势 | 效果 | 反馈 |
|:----|:-----|:-----|
| 点击卡片 | 编辑页 | Ripple |
| 点击 Switch | 即时开关 | 背景色过渡 400ms |
| 长按卡片 | 多选模式 | 震动 + 卡片抬升 |
| 左滑卡片 | 露出删除按钮 | 红底白字 |
| 下拉列表 | 刷新 | 顶部进度条 |
| 点击 FAB | 新增 | — |
| 双击 TopBar | 滚动到顶 | 平滑滚动 |

#### 搜索过滤算法

```
搜索: name / localAddr / localPort / remotePort 文本匹配
过滤: 类型(ALL/TCP/HTTP/…) AND 状态(ALL/ONLINE/OFFLINE/ERROR)
排序: 在线优先 → 连接中 → 离线 → 错误
      同状态按名称字母序
```

### 4.2 TunnelEditorScreen — 隧道编辑

#### 表单

```
┌── TopAppBar: ← 新增隧道 [保存]
├── 名称:     [________________]
├── 协议:     [TCP ▾]
├── 本地地址: [127.0.0.1]  本地端口: [25565]
├── 远程端口: [25565]
├── ☐ 加密  ☐ 压缩  ☐ TLS
└── ▸ 高级选项 (可折叠)
    ├── 自定义域名: [________]
    ├── HTTP 用户:  [________]
    └── HTTP 密码:  [________]
```

#### 验证规则

| 字段 | 必填 | 规则 | 错误信息 |
|:----|:-----|:-----|:---------|
| 名称 | ✓ | 1-32字符 | "请输入隧道名称" |
| 协议 | ✓ | — | — |
| 本地地址 | ✓ | 合法IP/域名 | "地址格式不正确" |
| 本地端口 | ✓ | 1-65535 | "有效端口: 1-65535" |
| 远程端口 | ✓ | 1-65535, 唯一 | "端口已被使用" |
| 自定义域名 | ✗ | 合法域名 | "域名格式不正确" |

#### 场景

| 场景 | 表现 |
|:----|:-----|
| 新增 | 空白表单 + placeholder |
| 编辑 | 预填 + 保存更新 |
| 未修改返回 | 直接返回 |
| 已修改返回 | "放弃修改？" 弹窗 |
| 保存失败 | 弹窗提示原因 |

### 4.3 StatusScreen — 全局状态

```
┌── 服务端连接 ─────────────────────┐
│  🟢 已连接 frp.example.com:7000    │
│  延迟 23ms  |  运行 12h34m         │
└────────────────────────────────────┘
┌── 今日流量 ───────────────────────┐
│  上行 15.2 MB  |  下行 42.7 MB    │
│  峰值 2.3 MB/s |  总计 57.9 MB    │
│  Canvas 折线图 (5min/1h/今日/本周)│
└────────────────────────────────────┘
┌── 隧道活跃度 ────────────────────┐
│  3/5 在线  ████████░░░░░░  60%    │
│  🟢 mc-server    ↑1.2M            │
│  🟡 ssh-dev      (重连中)         │
│  🔴 db-tunnel    (错误)           │
└────────────────────────────────────┘
┌── 最近事件 ──────────────────────┐
│  12:34 mc-server 流量异常         │
│  12:30 ssh-dev 断开               │
└────────────────────────────────────┘
```

### 4.4 LogScreen — 日志

```
┌── TopAppBar: 📋 日志 [清空] [⇅]
├── FilterRow: [ALL] [INFO] [WARN] [ERROR]
├── LazyColumn:
│  [INFO] 12:34:56 连接成功
│  [WARN] 12:35:10 ssh-dev 断开 | 重连 1/5
│  [ERROR]12:35:12 连接超时
└── 底部指示器: ████████░░ 自动滚动
```

级别着色: DEBUG=灰, INFO=正常, WARN=橙底, ERROR=红底+左边框

### 4.5 SettingsScreen — 设置

```
├── 服务端: 地址 [__] 端口 [__] 令牌 [__] [测试连接]
├── 全局: 自启 ☑ 重连 ☑ 通知 ☐ 超时 [30s] 级别 [INFO]
├── 外观: 主题 [跟随系统] 动态色 ☐
├── 数据: [导出] [导入] [清除]
└── 关于: v1.0.0 / frp v0.62.x / leemwood
```

---

---

## 5. JNI 桥接层 — 前端←→frp 后端

### 5.1 整体架构

```
┌── KMP 共用层 ──────────────────────────────┐
│  expect class FrpController {               │
│    fun connect(host, port, token): Boolean  │
│    fun disconnect()                         │
│    fun startTunnel(config): Boolean         │
│    fun stopTunnel(id): Boolean              │
│    fun getStatus(id): TunnelStatus          │
│    fun reloadConfig(): Boolean              │
│  }                                          │
└───────────────┬─────────────────────────────┘
                │ (expect / actual 多态)
    ┌───────────┴────────────┐
    │  actual 实现            │
    ├────────────────────────┤
    │ Android: JNI → frp.so  │
    │ Desktop: JNI/JNA → .so │
    └───────────┬────────────┘
                │ (JNI bridge)
    ┌───────────┴────────────┐
    │  C API (cgo 导出)       │
    │  libfrp_bridge.so      │
    ├────────────────────────┤
    │  frp_start(conf)       │
    │  frp_stop()            │
    │  frp_status()          │
    │  frp_reload()          │
    │  frp_event_callback    │
    └───────────┬────────────┘
                │ (embed)
    ┌───────────┴────────────┐
    │  frp core              │
    │  (fatedier/frp)        │
    │  client/service.go     │
    └────────────────────────┘
```

### 5.2 Go 导出 C API (libfrp_bridge)

```c
// 导出的纯 C 接口 (不耦合 JNI，方便跨平台复用)

// 初始化 frp 客户端并连接服务器
// config_path: 配置文件路径
// callback: 事件回调函数指针 (JSON 字符串事件)
// 返回: 0=成功, 负值=错误码
int frp_start(const char* config_path, void (*callback)(const char* event_json));

// 停止 frp 客户端
void frp_stop();

// 获取当前状态 (JSON)
// 返回: JSON 字符串, 需调用 frp_free_string 释放
char* frp_get_status();

// 重载配置
int frp_reload(const char* config_path);

// 启动单个隧道 (热加载)
int frp_tunnel_start(const char* config_json);

// 停止单个隧道
int frp_tunnel_stop(const char* tunnel_id);

// 获取隧道状态
char* frp_tunnel_status(const char* tunnel_id);

// 释放 Go 分配的 C 字符串
void frp_free_string(char* str);

// 获取版本信息
char* frp_version();
```

### 5.3 Go 实现要点

```go
package main

/*
#include <stdlib.h>
*/
import "C"
import (
    "encoding/json"
    "unsafe"
    "github.com/fatedier/frp/client"
    "github.com/fatedier/frp/pkg/config"
    v1 "github.com/fatedier/frp/pkg/config/v1"
)

var (
    service  *client.Service
    callback func(string)
)

//export frp_start
func frp_start(configPath *C.char, cb unsafe.Pointer) C.int {
    path := C.GoString(configPath)
    callback = cb // 存储回调

    // 加载配置
    cfg, err := config.LoadFile(path)
    if err != nil {
        return -1
    }

    // 创建 service
    svr, err := client.NewService(cfg)
    if err != nil {
        return -2
    }
    service = svr

    // 启动 (goroutine)
    go func() {
        err := svr.Run(context.Background())
        if err != nil {
            notifyEvent("error", err.Error())
        }
    }()
    return 0
}

//export frp_stop
func frp_stop() {
    if service != nil {
        service.GracefulClose(5 * time.Second)
        service = nil
    }
}
```

### 5.4 Android JNI 实现

```kotlin
// androidMain/kotlin/com/frp/tunnel/platform/FrpController.android.kt

actual class FrpController actual constructor() {
    private var nativeLoaded = false

    init {
        try {
            System.loadLibrary("frp_bridge")
            nativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // fallback: 内置 frpc 二进制
        }
    }

    private external fun nativeStart(configPath: String): Int
    private external fun nativeStop()
    private external fun nativeGetStatus(): String?
    private external fun nativeTunnelStart(configJson: String): Int
    private external fun nativeTunnelStop(tunnelId: String): Int
    private external fun nativeVersion(): String

    // JNI 回调: Go → Java
    // 对应 Go 端的 callback 参数
    fun onEvent(eventJson: String) {
        // 解析事件 → 更新 AppStateHolder
        // 事件类型: "connected", "disconnected", "tunnel_online",
        //           "tunnel_offline", "tunnel_error", "traffic"
    }

    actual fun connect(serverAddr: String, serverPort: Int, token: String): Boolean {
        if (!nativeLoaded) return false
        val config = buildFrpConfig(serverAddr, serverPort, token)
        val configPath = writeConfigToFile(config)
        return nativeStart(configPath) == 0
    }

    actual fun disconnect() {
        if (nativeLoaded) nativeStop()
    }

    actual fun startTunnel(configJson: String): Boolean =
        nativeLoaded && nativeTunnelStart(configJson) == 0

    actual fun stopTunnel(tunnelId: String): Boolean =
        nativeLoaded && nativeTunnelStop(tunnelId) == 0

    actual fun getTunnelStatus(tunnelId: String): TunnelStatus {
        if (!nativeLoaded) return TunnelStatus.OFFLINE
        val status = nativeGetStatus() ?: return TunnelStatus.OFFLINE
        return parseStatus(status, tunnelId)
    }

    actual fun reloadConfig(): Boolean = true
}
```

### 5.5 Desktop JNI 实现

```kotlin
// desktopMain/kotlin/com/frp/tunnel/platform/FrpController.desktop.kt

actual class FrpController actual constructor() {
    // 同 Android 一样加载 .so
    // 或在未找到 .so 时回退到 CLI 模式

    private var process: Process? = null // CLI 回退
    private var nativeLoaded = false

    init {
        try {
            System.loadLibrary("frp_bridge")
            nativeLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            // fallback: CLI 模式
        }
    }

    actual fun connect(serverAddr: String, serverPort: Int, token: String): Boolean {
        if (nativeLoaded) {
            // JNI 模式 (同 Android)
            val config = buildFrpConfig(serverAddr, serverPort, token)
            return nativeStart(writeConfigToFile(config)) == 0
        } else {
            // CLI 回退: 启动 frpc 子进程
            val pb = ProcessBuilder(
                "frpc", "-c", writeConfigToFile(config),
            )
            pb.redirectErrorStream(true)
            process = pb.start()
            return true
        }
    }

    actual fun disconnect() {
        if (nativeLoaded) nativeStop()
        else process?.destroy()
    }
}
```

### 5.6 跨平台编译

#### Android NDK 交叉编译

```bash
# 设置 NDK 工具链
export NDK=$ANDROID_NDK_HOME
export CC=$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android21-clang

# 交叉编译 Go → Android .so
cd frp-bridge
CGO_ENABLED=1 \
GOOS=android \
GOARCH=arm64 \
CC=$CC \
go build -buildmode=c-shared \
  -o output/arm64-v8a/libfrp_bridge.so .

# 架构支持
# arm64 (v8a): 主流 Android 设备
# arm (v7a):  老旧设备
# x86_64:     模拟器
```

#### Desktop 编译

```bash
# Linux
CGO_ENABLED=1 go build -buildmode=c-shared \
  -o output/linux/libfrp_bridge.so .

# Windows (交叉编译)
CGO_ENABLED=1 \
GOOS=windows \
GOARCH=amd64 \
CC=x86_64-w64-mingw32-gcc \
go build -buildmode=c-shared \
  -o output/windows/frp_bridge.dll .
```

### 5.7 Go 依赖管理

```
frp-bridge/
├── go.mod              # module frp-bridge, 依赖 github.com/fatedier/frp
├── bridge.go           # cgo export 入口
├── api.go              # JNI 可调用函数实现
├── event.go            # 事件回调管理
├── config.go           # 配置生成和写入
└── Makefile            # 跨平台编译脚本
    ├── make android    # 编译所有 Android 架构
    ├── make linux      # 编译 Linux .so
    └── make windows    # 编译 Windows .dll
```

### 5.8 Android 集成

```
composeApp/
└── src/androidMain/
    └── jniLibs/                 # JNI 库目录
        ├── arm64-v8a/
        │   └── libfrp_bridge.so
        ├── armeabi-v7a/
        │   └── libfrp_bridge.so
        └── x86_64/
            └── libfrp_bridge.so
```

Gradle 配置：
```kotlin
android {
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/androidMain/jniLibs")
        }
    }
}
```

### 5.9 事件协议

Go → Java 的回调事件格式 (JSON)：

```json
// 连接状态
{"type":"connection","status":"connected","server":"frp.example.com:7000"}
{"type":"connection","status":"disconnected","reason":"timeout"}

// 隧道状态
{"type":"tunnel","id":"mc-server","status":"online","local":"127.0.0.1:25565","remote":":25565"}
{"type":"tunnel","id":"mc-server","status":"offline","reason":"port_closed"}
{"type":"tunnel","id":"mc-server","status":"error","error":"dial tcp 127.0.0.1:25565: connect: connection refused"}

// 流量数据
{"type":"traffic","id":"mc-server","up":1024,"down":4096,"timestamp":1719812345}

// 日志
{"type":"log","level":"INFO","message":"proxy start success","time":"12:34:56"}
```

### 5.10 错误码

| 错误码 | 常量名 | 含义 |
|:------|:-------|:-----|
| 0 | SUCCESS | 成功 |
| -1 | ERR_CONFIG | 配置加载失败 |
| -2 | ERR_CREATE_SERVICE | 创建 frp service 失败 |
| -3 | ERR_CONNECT | 连接服务端失败 |
| -4 | ERR_AUTH | 认证失败 |
| -5 | ERR_RUN | 运行异常 |
| -6 | ERR_TUNNEL_START | 隧道启动失败 |
| -7 | ERR_TUNNEL_STOP | 隧道停止失败 |
| -8 | ERR_INVALID_PARAM | 参数错误 |
| -9 | ERR_NOT_INITIALIZED | 未初始化 |

---

## 6. 组件库

### 5.1 完整清单 (17个)

| 组件 | 用途 | 状态变体 |
|:----|:-----|:---------|
| TunnelCard | 隧道卡片 | 在线/离线/连接中/错误/多选 |
| StatusBadge | 10dp 状态圆点 | 绿/灰/橙(脉冲)/红 |
| TypeChip | 协议标签 | TCP/HTTP/HTTPS/STCP/XTCP/UDP |
| TrafficRow | 流量行 | 上行蓝 + 下行绿 |
| LogEntryRow | 日志行 | 4 级别着色 |
| EmptyState | 空状态 | 无隧道/搜索无结果/无日志 |
| ErrorBanner | 错误横幅 | 可关闭 + 操作按钮 |
| ConfirmDialog | 确认弹窗 | 删除/放弃/清除 |
| SettingsRow | 设置行 | 文本/开关/下拉/按钮 |
| TrafficChart | 折线图 Canvas | 5min/1h/今日/本周 |
| SearchBar | 搜索栏 | 空/输入中/有结果 |
| FilterChips | 过滤芯片 | 选中/未选/计数 |
| SkeletonLoader | 骨架屏 | shimmer 动画 |
| Snackbar | 底部提示 | 信息/成功/错误 + 操作 |
| AddressLine | 地址文字 | — |
| ErrorLine | 错误文字 | 红色 + 图标 |
| SectionHeader | 段落标题 | — |

### 5.2 TunnelCard 规格

```
尺寸: FillMaxWidth × 80dp
内边距: 12dp/16dp
圆角: 12dp | 阴影: 1dp (在线) / 0dp (离线)
边框: 在线=绿左边框 2dp, 错误=红左边框 2dp
背景: surface / surfaceVariant / errorContainer
```

### 5.3 Dialog 规格

```
标题: headlineSmall | 内容: bodyMedium
按钮: TextButton (取消) + FilledTonalButton (确认, 红色)
动效: scale 0.9→1.0 + fadeIn 250ms
遮罩: scrim 40% 黑
```

---

## 7. 交互流程

### 6.1 核心流程

**新增:** FAB → 填写 → 保存 → 列表 + Snackbar
**开关:** Switch → 连接动画 → 成功(绿)/失败(红+回OFF)
**删除:** 左滑 → 确认 → 消失 + Snackbar [撤销]
**连接:** 设置 → 保存 → 自动连 → 成功(🟢)/失败(🔴+重连)

### 6.2 异常流程

| 场景 | 表现 |
|:----|:-----|
| 连接超时 | Banner + 重连计数 |
| 令牌失效 | Dialog "认证失败" |
| 端口冲突 | Dialog "端口已被占用" |
| 断网 | Banner + 全部离线 |
| frp 崩溃 | Banner + 自动重启 |
| 配置损坏 | Dialog "恢复默认？" |

### 6.3 后台行为

| 场景 | 行为 |
|:----|:-----|
| App 切后台 | 隧道继续运行 |
| Android 锁屏 | 通知栏: 在线数+流量 |
| 桌面最小化 | 系统托盘 + 右键菜单 |
| 进程被杀 | 开机自启 (若开启) |
| 网络切换 | 自动检测重连 |

---

## 8. 状态与异常处理

### 7.1 状态机

```
IDLE → LOADING → SUCCESS
  │                │
  └────→ ERROR ←───┘
```

### 7.2 空状态全集

| 页面 | 场景 | 插画 | 操作 |
|:----|:-----|:-----|:-----|
| 首页 | 无隧道 | 📡 | FAB 引导 |
| 首页 | 搜索无结果 | 🔍 | [清除过滤] |
| 日志 | 无日志 | 📝 | — |
| 状态 | 未连接 | 🔌 | 跳转设置 |
| 状态 | 无流量 | 📊 | — |

### 7.3 错误矩阵

| 错误 | 提示 | 自动恢复 |
|:----|:-----|:---------|
| 网络不可用 | Banner | ✓ |
| 连接超时 | Banner+重连计数 | ✓ (可配) |
| 认证失败 | Dialog | ✗ |
| 端口冲突 | Dialog | ✗ |
| frp 崩溃 | Banner | ✓ |
| 配置损坏 | Dialog | ✗ |

---

## 9. 动效与过渡

### 8.1 页面过渡

| 场景 | 动效 | 时长 |
|:----|:-----|:-----|
| Tab 切换 | FadeThrough | 300ms |
| 编辑页进入 | SlideIn 右→左 | 350ms |
| 返回 | SlideOut 左→右 | 300ms |
| Dialog | Scale 0.9→1 + Fade | 250ms |
| Snackbar | SlideIn 下→上 | 300ms |

### 8.2 微交互 (11个)

| 元素 | 动效 | 触发 |
|:----|:-----|:-----|
| Switch | 滑块+背景色渐变 300ms | 点击 |
| StatusBadge | 脉冲 0.8↔1.2 1.5s 循环 | CONNECTING |
| TunnelCard | 背景色渐变 400ms | 状态变更 |
| 新建隧道 | 卡片从底弹入 350ms | 添加 |
| 删除隧道 | 左滑淡出 300ms | 确认 |
| 流量数字 | 滚动变化 | 更新 |
| 状态灯 | 颜色过渡 300ms | 连接变化 |
| FAB | 旋转展开 | 滚动 |
| 长按多选 | 抬升 100ms + haptic | 长按 |
| 保存按钮 | 转圈 loading→✓ | 保存过程 |
| 测试连接 | 按钮转圈 | 测试中 |
# Frp KMP — 前端 UI 设计 v2 (第9~13章+附录)

---

## 10. 响应式布局

### 9.1 断点

| 断点 | 宽度 | 设备 | 导航 | 列数 |
|:----|:-----|:-----|:-----|:-----|
| Compact | < 600dp | 手机竖屏 | BottomNav | 1列 |
| Medium | 600-840dp | 小平板/横屏 | BottomNav+可展开 | 2列网格 |
| Expanded | ≥ 840dp | 平板/桌面 | NavRail | 全宽+右面板 |

### 9.2 自适应网格

```
Compact           Medium              Expanded
┌────────┐   ┌──────┬──────┐   ┌────┬────────────────┐
│Card    │   │Card  │Card  │   │Rail│Card            │
├────────┤   ├──────┼──────┤   │    ├────────────────┤
│Card    │   │Card  │Card  │   │    │Card            │
└────────┘   └──────┴──────┘   └────┴────────────────┘
  1列           2列               Nav + 全宽内容
```

### 9.3 组件自适应

| 组件 | Compact | Expanded |
|:----|:--------|:---------|
| TunnelCard | 全宽 | 全宽+更多信息列 |
| 编辑页 | 全屏推入 | 右侧面板 480dp |
| 日志 | 全屏 | 分屏(日志+详情) |
| 状态 | 上下排列 | 左右分栏 |
| Dialog | 全宽+边距 | 居中 480dp |

---

## 11. 主题系统

### 10.1 亮色主题

| Token | 色值 | 用途 |
|:------|:-----|:------|
| primary | #1565C0 | FAB、按钮、选中态 |
| onPrimary | #FFFFFF | 主色上文字 |
| primaryContainer | #D1E4FF | 标签、选中背景 |
| secondary | #00897B | 在线状态、成功 |
| error | #E53935 | 错误、删除 |
| background | #F8F9FE | 页面背景 |
| surface | #FFFFFF | 卡片 |
| surfaceVariant | #F0F2F8 | 离线卡片 |
| outline | #B0B7C4 | 边框 |

### 10.2 暗色主题

| Token | 色值 | 用途 |
|:------|:-----|:------|
| primary | #64B5F6 | 主色 |
| primaryContainer | #00497D | 标签背景 |
| secondary | #4DB6AC | 在线 |
| error | #EF5350 | 错误 |
| background | #111318 | 深色背景 |
| surface | #1A1C23 | 卡片 |
| surfaceVariant | #262931 | 离线卡片 |

### 10.3 语义色

| 用途 | 色值 |
|:-----|:-----|
| ONLINE | #4CAF50 |
| OFFLINE | #9E9E9E |
| CONNECTING | #FFA726 |
| ERROR | #E53935 |
| 上行流量 | #42A5F5 |
| 下行流量 | #66BB6A |

### 10.4 字体

| 样式 | 字重 | 字号 | 用途 |
|:----|:-----|:-----|:-----|
| headlineLarge | Bold | 28sp | 页面大标题 |
| headlineMedium | SemiBold | 22sp | 页面标题 |
| titleLarge | SemiBold | 18sp | 卡片标题 |
| titleMedium | Medium | 15sp | 列表项标题 |
| bodyMedium | Normal | 14sp | 正文 |
| bodySmall | Normal | 12sp | 辅助信息 |
| labelSmall | Normal/Mono | 11sp | 标签/日志 |

### 10.5 圆角

| Token | 值 | 用途 |
|:------|:---|:------|
| cardRadius | 12dp | 卡片 |
| chipRadius | 4dp | 标签 |
| dialogRadius | 16dp | 弹窗 |
| buttonRadius | 20dp | 按钮 |

### 10.6 主题切换

- 跟随系统 (默认): `isSystemInDarkTheme()`
- 手动: 设置页开关
- 动态取色: Android 12+ Monet (可选)
- 切换动画: 平滑过渡

---

## 12. 平台适配

### 11.1 Android 特有

| 特性 | 实现 |
|:----|:-----|
| 前台服务 | tunnel 运行时 systemService |
| 通知栏 | 常驻: 在线数+流量+快捷开关 |
| 动态取色 | Android 12+ Monet |
| 边到边 | enableEdgeToEdge() |
| 后退手势 | BackHandler |
| 快捷设置 Tile | 快速开关 (可选) |
| Widget | 桌面状态概览 (可选) |

### 11.2 Desktop 特有

| 特性 | 实现 |
|:----|:-----|
| 窗口管理 | 最小化到系统托盘 |
| 托盘菜单 | 右键: 显示/隐藏/开关/退出 |
| 多窗口 | 支持分离多个页面 |
| CLI 模式 | `--headless` 无 UI 运行 |
| 配置路径 | `~/.config/frp-kmp/config.json` |
| 桌面通知 | OS 原生通知 |
| CLI 命令 | `frp-kmp start/stop/status` |

### 11.3 差异对照

| 特性 | Android | Desktop |
|:----|:--------|:--------|
| 导航 | BottomNav | NavRail |
| 编辑页 | 全屏 | 侧面板 |
| 后台 | Foreground Service | System Tray |
| 配置存储 | app internal | ~/.config/ |
| 更新 | Google Play / APK | apt/msi/manual |

---

## 13. 无障碍 (8项)

| 要求 | 实现 |
|:----|:------|
| 内容描述 | 所有 Icon/Image 提供 contentDescription |
| 标签关联 | 表单字段用 label + placeholder |
| 触摸目标 | ≥ 48dp |
| 对比度 | 文字/背景 ≥ 4.5:1 |
| 焦点顺序 | 表单按逻辑 Tab 顺序 |
| 状态播报 | 连接状态变化触发 AccessibilityEvent |
| 键盘操作 | Tab/Enter/Space (Desktop) |
| 滚动 | 键盘上下翻页 (Desktop) |

---

## 14. 文件结构

```
composeApp/src/
├── commonMain/kotlin/com/frp/tunnel/
│   ├── App.kt                   # 入口 + 导航状态机
│   ├── theme/
│   │   ├── Theme.kt, Color.kt, Type.kt, Shape.kt, Dimen.kt
│   ├── navigation/
│   │   └── Navigation.kt        # Screen + NavTarget
│   ├── model/
│   │   ├── TunnelUiState.kt, AppState.kt, LogEntry.kt
│   ├── platform/
│   │   └── FrpController.kt     # expect 声明
│   ├── ui/
│   │   ├── TunnelListScreen.kt / TunnelEditorScreen.kt
│   │   ├── StatusScreen.kt / LogScreen.kt / SettingsScreen.kt
│   ├── components/
│   │   ├── TunnelCard.kt / StatusBadge.kt / TypeChip.kt
│   │   ├── TrafficRow.kt / LogEntryRow.kt / EmptyState.kt
│   │   ├── ErrorBanner.kt / ConfirmDialog.kt
│   │   ├── SkeletonLoader.kt / SearchBar.kt / FilterChips.kt
│   │   └── SettingsRow.kt / TrafficChart.kt / AddressLine.kt
│   └── util/
│       └── FormatUtil.kt / Validator.kt
│
├── androidMain/.../
│   ├── MainActivity.kt / FrpService.kt
│   └── platform/FrpController.android.kt
│
└── desktopMain/.../
    ├── Main.kt / SystemTray.kt / CliMode.kt
    └── platform/FrpController.desktop.kt
```

---

## 附录 A: 交互流程总图

```
[启动 App]
  ├── 首次 → 引导页 (1.介绍 / 2.配置 / 3.添加)
  └── 非首次
      ├── 加载配置 → 自动连接
      │   ├── 成功 → 恢复状态 → 🏠 首页
      │   └── 失败 → Banner + ⚙️ 引导
      └── 🏠 首页
          ├── 浏览/开关/点击/左滑/长按/搜索/FAB
          ├── 点击 → 📝 编辑 → 保存/放弃
          ├── 左滑 → 🗑️ 确认删除 → 撤销
          └── 长按 → ☑ 多选 → 批量开关/删除
```

## 附录 B: 组件状态矩阵

```
TunnelCard:
  在线 🟢 + 流量 + 可开关
  离线 ⚪ + 无流量 + 可开关
  连接中 🟡 + 脉冲 + 不可操作
  错误 🔴 + 错误信息 + 可重试
  多选 ☑ + checkbox

StatusBadge:
  纯色 (在线/离线/错误) / 脉冲 (连接中)

SearchBar:
  空 / 输入中 (有清除) / 有结果 / 无结果

EmptyState:
  无隧道 📡 / 无结果 🔍 / 无日志 📝
  每个含: 主文案 + 副文案 + (可选操作按钮)

ErrorBanner:
  展开: 错误信息 + 重连计数 + [重试][设置]
  收起: 仅红色条纹
```

---

> v2.0 | 2026-07-01 | leemwood
