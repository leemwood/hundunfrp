# Checklist

- [x] `SettingsStore` expect/actual 抽象存在于 `cn.lemwood.data`
- [x] `TunnelConfigStore` expect/actual 抽象存在于 `cn.lemwood.data`
- [x] Android `SettingsStore` 使用 DataStore 持久化 `AppSettings`
- [x] Android `TunnelConfigStore` 使用私有文件保存隧道 JSON
- [x] Desktop `SettingsStore` 读写 `D:\.config\frp-kmp\settings.json`
- [x] Desktop `TunnelConfigStore` 读写 `D:\.config\frp-kmp\tunnels.json`
- [x] 配置目录不存在时自动创建
- [x] `AppStateHolder` 启动时加载持久化数据
- [x] 隧道增删改后自动保存
- [x] 设置变更后自动保存
- [x] `AppStateHolder.resetToDefaults()` 清除持久化数据并恢复默认
- [x] `ExportData` 数据类封装导出格式
- [x] 导入导出支持 JSON 序列化/反序列化并做基础校验
- [x] 导入失败时回滚到原数据
- [x] SettingsScreen 的导出/导入/清除按钮连接到真实逻辑
- [x] Android assembleDebug 构建成功
- [x] Desktop run 成功且配置正确落盘
- [x] 重启后设置与隧道能够恢复
