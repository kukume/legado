package me.kuku.legado.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import me.kuku.legado.dao.CurrentReadData
import me.kuku.legado.toolwindow.IndexUI

class NextChapterAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val next = CurrentReadData.bookIndex + 1
        if (next >= CurrentReadData.bookChapterList.size) {
            return
        }
        CurrentReadData.indexAtomicIncrement()
        IndexUI.getInstance().switchChapter(0)
    }
}
