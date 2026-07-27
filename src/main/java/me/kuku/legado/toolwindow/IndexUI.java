package me.kuku.legado.toolwindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import me.kuku.legado.api.ApiUtils;
import me.kuku.legado.api.dto.BookDTO;
import me.kuku.legado.common.Constant;
import me.kuku.legado.dao.CurrentReadData;
import me.kuku.legado.state.State;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;

@Getter
public class IndexUI {

    /**
     * 主面板
     */
    private JPanel rootPanel;

    /**
     * 书架面板
     */
    private JPanel bookshelfPanel;
    /**
     * 书架面板的目录的滚动面板
     */
    private JScrollPane bookshelfScrollPane;
    /**
     * 书架面板的目录表格
     */
    private JTable bookshelfTable;
    /**
     * 书架面板的错误提示
     */
    private JTextPane errorTipsPane;

    /**
     * 正文面板
     */
    private JPanel textBodyPanel;
    /**
     * 正文面板的操作按钮bar
     */
    private JToolBar bar1;
    /**
     * 正文面板的滚动面板
     */
    private JScrollPane textBodyScrollPane;
    /**
     * 正文面板的滚动面板中的正文
     */
    private JTextPane textBodyPane;
    private JToolBar bar2;

    private List<BookDTO> bookshelf = new ArrayList<>();

    private static final DefaultTableModel BOOK_SHELF_TABLE_MODEL = new DefaultTableModel(null, new String[]{"name", "current", "source", "author"}) {
        @Override
        public boolean isCellEditable(int row, int column) {
            // 表格不允许被编辑
            return false;
        }
    };

    private String currentPreLoad;

    private static final IndexUI INSTANCE = new IndexUI();

    public IndexUI() {
        // 隐藏正文面板
        textBodyPanel.setVisible(false);

        // 隐藏书架面板的错误提示
        errorTipsPane.setVisible(false);
        // 设置书架面板的错误提示为不可编辑
        errorTipsPane.setEditable(false);

        // 设置书架面板的表格数据格式
        bookshelfTable.setModel(IndexUI.BOOK_SHELF_TABLE_MODEL);

        final ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar shelfBar = actionManager.createActionToolbar(Constant.PLUGIN_SHELF_BAR_ID, (DefaultActionGroup) actionManager.getAction(Constant.PLUGIN_SHELF_BAR_ID), true);
        shelfBar.setTargetComponent(bar1);
        bar1.add(shelfBar.getComponent());
        bar1.setBorderPainted(false);
        bar1.setFloatable(false);


        ActionToolbar actionToolbar = actionManager.createActionToolbar(Constant.PLUGIN_TOOL_BAR_ID, (DefaultActionGroup) actionManager.getAction(Constant.PLUGIN_TOOL_BAR_ID), true);
        actionToolbar.setTargetComponent(bar2);
        bar2.add(actionToolbar.getComponent());
        bar2.setBorderPainted(false);
        bar2.setFloatable(false);
        bar2.setVisible(false);
        bookshelfTable.addMouseListener(toTextBodyMouseAdapter());

        textBodyScrollPane.getVerticalScrollBar().addAdjustmentListener(preload());
    }

    private AdjustmentListener preload() {
        return e -> {
            Adjustable adjustable = e.getAdjustable();
            int value = adjustable.getValue();
            int max = adjustable.getMaximum();
            int extent = adjustable.getVisibleAmount();

            int totalRange = max - extent;
            double progress = 0.0;
            if (totalRange > 0) {
                progress = (double) value / totalRange;
            }
            if (progress > 0.5) {
                BookDTO book = CurrentReadData.getBook();
                int nextIndex = CurrentReadData.getBookIndex() + 1;
                if (nextIndex >= CurrentReadData.getBookChapterList().size()) {
                    return;
                }
                String key = book.getBookId() + ":" + book.getSource() + ":" + nextIndex;
                if (!Objects.equals(key, currentPreLoad)) {
                    currentPreLoad = key;
                    ApplicationManager.getApplication().executeOnPooledThread(() -> {
                        ApiUtils.getBookContent(book, nextIndex);
                    });
                }
            }
        };
    }

    public void refreshBookshelf() {
        bar1.setVisible(true);
        bar2.setVisible(false);
        bookshelfPanel.setVisible(true);
        hideErrorTips();
        CompletableFuture.supplyAsync(ApiUtils::getBookshelf)
                .thenAccept(books -> {
                    this.bookshelf = new ArrayList<>(books);
                    setBookshelfUI(books);
                }).exceptionally(throwable -> {
                    showErrorTips("获取书架列表失败: " + rootMessage(throwable));
                    return null;
                });
    }

    public void refreshTextBody() {
        initTextBodyUI();

        BookDTO book = CurrentReadData.getBook();
        CompletableFuture.supplyAsync(() -> ApiUtils.getChapterList(book))
                .thenAccept(bookChapters -> {
                    // 保存章节列表
                    CurrentReadData.setBookChapterList(bookChapters);

                    // 根据 last_chapter_item_id 定位章节
                    int index = ApiUtils.resolveChapterIndex(book, bookChapters);
                    CurrentReadData.setBookIndex(index);
                    book.setDurChapterIndex(index);
                    if (index >= 0 && index < bookChapters.size()) {
                        book.setDurChapterTitle(bookChapters.get(index).getTitle());
                    }

                    // 设置正文数据
                    setTextBodyUIData(book.getDurChapterPos() == null ? 0 : book.getDurChapterPos());
                }).exceptionally(throwable -> {
                    showErrorTips(rootMessage(throwable));
                    return null;
                });
    }

    private MouseAdapter toTextBodyMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                bar2.setVisible(true);
                bar1.setVisible(false);
                int row = bookshelfTable.rowAtPoint(evt.getPoint());
                int col = bookshelfTable.columnAtPoint(evt.getPoint());

                if (row < 0 || col < 0 || row >= bookshelf.size()) {
                    return;
                }

                // 保存当前阅读信息
                BookDTO book = bookshelf.get(row);
                CurrentReadData.setBook(book);
                CurrentReadData.setBookIndex(book.getDurChapterIndex() == null ? 0 : book.getDurChapterIndex());
                refreshTextBody();
            }
        };
    }

    private void setBookshelfUI(List<BookDTO> books) {
        // 清空表格
        IndexUI.BOOK_SHELF_TABLE_MODEL.getDataVector().clear();

        // 添加表格数据
        books.stream().map(book -> {
            Vector<String> bookVector = new Vector<>();
            bookVector.add(book.getName());
            String current = book.getDurChapterTitle();
            if (current == null || current.isBlank()) {
                current = book.getLatestChapterTitle();
            }
            bookVector.add(current);
            bookVector.add(book.getSource());
            bookVector.add(book.getAuthor());
            return bookVector;
        }).forEach(IndexUI.BOOK_SHELF_TABLE_MODEL::addRow);

        BOOK_SHELF_TABLE_MODEL.fireTableDataChanged();

        if (!bookshelfScrollPane.isShowing()) {
            bookshelfScrollPane.setVisible(true);
            errorTipsPane.setVisible(false);
        }
    }

    /**
     * 切换章节
     *
     * @param durChapterPos 当前在章节中的位置
     */
    public void switchChapter(int durChapterPos) {
        textBodyScrollPane.getVerticalScrollBar().setValue(0);

        // 设置正文面板UI
        initTextBodyUI();

        // 设置正文数据
        setTextBodyUIData(durChapterPos);
    }

    private void initTextBodyUI() {
        hideErrorTips();

        Font font = textBodyPane.getFont();
        int fontSize = State.getSettings().getTextBodyFontSize();
        if (fontSize > 0) {
            textBodyPane.setFont(new Font(font.getName(), font.getStyle(), fontSize));
        }
        // 设置加载中的提示
        textBodyPane.setText("加载中...");

        if (!textBodyPanel.isShowing()) {
            textBodyPanel.setVisible(true);
            bookshelfPanel.setVisible(false);
        }

        if (!textBodyScrollPane.isShowing()) {
            textBodyScrollPane.setVisible(true);
        }

        // 获取焦点到文本框
        textBodyPane.requestFocus();
    }

    private void setTextBodyUIData(int durChapterPos) {
        BookDTO book = CurrentReadData.getBook();
        int index = CurrentReadData.getBookIndex();
        if (index < 0 || index >= CurrentReadData.getBookChapterList().size()) {
            showErrorTips("章节下标无效");
            return;
        }

        // 获取章节标题
        String title = CurrentReadData.getBookChapter().getTitle();

        // 调用API获取正文内容
        CompletableFuture.supplyAsync(() -> ApiUtils.getBookContent(book, index))
                .thenAccept(bookContent -> {
                    CurrentReadData.setBodyContent(bookContent);

                    // 设置正文内容
                    textBodyPane.setText(title + "\n" + bookContent);
                    int caret = Math.max(0, Math.min(durChapterPos, textBodyPane.getDocument().getLength()));
                    textBodyPane.setCaretPosition(caret);
                }).exceptionally(throwable -> {
                    showErrorTips(rootMessage(throwable));
                    return null;
                });

        // 同步阅读进度
        CompletableFuture.runAsync(() -> ApiUtils.saveBookProgress(book, index));
    }

    private void hideErrorTips() {
        errorTipsPane.setVisible(false);
    }

    private void showErrorTips(String text) {
        textBodyPanel.setVisible(false);
        bookshelfPanel.setVisible(false);
        errorTipsPane.setVisible(true);
        errorTipsPane.setText(text);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable t = throwable;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return msg == null || msg.isBlank() ? t.toString() : msg;
    }

    public JComponent getComponent() {
        return rootPanel;
    }

    public static IndexUI getInstance() {
        return INSTANCE;
    }
}
