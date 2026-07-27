package me.kuku.legado.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 书架书籍信息（langge /get_book_shelf）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookDTO {

    /** 书架记录 id，用于更新进度 */
    @JsonProperty("id")
    private Long shelfId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("book_name")
    private String name;

    @JsonProperty("book_id")
    private String bookId;

    /**
     * 详情接口返回的 book_id（常带 toc 信息），优先用于拉目录
     */
    private String catalogBookId;

    @JsonProperty("author")
    private String author;

    @JsonProperty("thumb_url")
    private String coverUrl;

    @JsonProperty("abstract")
    private String intro;

    @JsonProperty("source")
    private String source;

    @JsonProperty("tab")
    private String tab;

    @JsonProperty("category")
    private String kind;

    @JsonProperty("last_chapter_update_time")
    private Long latestChapterTime;

    @JsonProperty("last_chapter_title")
    private String latestChapterTitle;

    /** 阅读进度章节 item_id */
    @JsonProperty("last_chapter_item_id")
    private String lastChapterItemId;

    @JsonProperty("status")
    private String status;

    /** 1=在书架，2=已移出等 */
    @JsonProperty("read_status")
    private Integer readStatus;

    /** 当前章节下标（本地根据目录解析后填充） */
    private Integer durChapterIndex = 0;

    /** 章节内阅读位置（本地使用） */
    private Integer durChapterPos = 0;

    /** 当前章节标题（展示用） */
    private String durChapterTitle;

    /**
     * 兼容旧逻辑：用 bookId 充当唯一标识
     */
    public String getBookUrl() {
        return bookId;
    }

    public void setBookUrl(String bookUrl) {
        this.bookId = bookUrl;
    }

    public String getOrigin() {
        return source;
    }

    public String getOriginName() {
        return source;
    }
}
