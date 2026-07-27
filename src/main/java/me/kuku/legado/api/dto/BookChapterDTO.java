package me.kuku.legado.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookChapterDTO {

    @JsonProperty("item_id")
    private String itemId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("index")
    private Integer index;

    /**
     * 兼容旧字段：url 等同 item_id
     */
    public String getUrl() {
        return itemId;
    }

    public void setUrl(String url) {
        this.itemId = url;
    }
}
