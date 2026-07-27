package cn.lemwood.platform

/**
 * frpc stdout 日志事件
 */
sealed interface FrpLogEvent {
    data class ProxyStarted(val name: String) : FrpLogEvent
    data class ProxyStartError(val name: String, val message: String) : FrpLogEvent
    data class ProxyClosed(val name: String) : FrpLogEvent
    data object LoginSuccess : FrpLogEvent
    data class LoginFailed(val message: String) : FrpLogEvent
    data class Plain(val line: String) : FrpLogEvent
}

/**
 * frpc 日志行解析器，按关键字匹配（忽略大小写）
 */
object FrpLogParser {

    private val proxyNameRegex = Regex("""\[([A-Za-z0-9_\-]+)\]""")

    fun parse(line: String): FrpLogEvent {
        val lower = line.lowercase()
        return when {
            lower.contains("login to server success") -> FrpLogEvent.LoginSuccess
            lower.contains("login") && (lower.contains("failed") || lower.contains("error")) ->
                FrpLogEvent.LoginFailed(line)
            lower.contains("start proxy success") ->
                FrpLogEvent.ProxyStarted(extractProxyName(line))
            lower.contains("start error") ->
                FrpLogEvent.ProxyStartError(extractProxyName(line), line)
            lower.contains("proxy closed") ->
                FrpLogEvent.ProxyClosed(extractProxyName(line))
            else -> FrpLogEvent.Plain(line)
        }
    }

    /**
     * 取行中最后一个 [xxx] 作为代理名，取不到返回空串
     */
    private fun extractProxyName(line: String): String =
        proxyNameRegex.findAll(line).lastOrNull()?.groupValues?.get(1) ?: ""
}
