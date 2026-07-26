#!/bin/sh
# 交叉编译 Android 版 frpc JNI 共享库（Go -buildmode=c-shared），
# 输出为 composeApp/src/androidMain/jniLibs/arm64-v8a/libfrpc_jni.so
# 用法: sh scripts/build-frpc-android.sh
# 可选环境变量: FRP_VERSION（默认 v0.70.1）
# NDK 定位: 依次尝试 $ANDROID_NDK_HOME、$ANDROID_HOME/ndk/*、$ANDROID_SDK_ROOT/ndk/*（取版本最大者）
set -e

FRP_VERSION="${FRP_VERSION:-v0.70.1}"

# 用脚本所在路径推导项目根，不依赖 cwd
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(dirname -- "$SCRIPT_DIR")
OUTPUT_DIR="$PROJECT_ROOT/composeApp/src/androidMain/jniLibs"

# 前置检查：go 命令必须存在
if ! command -v go >/dev/null 2>&1; then
    echo "error: 未找到 go 命令，请先安装 Go（https://go.dev/doc/install）" >&2
    exit 1
fi

# 定位 Android NDK 的 aarch64 clang（c-shared 必须 CGO 外部链接）
# minSdk 24 → 使用 aarch64-linux-android24-clang
detect_ndk_clang() {
    ndk_dirs=""
    if [ -n "${ANDROID_NDK_HOME:-}" ] && [ -d "${ANDROID_NDK_HOME:-}" ]; then
        ndk_dirs="$ANDROID_NDK_HOME"
    fi
    for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}"; do
        if [ -n "$sdk" ] && [ -d "$sdk/ndk" ]; then
            for d in "$sdk"/ndk/*; do
                [ -d "$d" ] && ndk_dirs="$ndk_dirs $d"
            done
        fi
    done
    if [ -z "${ndk_dirs# }" ]; then
        echo "error: 未找到 Android NDK，请安装 NDK 并设置 ANDROID_NDK_HOME 或 ANDROID_HOME" >&2
        exit 1
    fi
    # 版本号最大者优先
    NDK_HOME=$(printf '%s\n' $ndk_dirs | sort -uV | tail -n 1)
    # prebuilt 目录名随宿主平台变化（linux-x86_64 / darwin-* / windows-x86_64）
    NDK_CLANG=""
    for c in "$NDK_HOME"/toolchains/llvm/prebuilt/*/bin/aarch64-linux-android24-clang; do
        if [ -x "$c" ]; then
            NDK_CLANG="$c"
            break
        fi
    done
    if [ -z "$NDK_CLANG" ]; then
        echo "error: 在 NDK $NDK_HOME 下未找到 aarch64-linux-android24-clang" >&2
        exit 1
    fi
}

detect_ndk_clang

echo "==> Go 版本: $(go version)"
echo "==> frp 版本: $FRP_VERSION"
echo "==> NDK: $NDK_HOME"
echo "==> CC: $NDK_CLANG"

# 克隆 frp 源码到临时目录
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

echo "==> 克隆 frp 源码..."
git clone --depth 1 --branch "$FRP_VERSION" https://github.com/fatedier/frp "$TMP_DIR/frp"

cd "$TMP_DIR/frp"

# frpc 的 web dashboard 静态文件不在 git 仓库里（release 流程中由 npm 构建），
# web/frpc/embed.go 的 go:embed 找不到 dist 会编译失败。
# 本 App 只用 admin API（/api/status），不需要 dashboard 页面，放空占位文件即可。
mkdir -p web/frpc/dist
echo '<html><body>frpc dashboard not bundled</body></html>' > web/frpc/dist/index.html

# 写入 JNI wrapper（package main，-buildmode=c-shared 编译为共享库）
# 对应 Kotlin 侧 object cn.lemwood.platform.FrpcNative 的 native 方法
mkdir -p android

# jstring ↔ char* 转换 helper 单独一个文件：
# 含 //export 的文件里 C preamble 只允许声明不允许定义（会被复制到多个生成文件导致重复符号），
# 所以 C 函数定义放在本文件，Go 包装函数供 frpc_jni.go 调用。
cat > android/jnihelper.go <<'EOF'
package main

/*
#include <jni.h>
#include <stdlib.h>
#include <string.h>

static char* frpJstringToC(JNIEnv* env, jstring s) {
    if (s == NULL) return NULL;
    const char* str = (*env)->GetStringUTFChars(env, s, NULL);
    if (str == NULL) return NULL;
    char* out = strdup(str);
    (*env)->ReleaseStringUTFChars(env, s, str);
    return out;
}
*/
import "C"

import "unsafe"

// jstringToGo 把 JNI jstring 转成 Go string
func jstringToGo(env *C.JNIEnv, s C.jstring) string {
	cs := C.frpJstringToC(env, s)
	if cs == nil {
		return ""
	}
	defer C.free(unsafe.Pointer(cs))
	return C.GoString(cs)
}
EOF

cat > android/frpc_jni.go <<'EOF'
package main

/*
#include <jni.h>
*/
import "C"

import (
	"context"
	"sync"

	"github.com/fatedier/frp/client"
	"github.com/fatedier/frp/pkg/config"
	"github.com/fatedier/frp/pkg/config/source"
	"github.com/fatedier/frp/pkg/config/v1/validation"
	"github.com/fatedier/frp/pkg/policy/security"
	"github.com/fatedier/frp/pkg/util/log"
)

// 全局 service 句柄，mu 保护；frpc 在 App 进程内单实例运行
var (
	mu      sync.Mutex
	service *client.Service
	cancel  context.CancelFunc
)

// stopLocked 停止当前实例，幂等；调用前必须持有 mu
func stopLocked() {
	// 取消父 context 即可停止 service（Run 内部 ctx 派生自它），
	// 不直接调 GracefulClose，避免 Run 尚未执行时 svr.cancel 为 nil 的竞态
	if cancel != nil {
		cancel()
		cancel = nil
	}
	service = nil
}

//export Java_cn_lemwood_platform_FrpcNative_nativeStart
func Java_cn_lemwood_platform_FrpcNative_nativeStart(env *C.JNIEnv, clazz C.jclass, configPath C.jstring) C.jint {
	path := jstringToGo(env, configPath)
	if path == "" {
		return 1
	}

	mu.Lock()
	defer mu.Unlock()

	// 重复调用先停掉旧实例
	stopLocked()

	// 加载配置（严格模式，与 frpc CLI 默认一致）
	result, err := config.LoadClientConfigResult(path, true)
	if err != nil {
		return 2
	}

	unsafeFeatures := security.NewUnsafeFeatures(nil)

	configSource := source.NewConfigSource()
	if err := configSource.ReplaceAll(result.Proxies, result.Visitors); err != nil {
		return 3
	}
	aggregator := source.NewAggregator(configSource)

	// 提前校验配置，错误时返回非 0 而不是进 goroutine 后才失败
	proxyCfgs, visitorCfgs, err := aggregator.Load()
	if err != nil {
		return 4
	}
	proxyCfgs, visitorCfgs = config.FilterClientConfigurers(result.Common, proxyCfgs, visitorCfgs)
	proxyCfgs = config.CompleteProxyConfigurers(proxyCfgs)
	visitorCfgs = config.CompleteVisitorConfigurers(visitorCfgs)
	if _, err := validation.ValidateAllClientConfig(result.Common, proxyCfgs, visitorCfgs, unsafeFeatures); err != nil {
		return 5
	}

	// 日志走 ini [common] 的 log.to（控制器已指向 filesDir/frp/frpc.log）
	log.InitLogger(result.Common.Log.To, result.Common.Log.Level, int(result.Common.Log.MaxDays), result.Common.Log.DisablePrintColor)

	svr, err := client.NewService(client.ServiceOptions{
		Common:                 result.Common,
		ConfigSourceAggregator: aggregator,
		UnsafeFeatures:         unsafeFeatures,
		ConfigFilePath:         path,
	})
	if err != nil {
		return 6
	}

	ctx, cancelFn := context.WithCancel(context.Background())
	service = svr
	cancel = cancelFn

	go func() {
		if err := svr.Run(ctx); err != nil {
			log.Warnf("frpc service 退出: %v", err)
		}
	}()
	return 0
}

//export Java_cn_lemwood_platform_FrpcNative_nativeStop
func Java_cn_lemwood_platform_FrpcNative_nativeStop(env *C.JNIEnv, clazz C.jclass) {
	mu.Lock()
	defer mu.Unlock()
	stopLocked()
}

func main() {}
EOF

OUT="$OUTPUT_DIR/arm64-v8a/libfrpc_jni.so"
mkdir -p "$(dirname -- "$OUT")"

# 注：Go 的 android 目标仅 arm64 生态完整且真机主流，故只打 arm64-v8a
echo "==> 构建 arm64-v8a c-shared 共享库..."
CGO_ENABLED=1 GOOS=android GOARCH=arm64 CC="$NDK_CLANG" \
    go build -buildmode=c-shared -ldflags "-s -w" -o "$OUT" ./android

echo "==> 构建完成，产物大小:"
if [ -f "$OUT" ]; then
    size=$(du -h "$OUT" | cut -f1)
    echo "    arm64-v8a/libfrpc_jni.so: $size"
else
    echo "error: 缺少产物 $OUT" >&2
    exit 1
fi
