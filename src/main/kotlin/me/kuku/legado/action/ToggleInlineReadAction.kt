package me.kuku.legado.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.util.IconLoader
import me.kuku.legado.inline.InlineReadMode

/**
 * 标题栏开关：行内隐蔽阅读（编辑器光标旁分段显示）。
 */
class ToggleInlineReadAction : AnAction(
    "行内阅读",
    "开启后：单击显示；同行再点下一段；同行 Ctrl/⌘+单击上一段；换行只换位置",
    IconLoader.getIcon("/icons/inlineRead.svg", ToggleInlineReadAction::class.java)
), Toggleable {

    override fun actionPerformed(e: AnActionEvent) {
        val on = InlineReadMode.toggle()
        updatePresentation(e, on)
    }

    override fun update(e: AnActionEvent) {
        updatePresentation(e, InlineReadMode.enabled)
        e.presentation.isEnabled = true
    }

    private fun updatePresentation(e: AnActionEvent, on: Boolean) {
        Toggleable.setSelected(e.presentation, on)
        e.presentation.text = if (on) "关闭行内阅读" else "开启行内阅读"
        e.presentation.description = if (on) {
            "已开启：单击显示；同行再点下一段；同行 Ctrl/⌘+单击上一段；换行只换位置（每段 ${InlineReadMode.chunkSize} 字）"
        } else {
            "开启后在编辑器中分段显示正文，适合隐蔽阅读"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
