package cn.lemwood.data

/**
 * 跨平台文件写入辅助函数（expect）。
 * Android 与 Desktop 均为 JVM 平台，使用 java.io.File 实现。
 */
internal expect fun writeTextToFile(path: String, content: String)

/**
 * 跨平台文件读取辅助函数（expect）。
 * 若文件不存在或读取失败，返回 null。
 */
internal expect fun readTextFromFile(path: String): String?

/**
 * 跨平台文件保存对话框（expect）。
 * 显示系统文件选择器，返回用户选择的文件路径；取消则返回 null。
 */
expect fun showFileSaveDialog(title: String, initialPath: String): String?

/**
 * 跨平台文件打开对话框（expect）。
 * 显示系统文件选择器，返回用户选择的文件路径；取消则返回 null。
 */
expect fun showFileOpenDialog(title: String): String?

