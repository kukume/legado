package me.kuku.legado.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import me.kuku.legado.action.ToggleActionBarAction;
import me.kuku.legado.action.ToggleInlineReadAction;
import me.kuku.legado.inline.InlineReadMode;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IndexWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 标题栏右侧：行内阅读开关、下方工具栏显隐
        toolWindow.setTitleActions(List.of(
                new ToggleInlineReadAction(),
                new ToggleActionBarAction()
        ));
        // 若上次已开启行内阅读，确保监听器挂上
        if (InlineReadMode.getEnabled()) {
            InlineReadMode.setEnabled(true);
        }

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(IndexUI.getInstance().getComponent(), "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
