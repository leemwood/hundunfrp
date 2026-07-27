package cn.lemwood.platform

actual object BatteryOptimizer {
    actual val isSupported: Boolean = false

    // 桌面端无电池优化概念，视为已豁免
    actual fun isIgnoringBatteryOptimizations(): Boolean = true

    actual fun requestExemption() = Unit

    actual fun openOemBackgroundSettings() = Unit
}
