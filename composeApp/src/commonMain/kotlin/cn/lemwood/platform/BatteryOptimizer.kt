package cn.lemwood.platform

expect object BatteryOptimizer {
    /** 当前平台是否支持电池优化白名单（仅 Android 为 true） */
    val isSupported: Boolean
    /** 是否已加入电池优化白名单（不支持的平台返回 true） */
    fun isIgnoringBatteryOptimizations(): Boolean
    /** 弹出系统「忽略电池优化」授权对话框（不支持的平台 no-op） */
    fun requestExemption()
    /** 打开厂商自启动/后台管理设置页，无匹配回退应用详情页（不支持的平台 no-op） */
    fun openOemBackgroundSettings()
}
