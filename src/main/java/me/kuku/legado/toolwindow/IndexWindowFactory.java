package me.kuku.legado.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import me.kuku.legado.action.ToggleActionBarAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IndexWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 标题栏右侧（三点/折叠左侧）：切换下方图标工具栏
        toolWindow.setTitleActions(List.of(new ToggleActionBarAction()));

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(IndexUI.getInstance().getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
