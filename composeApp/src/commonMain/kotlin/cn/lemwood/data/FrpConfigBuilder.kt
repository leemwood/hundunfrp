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
        sb.appendLine()

        tunnels.filter { it.enabled }.forEach { tunnel ->
            val section = when (tunnel.type) {
                TunnelType.TCP -> "tcp"
                TunnelType.UDP -> "udp"
                TunnelType.HTTP -> "http"
                TunnelType.HTTPS -> "https"
                TunnelType.STCP -> "stcp"
                TunnelType.XTCP -> "xtcp"
            }
            sb.appendLine("[$section]")
            sb.appendLine("name = ${tunnel.id}")
            sb.appendLine("type = ${tunnel.type.name.lowercase()}")
            sb.appendLine("local_ip = ${tunnel.localAddr}")
            sb.appendLine("local_port = ${tunnel.localPort}")
            sb.appendLine("remote_port = ${tunnel.remotePort}")

            if (tunnel.encryption) sb.appendLine("use_encryption = true")
            if (tunnel.compression) sb.appendLine("use_compression = true")
            if (tunnel.tls) sb.appendLine("tls_enable = true")
            tunnel.customDomain?.let { sb.appendLine("custom_domains = $it") }
            tunnel.httpUser?.let { sb.appendLine("http_user = $it") }
            tunnel.httpPassword?.let { sb.appendLine("http_pwd = $it") }
            sb.appendLine()
        }

        return sb.toString()
    }
}
