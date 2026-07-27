package cn.lemwood.data

import cn.lemwood.model.TunnelUiState

/**
 * 平台无关的隧道配置持久化存储抽象。
 * 具体实现由 Android/Desktop 源集提供。
 */
expect class TunnelConfigStore {

    /**
     * 从平台持久化存储加载隧道列表。
     * 若尚无保存数据，返回空列表。
     */
    suspend fun load(): List<TunnelUiState>

    /**
     * 将 [tunnels] 保存到平台持久化存储。
     */
    suspend fun save(tunnels: List<TunnelUiState>)

    /**
     * 是否已有持久化数据（用于区分真正的首次启动）。
     */
    suspend fun hasData(): Boolean
}

/**
 * 创建 [TunnelConfigStore] 实例。
 *
 * @param context 平台相关上下文（Android 传入 Context，Desktop 可传 null）。
 */
expect fun createTunnelConfigStore(context: Any? = null): TunnelConfigStore
