package cn.lemwood.data

import cn.lemwood.model.AppSettings
import cn.lemwood.model.TunnelUiState
import kotlinx.serialization.Serializable

/**
 * 应用导出/导入数据的统一结构。
 * 包含完整的隧道配置与应用设置，用于备份与恢复。
 */
@Serializable
data class ExportData(
    val tunnels: List<TunnelUiState>,
    val settings: AppSettings
)
