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
    "开启后：左键同行下一段；右键同行上一段/其它行换位；拦截右键菜单",
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
            "已开启：同行左键下一段；同行右键上一段；其它行左/右键只换位（拦截菜单，每段 ${InlineReadMode.chunkSize} 字）。Ctrl+Alt+单击可开关"
        } else {
            "开启后在编辑器中分段显示正文。也可用 Ctrl+Alt+单击开关（点在类/方法上时仍走 IDE 跳转）"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}
