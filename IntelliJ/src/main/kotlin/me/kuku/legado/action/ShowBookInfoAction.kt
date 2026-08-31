package me.kuku.legado.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import me.kuku.legado.dao.CurrentReadData

class ShowBookInfoAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
    }

    override fun update(e: AnActionEvent) {
        val book = CurrentReadData.book

        if (CurrentReadData.bookChapterList.isEmpty()) {
            return
        }

        val text = book.name + " - " + CurrentReadData.getBookChapter().getTitle()
        e.presentation.setText(text)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
