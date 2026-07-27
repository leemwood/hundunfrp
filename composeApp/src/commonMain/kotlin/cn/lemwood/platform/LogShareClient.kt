package cn.lemwood.platform

/**
 * logshare.cn 上传结果
 */
data class LogShareResult(
    val url: String? = null,
    val error: String? = null,
)

/**
 * 上传日志内容到 logshare.cn，成功返回分享链接。
 * 阻塞调用，需在 IO 线程执行。
 */
expect fun uploadToLogShare(content: String, source: String = "hundunfrp"): LogShareResult
