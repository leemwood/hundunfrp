package cn.lemwood.data

import java.io.File

internal actual fun writeTextToFile(path: String, content: String) {
    File(path).writeText(content, Charsets.UTF_8)
}

internal actual fun readTextFromFile(path: String): String? {
    val file = File(path)
    return if (file.exists()) file.readText(Charsets.UTF_8) else null
}

actual fun showFileSaveDialog(title: String, initialPath: String): String? {
    return null
}

actual fun showFileOpenDialog(title: String): String? {
    return null
}
