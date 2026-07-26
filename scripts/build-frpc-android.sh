#!/bin/sh
# 交叉编译 Android 各 ABI 的 frpc 二进制，输出为 jniLibs/<abi>/libfrpc.so
# 用法: sh scripts/build-frpc-android.sh
# 可选环境变量: FRP_VERSION（默认 v0.70.1）
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

echo "==> Go 版本: $(go version)"
echo "==> frp 版本: $FRP_VERSION"

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

# ABI:GOARCH 列表，armeabi-v7a 需要额外 GOARM=7
build_frpc() {
    abi="$1"
    goarch="$2"
    goarm="$3"

    out="$OUTPUT_DIR/$abi/libfrpc.so"
    mkdir -p "$(dirname -- "$out")"

    echo "==> 构建 $abi (GOARCH=$goarch${goarm:+ GOARM=$goarm})..."
    if [ -n "$goarm" ]; then
        CGO_ENABLED=0 GOOS=android GOARCH="$goarch" GOARM="$goarm" \
            go build -ldflags "-s -w" -o "$out" ./cmd/frpc
    else
        CGO_ENABLED=0 GOOS=android GOARCH="$goarch" \
            go build -ldflags "-s -w" -o "$out" ./cmd/frpc
    fi
}

# 注：android/arm（armeabi-v7a）Go 要求外部 cgo 链接（需 NDK），纯 Go 无法编译，故只打 64 位 ABI
build_frpc arm64-v8a   arm64 ""
build_frpc x86_64      amd64 ""

echo "==> 构建完成，产物大小:"
for abi in arm64-v8a x86_64; do
    f="$OUTPUT_DIR/$abi/libfrpc.so"
    if [ -f "$f" ]; then
        size=$(du -h "$f" | cut -f1)
        echo "    $abi/libfrpc.so: $size"
    else
        echo "error: 缺少产物 $f" >&2
        exit 1
    fi
done
