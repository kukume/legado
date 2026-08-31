package me.kuku.legado.common;

public interface Constant {
    /**
     * 插件包名
     */
    String PLUGIN_PACKAGE = "me.kuku.legado";
    /**
     * 插件id前缀
     */
    String PLUGIN_ID_PREFIX = "me.kuku.legado";
    /**
     * 插件设置id
     */
    String PLUGIN_SETTING_ID = PLUGIN_ID_PREFIX + ".setting";

    /**
     * action id前缀
     */
    String PLUGIN_ACTION_ID_PREFIX = PLUGIN_ID_PREFIX + ".action";

    /**
     * action 上一章id
     */
    String PLUGIN_ACTION_PREVIOUS_CHAPTER_ID = PLUGIN_ACTION_ID_PREFIX + ".previousChapter";
    /**
     * action 下一章id
     */
    String PLUGIN_ACTION_NEXT_CHAPTER_ID = PLUGIN_ACTION_ID_PREFIX + ".nextChapter";

    /**
     * 正文阅读tool bar
     */
    String PLUGIN_TOOL_BAR_ID = PLUGIN_ID_PREFIX + ".bar.textBodyToolbar";


    String PLUGIN_SHELF_BAR_ID = PLUGIN_ID_PREFIX + ".bar.bookshelfBar";

    /**
     * 标题栏：显示/隐藏下方图标工具栏
     */
    String PLUGIN_ACTION_TOGGLE_ACTION_BAR_ID = PLUGIN_ACTION_ID_PREFIX + ".toggleActionBar";

    /**
     * 持久化数据
     */
    String PLUGIN__PERSISTENCE_DATA = PLUGIN_ID_PREFIX + ".persistence.data";
}
