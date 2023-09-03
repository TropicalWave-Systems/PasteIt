package de.mischmaschine.pasteit

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class PasteItAction : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getRequiredData(CommonDataKeys.EDITOR)
        val selectionModel = editor.selectionModel
        val document = editor.document

        val code = when {
            selectionModel.hasSelection() -> selectionModel.selectedText
            else -> document.text
        }


        val fileExtension = event.getData(CommonDataKeys.VIRTUAL_FILE)?.extension
        code?.let { runBlocking { uploadCode(it, fileExtension) } }
    }

    @OptIn(InternalAPI::class)
    private suspend fun uploadCode(code: String, fileExtension: String?) {
        val client = HttpClient(CIO)
        val response = client.post(DEFAULT_URL + "data/post") {
            header(HttpHeaders.ContentType, getContentTypeByFileExtension(fileExtension))
            body = code
        }

        val uploadCode = response.bodyAsText().replace("{\"key\":\"", "").replace("\"}", "")
        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("PasteIt")
        val notification = notificationGroup.createNotification(
            "Upload successful",
            """
                The URL has been copied to your clipboard
                Upload Code: $uploadCode
            """.trimIndent(),
            NotificationType.INFORMATION
        )
        notification.notify(null)

        notification.addAction(object : AnAction("Re-Copy URL") {
            override fun actionPerformed(event: AnActionEvent) {
                copyToClipboard(DEFAULT_URL + uploadCode)
            }
        })

        copyToClipboard(DEFAULT_URL + uploadCode)

    }

    private fun copyToClipboard(text: String) {
        val selection = StringSelection(text)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }

    private fun getContentTypeByFileExtension(extension: String?): String {
        return when (extension?.lowercase()) {
            "yaml" -> "text/yaml"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "ini" -> "text/ini"
            "java" -> "text/java"
            "js" -> "application/javascript"
            "ts" -> "application/typescript"
            "py" -> "text/python"
            "kt", "kts" -> "text/kotlin"
            "cpp" -> "text/cpp"
            "cs" -> "text/csharp"
            "sh" -> "text/shellscript"
            "rb" -> "text/ruby"
            "rs" -> "text/rust"
            "sql" -> "application/sql"
            "go" -> "text/go"
            "html" -> "text/html"
            "css" -> "text/css"
            "php" -> "text/php"
            "dockerfile" -> "text/dockerfile"
            "md" -> "text/markdown"
            else -> "text/plain"
        }
    }

    private companion object {
        private const val DEFAULT_URL = "https://paste.tropicalwave.de/"
    }

}
