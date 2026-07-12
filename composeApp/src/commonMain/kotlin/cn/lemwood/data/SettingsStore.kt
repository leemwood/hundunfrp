package cn.lemwood.data

import cn.lemwood.model.AppSettings

/**
 * 平台无关的 AppSettings 持久化存储抽象。
 * 具体实现由 Android/Desktop 源集提供。
 */
expect class SettingsStore {

    /**
     * 从平台持久化存储加载 [AppSettings]。
     * 若尚无保存数据，返回默认 [AppSettings]。
     */
    suspend fun load(): AppSettings

    /**
     * 将 [settings] 保存到平台持久化存储。
     */
    suspend fun save(settings: AppSettings)
}

/**
 * 创建 [SettingsStore] 实例。
 *
 * @param context 平台相关上下文（Android 传入 Context，Desktop 可传 null）。
 */
expect fun createSettingsStore(context: Any? = null): SettingsStore
