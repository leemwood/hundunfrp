package cn.lemwood.platform

import android.util.Log

/**
 * frpc JNI 绑定：frpc 以 Go -buildmode=c-shared 编译为 libfrpc_jni.so，
 * 在 App 进程内运行（targetSdk 29+ 禁止 exec filesDir 下的二进制，无法再走子进程方案）。
 * 仅 arm64-v8a 打包该库，其他 ABI 设备加载失败时 available=false。
 */
object FrpcNative {

    /** libfrpc_jni 是否加载成功，控制器启动前必须检查 */
    val available: Boolean

    init {
        available = runCatching {
            System.loadLibrary("frpc_jni")
        }.onFailure {
            Log.w(TAG, "libfrpc_jni 加载失败: ${it.message}")
        }.isSuccess
    }

    /**
     * 启动 frpc（进程内运行）
     * @param configPath frpc ini 配置文件绝对路径
     * @return 0 表示成功，非 0 为失败
     */
    external fun nativeStart(configPath: String): Int

    /** 停止 frpc */
    external fun nativeStop()

    private const val TAG = "FrpcNative"
}
