package me.kuku.legado.api

import com.google.common.cache.CacheBuilder
import me.kuku.legado.api.dto.BookChapterDTO
import me.kuku.legado.api.dto.BookDTO
import me.kuku.legado.state.SettingsService
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.util.concurrent.TimeUnit

/**
 * 大灰狼融合书源 / langge 接口客户端。
 *
 * 对齐「安卓阅读app-大灰狼融合」书源：
 * 书架：GET  /get_book_shelf
 * 详情：GET  /detail?book_id&source&tab&variable
 * 目录：GET  /catalog?book_id&source&tab&variable
 * 正文：POST /content  body={html,item_id,source,tab,tone_id,variable,version}
 * 进度：POST /update_book_shelf
 */
object ApiUtils {

    private const val DEFAULT_TAB = "小说"
    /** 书源 ruleContent 写死的 version */
    private const val DEFAULT_VERSION = "4.11.5.1"
    private const val DEFAULT_TONE_ID = "4"
    private const val DEFAULT_VARIABLE = """{"custom":""}"""

    private val bookCache = CacheBuilder.newBuilder()
        .maximumSize(20)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build<String, String>()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    private val settingsState by lazy {
        SettingsService.getInstance().state
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .header("User-Agent", DEFAULT_UA)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                val cookie = settingsState.cookie.trim()
                if (cookie.isNotEmpty()) {
                    builder.header("Cookie", cookie)
                }
                val base = normalizeBaseUrl(settingsState.address)
                if (base.isNotEmpty()) {
                    builder.header("Origin", base)
                    builder.header("Referer", "$base/online_search")
                }
                chain.proceed(builder.build())
            }
            .build()
    }

    val objectMapper = jsonMapper {
        addModule(kotlinModule())
    }

    private const val DEFAULT_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

    private fun normalizeBaseUrl(address: String?): String {
        val raw = address?.trim().orEmpty().ifBlank { "https://api.langge.cf" }
        return raw.trimEnd('/')
    }

    private fun String.toJsonNode(): JsonNode {
        return try {
            objectMapper.readTree(this)
        } catch (e: Exception) {
            error("接口返回非 JSON: ${e.message}")
        }
    }

    private fun baseUrl(): String = normalizeBaseUrl(settingsState.address)

    private fun ensureConfigured() {
        if (settingsState.cookie.isBlank()) {
            error("请先在 Settings → Tools → Legado Reader 中配置 Cookie")
        }
    }

    private fun get(path: String, query: Map<String, String?> = emptyMap()): JsonNode {
        ensureConfigured()
        val httpUrlBuilder = (baseUrl() + path).toHttpUrl().newBuilder()
        query.forEach { (k, v) ->
            if (!v.isNullOrBlank()) {
                httpUrlBuilder.addQueryParameter(k, v)
            }
        }
        val request = Request.Builder().url(httpUrlBuilder.build()).get().build()
        return client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: ${body.take(200)}")
            }
            body.toJsonNode()
        }
    }

    private fun postJson(path: String, json: String): JsonNode {
        ensureConfigured()
        val request = Request.Builder()
            .url(baseUrl() + path)
            .post(json.toRequestBody(mediaType))
            .build()
        return client.newCall(request).execute().use { response ->
            val body = response.body.string()
            if (!response.isSuccessful) {
                error("HTTP ${response.code}: ${body.take(200)}")
            }
            body.toJsonNode()
        }
    }

    private fun textOf(node: JsonNode?, vararg keys: String): String {
        if (node == null || node.isNull) return ""
        for (key in keys) {
            val child = node[key]
            if (child != null && !child.isNull) {
                val value = child.asString()
                if (value.isNotBlank()) return value
            }
        }
        return ""
    }

    private fun longOf(node: JsonNode?, key: String): Long? {
        val child = node?.get(key) ?: return null
        if (child.isNull) return null
        return try {
            child.asLong()
        } catch (_: Exception) {
            child.asString().toLongOrNull()
        }
    }

    private fun intOf(node: JsonNode?, key: String): Int? {
        val child = node?.get(key) ?: return null
        if (child.isNull) return null
        return try {
            child.asInt()
        } catch (_: Exception) {
            child.asString().toIntOrNull()
        }
    }

    private fun parseBook(node: JsonNode): BookDTO {
        val book = BookDTO()
        book.shelfId = longOf(node, "id")
        book.email = textOf(node, "email")
        book.name = textOf(node, "book_name", "name")
        book.bookId = textOf(node, "book_id")
        book.catalogBookId = textOf(node, "book_id")
        book.author = textOf(node, "author").ifBlank { "未知作者" }
        book.coverUrl = textOf(node, "thumb_url", "coverUrl")
        book.intro = textOf(node, "abstract", "intro")
        book.source = textOf(node, "source")
        book.tab = textOf(node, "tab").ifBlank { DEFAULT_TAB }
        book.kind = textOf(node, "category", "kind")
        book.latestChapterTime = longOf(node, "last_chapter_update_time")
        book.latestChapterTitle = textOf(node, "last_chapter_title", "latestChapterTitle")
        book.lastChapterItemId = textOf(node, "last_chapter_item_id")
        book.status = textOf(node, "status")
        book.readStatus = intOf(node, "read_status")
        book.durChapterTitle = book.latestChapterTitle
        book.durChapterIndex = 0
        book.durChapterPos = 0
        return book
    }

    private fun parseChapter(node: JsonNode, index: Int): BookChapterDTO {
        val chapter = BookChapterDTO()
        chapter.itemId = textOf(node, "item_id", "id", "chapter_id", "cid")
        chapter.title = textOf(node, "title", "chapter_title", "name").ifBlank { "第${index + 1}章" }
        chapter.index = index
        return chapter
    }

    private fun extractContentText(payload: JsonNode): String {
        // { content: "..." }
        val contentNode = payload["content"]
        if (contentNode != null && !contentNode.isNull) {
            if (contentNode.isObject) {
                val nested = textOf(contentNode, "content", "text", "html")
                if (nested.isNotBlank()) return nested
            } else {
                val text = contentNode.asString()
                if (text.isNotBlank()) return text
            }
        }
        // { data: { content: "..." } } or { data: "..." } or { data: [...] }
        val data = payload["data"]
        if (data != null && !data.isNull) {
            if (data.isObject) {
                val text = textOf(data, "content", "text", "html")
                if (text.isNotBlank()) return text
                val nestedList = data["data"]
                if (nestedList != null && nestedList.isArray && nestedList.size() > 0) {
                    val first = nestedList[0]
                    val firstText = textOf(first, "content", "text", "html")
                    if (firstText.isNotBlank()) return firstText
                }
            } else if (data.isArray && data.size() > 0) {
                val first = data[0]
                if (first.isObject) {
                    val firstText = textOf(first, "content", "text", "html")
                    if (firstText.isNotBlank()) return firstText
                } else {
                    return first.asString()
                }
            } else if (data.isString) {
                return data.asString()
            }
        }
        val fallback = textOf(payload, "text", "html", "body")
        return fallback
    }

    private fun toPlainText(content: String): String {
        if (content.isBlank()) return content
        if (!content.contains('<')) {
            return content
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
        }
        return content
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p\\s*>"), "\n")
            .replace(Regex("(?i)</div\\s*>"), "\n")
            .replace(Regex("(?i)<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun resolveCatalogBookId(book: BookDTO): String {
        if (!book.catalogBookId.isNullOrBlank() && book.catalogBookId != book.bookId) {
            return book.catalogBookId
        }
        val bookId = book.bookId
        val source = book.source
        require(!bookId.isNullOrBlank()) { "缺少 book_id" }
        require(!source.isNullOrBlank()) { "缺少 source" }
        val tab = book.tab?.ifBlank { DEFAULT_TAB } ?: DEFAULT_TAB

        val jsonNode = get(
            "/detail",
            mapOf(
                "book_id" to bookId,
                "source" to source,
                "tab" to tab,
                "variable" to DEFAULT_VARIABLE,
            )
        )
        val code = jsonNode["code"]
        if (code != null && !code.isNull && code.asInt() != 0) {
            // detail 失败时退回原始 book_id
            return bookId
        }
        val data = jsonNode["data"]
        val resolved = textOf(data, "book_id").ifBlank { bookId }
        book.catalogBookId = resolved
        if (book.name.isNullOrBlank()) {
            book.name = textOf(data, "book_name", "name")
        }
        if (book.author.isNullOrBlank()) {
            book.author = textOf(data, "author")
        }
        if (book.coverUrl.isNullOrBlank()) {
            book.coverUrl = textOf(data, "thumb_url")
        }
        if (book.intro.isNullOrBlank()) {
            book.intro = textOf(data, "abstract")
        }
        return resolved
    }

    @JvmStatic
    fun getBookshelf(): List<BookDTO> {
        val jsonNode = get("/get_book_shelf")
        val data = jsonNode["data"]
        if (data == null || data.isNull) {
            // 兼容 {code, msg, data}
            val code = jsonNode["code"]
            if (code != null && !code.isNull && code.asInt() != 0) {
                error(textOf(jsonNode, "msg", "error", "errorMsg").ifBlank { "获取书架失败" })
            }
            return emptyList()
        }
        if (!data.isArray) {
            error("书架数据格式错误")
        }
        return buildList {
            data.forEach { node ->
                val book = parseBook(node)
                // read_status: 1=在书架，2=已移出；只展示在架书籍
                val status = book.readStatus
                if (status != null && status != 1) {
                    return@forEach
                }
                // 插件仅支持小说阅读
                val tab = book.tab.orEmpty()
                if (tab.isNotBlank() && tab != DEFAULT_TAB) {
                    return@forEach
                }
                add(book)
            }
        }
    }

    @JvmStatic
    fun getChapterList(book: BookDTO): List<BookChapterDTO> {
        val catalogBookId = resolveCatalogBookId(book)
        val source = book.source
        require(!source.isNullOrBlank()) { "缺少 source" }
        val tab = book.tab?.ifBlank { DEFAULT_TAB } ?: DEFAULT_TAB

        val jsonNode = get(
            "/catalog",
            mapOf(
                "book_id" to catalogBookId,
                "source" to source,
                "tab" to tab,
                "variable" to DEFAULT_VARIABLE,
            )
        )
        val code = jsonNode["code"]
        if (code != null && !code.isNull && code.asInt() != 0) {
            error(textOf(jsonNode, "msg", "error").ifBlank { "获取目录失败" })
        }
        val data = jsonNode["data"]
        if (data == null || !data.isArray) {
            error("目录数据格式错误")
        }
        return buildList {
            var index = 0
            data.forEach { node ->
                val chapter = parseChapter(node, index)
                if (!chapter.itemId.isNullOrBlank()) {
                    chapter.index = index
                    add(chapter)
                    index++
                }
            }
        }
    }

    /** 兼容旧签名：仅 bookUrl 无法拉目录，请使用 getChapterList(BookDTO) */
    @JvmStatic
    fun getChapterList(bookUrl: String): List<BookChapterDTO> {
        error("请使用 getChapterList(BookDTO)，当前 bookUrl=$bookUrl")
    }

    @JvmStatic
    fun getBookContent(book: BookDTO, index: Int): String {
        val chapters = me.kuku.legado.dao.CurrentReadData.bookChapterList
        require(index in chapters.indices) { "章节下标越界: $index / ${chapters.size}" }
        val chapter = chapters[index]
        val itemId = chapter.itemId
        require(!itemId.isNullOrBlank()) { "章节缺少 item_id" }
        val source = book.source
        require(!source.isNullOrBlank()) { "缺少 source" }
        val tab = book.tab?.ifBlank { DEFAULT_TAB } ?: DEFAULT_TAB
        val cacheKey = listOf(itemId, source, tab, DEFAULT_VERSION).joinToString("|")
        bookCache.getIfPresent(cacheKey)?.let { return it }

        // 与书源 ruleContent 一致：POST /content
        val body = objectMapper.createObjectNode().apply {
            put("html", "")
            put("item_id", itemId)
            put("source", source)
            put("tab", tab)
            put("tone_id", DEFAULT_TONE_ID)
            put("variable", DEFAULT_VARIABLE)
            put("version", DEFAULT_VERSION)
        }
        val jsonNode = postJson("/content", objectMapper.writeValueAsString(body))
        val code = jsonNode["code"]
        if (code != null && !code.isNull && code.asInt() != 0 && jsonNode["content"] == null) {
            error(textOf(jsonNode, "msg", "error").ifBlank { "获取正文失败" })
        }
        val msg = textOf(jsonNode, "msg")
        if (msg.isNotBlank() && extractContentText(jsonNode).isBlank()) {
            error(msg)
        }
        val raw = extractContentText(jsonNode)
        if (raw.isBlank()) {
            error(msg.ifBlank { "正文为空" })
        }
        val text = toPlainText(raw)
        bookCache.put(cacheKey, text)
        return text
    }

    /** 兼容旧签名 */
    @JvmStatic
    fun getBookContent(bookUrl: String, index: Int): String {
        val book = me.kuku.legado.dao.CurrentReadData.book
        return getBookContent(book, index)
    }

    @JvmStatic
    fun saveBookProgress(book: BookDTO, index: Int) {
        val shelfId = book.shelfId ?: return
        val chapters = me.kuku.legado.dao.CurrentReadData.bookChapterList
        if (index !in chapters.indices) return
        val chapter = chapters[index]
        val itemId = chapter.itemId ?: return
        val title = chapter.title.orEmpty()

        val payload = objectMapper.createObjectNode().apply {
            put("id", shelfId)
            put("last_chapter_item_id", itemId)
            put("last_chapter_title", title)
            put("last_chapter_update_time", System.currentTimeMillis() / 1000)
            if (!book.bookId.isNullOrBlank()) put("book_id", book.bookId)
            if (!book.source.isNullOrBlank()) put("source", book.source)
            if (!book.tab.isNullOrBlank()) put("tab", book.tab)
            put("read_status", 1)
        }
        try {
            postJson("/update_book_shelf", objectMapper.writeValueAsString(payload))
            book.lastChapterItemId = itemId
            book.durChapterTitle = title
            book.durChapterIndex = index
            book.latestChapterTitle = book.latestChapterTitle ?: title
        } catch (_: Exception) {
            // 进度同步失败不影响阅读
            if (settingsState.enableErrorLog) {
                // swallow, caller does not depend on result
            }
        }
    }

    /** 兼容旧签名 */
    @JvmStatic
    fun saveBookProgress(bookUrl: String, index: Int) {
        val book = me.kuku.legado.dao.CurrentReadData.book
        saveBookProgress(book, index)
    }

    /**
     * 根据 last_chapter_item_id 在目录中定位章节下标
     */
    @JvmStatic
    fun resolveChapterIndex(book: BookDTO, chapters: List<BookChapterDTO>): Int {
        val itemId = book.lastChapterItemId
        if (!itemId.isNullOrBlank()) {
            val found = chapters.indexOfFirst { it.itemId == itemId }
            if (found >= 0) return found
        }
        val byTitle = book.durChapterTitle ?: book.latestChapterTitle
        if (!byTitle.isNullOrBlank()) {
            val found = chapters.indexOfFirst { it.title == byTitle }
            if (found >= 0) return found
        }
        return 0
    }
}
