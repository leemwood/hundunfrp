package cn.lemwood.platform

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

actual fun uploadToLogShare(content: String, source: String): LogShareResult {
    var conn: HttpURLConnection? = null
    return try {
        val body = buildJsonObject {
            put("content", content)
            put("source", source.take(64))
        }.toString()

        conn = URL("https://api.logshare.cn/1/log").openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val responseText = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        val json = runCatching { Json.parseToJsonElement(responseText).jsonObject }.getOrNull()
        val success = json?.get("success")?.jsonPrimitive?.content == "true"
        // 实测响应 url 在顶层，API.md 描述为 data.url，两者都兼容
        val url = json?.get("url")?.jsonPrimitive?.content
            ?: json?.get("data")?.jsonObject?.get("url")?.jsonPrimitive?.content

        if (success && !url.isNullOrBlank()) {
            LogShareResult(url = url)
        } else {
            val message = json?.get("message")?.jsonPrimitive?.content
            LogShareResult(error = message ?: "上传失败（HTTP ${conn.responseCode}）")
        }
    } catch (e: Exception) {
        LogShareResult(error = e.message ?: "网络错误")
    } finally {
        conn?.disconnect()
    }
}
