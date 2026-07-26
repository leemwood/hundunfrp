package cn.lemwood.data

import cn.lemwood.model.AppSettings
import cn.lemwood.model.TunnelType
import cn.lemwood.model.TunnelUiState

object FrpConfigBuilder {

    fun buildConfig(settings: AppSettings, tunnels: List<TunnelUiState>): String {
        val sb = StringBuilder()
        sb.appendLine("[common]")
        sb.appendLine("server_addr = ${settings.serverAddr}")
        sb.appendLine("server_port = ${settings.serverPort}")
        if (settings.serverToken.isNotBlank()) {
            sb.appendLine("token = ${settings.serverToken}")
        }
        if (settings.timeoutSeconds > 0) {
            sb.appendLine("login_fail_exit = false")
        }
        sb.appendLine("admin_addr = 127.0.0.1")
        sb.appendLine("admin_port = 7400")
        sb.appendLine("admin_user = admin")
        sb.appendLine("admin_pwd = admin")
        sb.appendLine()

        tunnels.filter { it.enabled }.forEach { tunnel ->
            val isHttp = tunnel.type == TunnelType.HTTP || tunnel.type == TunnelType.HTTPS
            // http/https 隧道没有 custom_domains 无法生效，整条跳过不生成
            if (isHttp && tunnel.customDomain.isNullOrBlank()) return@forEach

            // frpc ini 以段名作为代理名，段名 = tunnel.id，供日志/admin API 回传时匹配
            sb.appendLine("[${tunnel.id}]")
            sb.appendLine("type = ${tunnel.type.name.lowercase()}")
            sb.appendLine("local_ip = ${tunnel.localAddr}")
            sb.appendLine("local_port = ${tunnel.localPort}")
            if (isHttp) {
                sb.appendLine("custom_domains = ${tunnel.customDomain}")
            } else {
                sb.appendLine("remote_port = ${tunnel.remotePort}")
            }

            if (tunnel.encryption) sb.appendLine("use_encryption = true")
            if (tunnel.compression) sb.appendLine("use_compression = true")
            if (tunnel.tls) sb.appendLine("tls_enable = true")
            tunnel.httpUser?.let { sb.appendLine("http_user = $it") }
            tunnel.httpPassword?.let { sb.appendLine("http_pwd = $it") }
            sb.appendLine()
        }

        return sb.toString()
    }
}
