package me.kuku.legado.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import me.kuku.legado.dao.CurrentReadData
import me.kuku.legado.toolwindow.IndexUI

class PreviousChapterAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        if (CurrentReadData.bookIndex < 1) {
            return
        }

        CurrentReadData.indexAtomicDecrement()

        IndexUI.getInstance().switchChapter(0)
    }
}
