package cn.lemwood.platform

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * frpc admin API 返回的单个代理状态。
 * statusText 取值（v0.70）：new / wait start / start error / running / check failed / closed；
 * 旧版本为 online / offline。err 在启动失败时含原因。
 * 注意：v0.70 起 /api/status 不再返回流量字段，trafficIn/Out 恒为 0。
 */
data class ProxyStatus(
    val name: String,
    val online: Boolean,
    val statusText: String = "",
    val err: String = "",
    val trafficIn: Long,
    val trafficOut: Long,
)

/**
 * frpc admin API /api/status 响应解析器。
 * 顶层结构：{"tcp":[...],"udp":[...],"http":[...],"https":[...],"stcp":[...],"xtcp":[...]}，
 * 数组元素含 name/status/today_traffic_in/today_traffic_out 字段。
 * 注意：部分 frpc 版本数字以 JSON 字符串形式返回，需容错；解析失败返回空列表。
 */
object FrpAdminStatus {

    private val sections = listOf("tcp", "udp", "http", "https", "stcp", "xtcp")

    fun parse(json: String): List<ProxyStatus> {
        return try {
            val root = Json.parseToJsonElement(json).jsonObject
            val result = mutableListOf<ProxyStatus>()
            for (section in sections) {
                val arr = root[section] as? JsonArray ?: continue
                for (element in arr) {
                    val obj = element as? JsonObject ?: continue
                    val name = obj["name"].asText() ?: continue
                    val statusText = obj["status"].asText().orEmpty()
                    result.add(
                        ProxyStatus(
                            name = name,
                            // v0.70 用 running，旧版本用 online
                            online = statusText == "running" || statusText == "online",
                            statusText = statusText,
                            err = obj["err"].asText().orEmpty(),
                            trafficIn = obj["today_traffic_in"].asLong(),
                            trafficOut = obj["today_traffic_out"].asLong(),
                        )
                    )
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * JsonPrimitive.content 对数字与字符串都返回原文，天然兼容数字以字符串返回的情况
     */
    private fun JsonElement?.asText(): String? = (this as? JsonPrimitive)?.content

    private fun JsonElement?.asLong(): Long =
        (this as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
}

/**
 * admin API phase → UI 状态映射。
 * v0.70：new / wait start / start error / running / check failed / closed；
 * 旧版：online / offline。返回 TunnelStatus 与 lastError（无错误为 null）。
 */
fun ProxyStatus.toTunnelStatus(): Pair<cn.lemwood.model.TunnelStatus, String?> = when (statusText) {
    "running", "online" -> cn.lemwood.model.TunnelStatus.ONLINE to null
    "start error", "check failed" ->
        cn.lemwood.model.TunnelStatus.ERROR to err.ifBlank { statusText }
    "closed", "offline" -> cn.lemwood.model.TunnelStatus.OFFLINE to null
    else -> cn.lemwood.model.TunnelStatus.CONNECTING to null // new / wait start
}
