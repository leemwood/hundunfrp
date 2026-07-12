package cn.lemwood.data

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

internal actual fun writeTextToFile(path: String, content: String) {
    File(path).writeText(content, Charsets.UTF_8)
}

internal actual fun readTextFromFile(path: String): String? {
    val file = File(path)
    return if (file.exists()) file.readText(Charsets.UTF_8) else null
}

actual fun showFileSaveDialog(title: String, initialPath: String): String? {
    val chooser = JFileChooser(File(initialPath).parent)
    chooser.dialogTitle = title
    chooser.selectedFile = File(initialPath)
    chooser.fileFilter = FileNameExtensionFilter("JSON 文件 (*.json)", "json")
    val result = chooser.showSaveDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

actual fun showFileOpenDialog(title: String): String? {
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    chooser.fileFilter = FileNameExtensionFilter("JSON 文件 (*.json)", "json")
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}
