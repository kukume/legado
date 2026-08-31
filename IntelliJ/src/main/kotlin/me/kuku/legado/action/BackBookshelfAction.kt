package me.kuku.legado.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import me.kuku.legado.toolwindow.IndexUI

class BackBookshelfAction: AnAction() {

    private val indexUI = IndexUI.getInstance()

    override fun actionPerformed(e: AnActionEvent) {
        indexUI.refreshBookshelf()

        indexUI.textBodyPanel.isVisible = false;
        indexUI.bookshelfPanel.isVisible = true;
    }
}
