package cn.lemwood.data

import cn.lemwood.state.AppStateHolder
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 应用数据导入/导出管理器。
 *
 * 负责将 [AppStateHolder] 中的隧道配置与设置序列化为 JSON 字符串，
 * 或从 JSON 字符串/文件反序列化并更新应用状态。
 *
 * 导入失败时不会修改 [AppStateHolder] 的任何状态。
 */
class ExportImportManager(private val appStateHolder: AppStateHolder) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * 将当前 [AppStateHolder] 中的隧道与设置导出为 JSON 字符串。
     */
    fun export(): String {
        val currentState = appStateHolder.state.value
        val exportData = ExportData(
            tunnels = currentState.tunnels,
            settings = currentState.settings
        )
        return json.encodeToString(exportData)
    }

    /**
     * 将当前应用数据导出并写入指定文件路径。
     */
    fun exportToFile(path: String) {
        writeTextToFile(path, export())
    }

    /**
     * 从 JSON 字符串导入应用数据。
     *
     * @return 导入成功返回 true，解析失败或校验失败返回 false。
     */
    fun import(jsonString: String): Boolean {
        val exportData: ExportData
        try {
            exportData = json.decodeFromString(jsonString)
        } catch (_: SerializationException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }

        if (!validate(exportData)) {
            return false
        }

        appStateHolder.replaceTunnels(exportData.tunnels)
        appStateHolder.replaceSettings(exportData.settings)
        return true
    }

    /**
     * 从指定文件路径读取 JSON 并导入应用数据。
     *
     * @return 读取或导入失败返回 false，成功返回 true。
     */
    fun importFromFile(path: String): Boolean {
        val content = readTextFromFile(path) ?: return false
        return import(content)
    }

    /**
     * 基础校验：
     * - tunnels 与 settings 已由反序列化保证非空，若 JSON 中缺失或显式为 null 会抛出序列化异常并在 [import] 中返回 false
     * - 每条隧道的本地端口与远程端口必须在 1..65535 范围内
     */
    private fun validate(exportData: ExportData): Boolean {
        return exportData.tunnels.all { tunnel ->
            isValidPort(tunnel.localPort) && isValidPort(tunnel.remotePort)
        }
    }

    private fun isValidPort(port: Int): Boolean = port in 1..65535
}
