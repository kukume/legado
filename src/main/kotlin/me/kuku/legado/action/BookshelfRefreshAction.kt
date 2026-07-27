package me.kuku.legado.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import me.kuku.legado.toolwindow.IndexUI

class BookshelfRefreshAction: AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        IndexUI.getInstance().refreshBookshelf()
    }


}
