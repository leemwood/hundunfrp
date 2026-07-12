package cn.lemwood.platform

import android.content.Context
import cn.lemwood.data.FrpConfigBuilder
import cn.lemwood.model.TunnelStatus
import cn.lemwood.state.AppStateHolder
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.Socket

actual class FrpController {
    private var process: java.lang.Process? = null

    private val context: android.content.Context?
        get() = AndroidFrpContext.appContext

    actual fun connect(host: String, port: Int, token: String): Boolean {
        val ctx = context
        if (ctx == null) {
            AppStateHolder.addLog(
                cn.lemwood.model.LogEntry(
                    level = cn.lemwood.model.LogLevel.ERROR,
                    message = "FrpController 未初始化，缺少 Context",
                    timestamp = System.currentTimeMillis(),
                )
            )
            return false
        }

        return try {
            disconnect()

            val state = AppStateHolder.state.value
            val config = FrpConfigBuilder.buildConfig(state.settings, state.tunnels)
            val configDir = File(ctx.filesDir, "frp")
            configDir.mkdirs()
            val configFile = File(configDir, "frpc.ini")
            configFile.writeText(config)

            val frpcBinary = extractFrpcBinary(ctx) ?: run {
                AppStateHolder.addLog(
                    cn.lemwood.model.LogEntry(
                        level = cn.lemwood.model.LogLevel.ERROR,
                        message = "未找到 frpc 二进制文件，请将对应架构的 frpc 放入 jniLibs/",
                        timestamp = System.currentTimeMillis(),
                    )
                )
                return false
            }
            frpcBinary.setExecutable(true)

            val pb = ProcessBuilder(frpcBinary.absolutePath, "-c", configFile.absolutePath)
            pb.directory(configDir)
            pb.redirectErrorStream(true)
            val p = pb.start()
            process = p

            Thread {
                p.inputStream.bufferedReader().use { reader ->
                    reader.lines().forEach { line ->
                        AppStateHolder.addLog(
                            cn.lemwood.model.LogEntry(
                                level = cn.lemwood.model.LogLevel.INFO,
                                message = line,
                                timestamp = System.currentTimeMillis(),
                            )
                        )
                    }
                }
                p.waitFor()
            }.start()

            AppStateHolder.addLog(
                cn.lemwood.model.LogEntry(
                    level = cn.lemwood.model.LogLevel.INFO,
                    message = "frpc 启动中...",
                    timestamp = System.currentTimeMillis(),
                )
            )
            true
        } catch (e: Exception) {
            AppStateHolder.addLog(
                cn.lemwood.model.LogEntry(
                    level = cn.lemwood.model.LogLevel.ERROR,
                    message = "启动 frpc 失败: ${e.message}",
                    timestamp = System.currentTimeMillis(),
                )
            )
            false
        }
    }

    actual fun disconnect() {
        try {
            process?.destroy()
            process = null
        } catch (_: Exception) {}
    }

    actual fun startTunnel(configJson: String): Boolean = true

    actual fun stopTunnel(tunnelId: String): Boolean {
        disconnect()
        return true
    }

    actual fun getTunnelStatus(tunnelId: String): TunnelStatus {
        return if (isProcessAlive()) TunnelStatus.CONNECTING else TunnelStatus.OFFLINE
    }

    private fun isProcessAlive(): Boolean {
        val p = process ?: return false
        return try {
            p.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    actual fun reloadConfig(): Boolean {
        val ctx = context ?: return false
        val state = AppStateHolder.state.value
        return connect(
            host = state.settings.serverAddr,
            port = state.settings.serverPort,
            token = state.settings.serverToken
        )
    }

    actual fun testConnection(host: String, port: Int, timeoutSeconds: Int): String? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), timeoutSeconds * 1000)
            socket.close()
            null
        } catch (e: Exception) {
            e.message ?: "连接失败"
        }
    }

    private fun extractFrpcBinary(ctx: Context): File? {
        val arch = android.os.Build.SUPPORTED_ABIS.firstOrNull()?.takeIf {
            it in listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        } ?: return null

        val binaryFile = File(ctx.filesDir, "frp/frpc-$arch")
        if (binaryFile.exists() && binaryFile.canExecute()) {
            return binaryFile
        }

        try {
            val libPath = "jniLibs/$arch/"
            val libNames = listOf("libfrpc.so", "frpc")

            for (libName in libNames) {
                try {
                    val inputStream = ctx.assets.open("$libPath$libName")
                    inputStream.use { input ->
                        FileOutputStream(binaryFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    binaryFile.setExecutable(true)
                    return binaryFile
                } catch (_: Exception) {}
            }

            try {
                val libDir = File(ctx.applicationInfo.nativeLibraryDir, "../$arch/")
                for (libName in libNames) {
                    val libFile = File(libDir, libName)
                    if (libFile.exists()) {
                        libFile.copyTo(binaryFile, overwrite = true)
                        binaryFile.setExecutable(true)
                        return binaryFile
                    }
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        return null
    }
}
