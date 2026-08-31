import * as http from "http";
import * as https from "https";
import { URL } from "url";
import { Book, BookChapter, ApiConfig } from "./types";
import { intOf, longOf, textOf, toPlainText } from "./text";

const DEFAULT_TAB = "小说";
const DEFAULT_VERSION = "4.11.5.1";
const DEFAULT_TONE_ID = "4";
const DEFAULT_VARIABLE = `{"custom":""}`;
const DEFAULT_UA =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36";

interface CacheEntry {
  value: string;
  expiresAt: number;
}

export class ApiClient {
  private readonly cache = new Map<string, CacheEntry>();
  private readonly cacheTtlMs = 10 * 60 * 1000;
  private readonly cacheMax = 20;

  constructor(private readonly configProvider: () => ApiConfig) {}

  private config(): ApiConfig {
    return this.configProvider();
  }

  private baseUrl(): string {
    const raw = this.config().address?.trim() || "https://api.langge.cf";
    return raw.replace(/\/+$/, "");
  }

  private ensureConfigured(): void {
    if (!this.config().cookie.trim()) {
      throw new Error("请先在设置中配置 Legado Reader 的 Cookie（搜索 legado.cookie）");
    }
  }

  private async request(path: string, init: { method?: string; headers?: Record<string, string>; body?: string } = {}): Promise<unknown> {
    this.ensureConfigured();
    const cfg = this.config();
    const base = this.baseUrl();
    const headers: Record<string, string> = {
      "User-Agent": DEFAULT_UA,
      Accept: "application/json, text/plain, */*",
      "Accept-Language": "zh-CN,zh;q=0.9",
      Origin: base,
      Referer: `${base}/online_search`,
      ...init.headers,
    };
    if (cfg.cookie.trim()) {
      headers.Cookie = cfg.cookie.trim();
    }
    if (init.body) {
      headers["Content-Length"] = String(Buffer.byteLength(init.body));
    }
    const { status, body } = await httpText(base + path, {
      method: init.method || "GET",
      headers,
      body: init.body,
    });
    if (status < 200 || status >= 300) {
      throw new Error(`HTTP ${status}: ${body.slice(0, 200)}`);
    }
    try {
      return JSON.parse(body) as unknown;
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      throw new Error(`接口返回非 JSON: ${msg}`);
    }
  }

  private async get(path: string, query: Record<string, string | undefined> = {}): Promise<unknown> {
    const params = new URLSearchParams();
    for (const [k, v] of Object.entries(query)) {
      if (v) {
        params.set(k, v);
      }
    }
    const qs = params.toString();
    return this.request(qs ? `${path}?${qs}` : path);
  }

  private async postJson(path: string, json: unknown): Promise<unknown> {
    return this.request(path, {
      method: "POST",
      headers: { "Content-Type": "application/json; charset=utf-8" },
      body: JSON.stringify(json),
    });
  }

  parseBook(node: unknown): Book {
    const latest = textOf(node, "last_chapter_title", "latestChapterTitle");
    return {
      shelfId: longOf(node, "id"),
      email: textOf(node, "email") || undefined,
      name: textOf(node, "book_name", "name"),
      bookId: textOf(node, "book_id"),
      catalogBookId: textOf(node, "book_id") || undefined,
      author: textOf(node, "author") || "未知作者",
      coverUrl: textOf(node, "thumb_url", "coverUrl") || undefined,
      intro: textOf(node, "abstract", "intro") || undefined,
      source: textOf(node, "source"),
      tab: textOf(node, "tab") || DEFAULT_TAB,
      kind: textOf(node, "category", "kind") || undefined,
      latestChapterTime: longOf(node, "last_chapter_update_time"),
      latestChapterTitle: latest || undefined,
      lastChapterItemId: textOf(node, "last_chapter_item_id") || undefined,
      status: textOf(node, "status") || undefined,
      readStatus: intOf(node, "read_status"),
      durChapterIndex: 0,
      durChapterPos: 0,
      durChapterTitle: latest || undefined,
    };
  }

  parseChapter(node: unknown, index: number): BookChapter {
    return {
      itemId: textOf(node, "item_id", "id", "chapter_id", "cid"),
      title: textOf(node, "title", "chapter_title", "name") || `第${index + 1}章`,
      index,
    };
  }

  extractContentText(payload: unknown): string {
    if (!payload || typeof payload !== "object") {
      return "";
    }
    const obj = payload as Record<string, unknown>;
    const contentNode = obj.content;
    if (contentNode != null) {
      if (typeof contentNode === "object") {
        const nested = textOf(contentNode, "content", "text", "html");
        if (nested) {
          return nested;
        }
      } else {
        const text = String(contentNode);
        if (text.trim()) {
          return text;
        }
      }
    }
    const data = obj.data;
    if (data != null) {
      if (Array.isArray(data) && data.length > 0) {
        const first = data[0];
        if (first && typeof first === "object") {
          const firstText = textOf(first, "content", "text", "html");
          if (firstText) {
            return firstText;
          }
        } else {
          return String(first);
        }
      } else if (typeof data === "object") {
        const text = textOf(data, "content", "text", "html");
        if (text) {
          return text;
        }
        const nestedList = (data as Record<string, unknown>).data;
        if (Array.isArray(nestedList) && nestedList.length > 0) {
          const firstText = textOf(nestedList[0], "content", "text", "html");
          if (firstText) {
            return firstText;
          }
        }
      } else if (typeof data === "string") {
        return data;
      }
    }
    return textOf(payload, "text", "html", "body");
  }

  async getBookshelf(): Promise<Book[]> {
    const json = (await this.get("/get_book_shelf")) as Record<string, unknown>;
    const data = json.data;
    if (data == null) {
      const code = json.code;
      if (typeof code === "number" && code !== 0) {
        throw new Error(textOf(json, "msg", "error", "errorMsg") || "获取书架失败");
      }
      return [];
    }
    if (!Array.isArray(data)) {
      throw new Error("书架数据格式错误");
    }
    const books: Book[] = [];
    for (const node of data) {
      const book = this.parseBook(node);
      if (book.readStatus != null && book.readStatus !== 1) {
        continue;
      }
      if (book.tab && book.tab !== DEFAULT_TAB) {
        continue;
      }
      books.push(book);
    }
    return books;
  }

  async resolveCatalogBookId(book: Book): Promise<string> {
    if (book.catalogBookId && book.catalogBookId !== book.bookId) {
      return book.catalogBookId;
    }
    if (!book.bookId) {
      throw new Error("缺少 book_id");
    }
    if (!book.source) {
      throw new Error("缺少 source");
    }
    const tab = book.tab || DEFAULT_TAB;
    const json = (await this.get("/detail", {
      book_id: book.bookId,
      source: book.source,
      tab,
      variable: DEFAULT_VARIABLE,
    })) as Record<string, unknown>;
    const code = json.code;
    if (typeof code === "number" && code !== 0) {
      return book.bookId;
    }
    const data = json.data;
    const resolved = textOf(data, "book_id") || book.bookId;
    book.catalogBookId = resolved;
    if (!book.name) {
      book.name = textOf(data, "book_name", "name");
    }
    if (!book.author) {
      book.author = textOf(data, "author");
    }
    if (!book.coverUrl) {
      book.coverUrl = textOf(data, "thumb_url") || undefined;
    }
    if (!book.intro) {
      book.intro = textOf(data, "abstract") || undefined;
    }
    return resolved;
  }

  async getChapterList(book: Book): Promise<BookChapter[]> {
    const catalogBookId = await this.resolveCatalogBookId(book);
    if (!book.source) {
      throw new Error("缺少 source");
    }
    const tab = book.tab || DEFAULT_TAB;
    const json = (await this.get("/catalog", {
      book_id: catalogBookId,
      source: book.source,
      tab,
      variable: DEFAULT_VARIABLE,
    })) as Record<string, unknown>;
    const code = json.code;
    if (typeof code === "number" && code !== 0) {
      throw new Error(textOf(json, "msg", "error") || "获取目录失败");
    }
    const data = json.data;
    if (!Array.isArray(data)) {
      throw new Error("目录数据格式错误");
    }
    const chapters: BookChapter[] = [];
    let index = 0;
    for (const node of data) {
      const chapter = this.parseChapter(node, index);
      if (chapter.itemId) {
        chapter.index = index;
        chapters.push(chapter);
        index++;
      }
    }
    return chapters;
  }

  private cacheGet(key: string): string | undefined {
    const hit = this.cache.get(key);
    if (!hit) {
      return undefined;
    }
    if (Date.now() > hit.expiresAt) {
      this.cache.delete(key);
      return undefined;
    }
    return hit.value;
  }

  private cachePut(key: string, value: string): void {
    if (this.cache.size >= this.cacheMax) {
      const first = this.cache.keys().next().value;
      if (first) {
        this.cache.delete(first);
      }
    }
    this.cache.set(key, { value, expiresAt: Date.now() + this.cacheTtlMs });
  }

  async getBookContent(book: Book, chapters: BookChapter[], index: number): Promise<string> {
    if (index < 0 || index >= chapters.length) {
      throw new Error(`章节下标越界: ${index} / ${chapters.length}`);
    }
    const chapter = chapters[index];
    if (!chapter.itemId) {
      throw new Error("章节缺少 item_id");
    }
    if (!book.source) {
      throw new Error("缺少 source");
    }
    const tab = book.tab || DEFAULT_TAB;
    const cacheKey = [chapter.itemId, book.source, tab, DEFAULT_VERSION].join("|");
    const cached = this.cacheGet(cacheKey);
    if (cached) {
      return cached;
    }
    const json = (await this.postJson("/content", {
      html: "",
      item_id: chapter.itemId,
      source: book.source,
      tab,
      tone_id: DEFAULT_TONE_ID,
      variable: DEFAULT_VARIABLE,
      version: DEFAULT_VERSION,
    })) as Record<string, unknown>;
    const code = json.code;
    if (typeof code === "number" && code !== 0 && json.content == null) {
      throw new Error(textOf(json, "msg", "error") || "获取正文失败");
    }
    const msg = textOf(json, "msg");
    const raw = this.extractContentText(json);
    if (msg && !raw.trim()) {
      throw new Error(msg);
    }
    if (!raw.trim()) {
      throw new Error(msg || "正文为空");
    }
    const text = toPlainText(raw);
    this.cachePut(cacheKey, text);
    return text;
  }

  async saveBookProgress(book: Book, chapters: BookChapter[], index: number): Promise<void> {
    if (book.shelfId == null) {
      return;
    }
    if (index < 0 || index >= chapters.length) {
      return;
    }
    const chapter = chapters[index];
    if (!chapter.itemId) {
      return;
    }
    const title = chapter.title || "";
    try {
      await this.postJson("/update_book_shelf", {
        id: book.shelfId,
        last_chapter_item_id: chapter.itemId,
        last_chapter_title: title,
        last_chapter_update_time: Math.floor(Date.now() / 1000),
        ...(book.bookId ? { book_id: book.bookId } : {}),
        ...(book.source ? { source: book.source } : {}),
        ...(book.tab ? { tab: book.tab } : {}),
        read_status: 1,
      });
      book.lastChapterItemId = chapter.itemId;
      book.durChapterTitle = title;
      book.durChapterIndex = index;
      book.latestChapterTitle = book.latestChapterTitle || title;
    } catch {
      // 进度同步失败不影响阅读
    }
  }

  resolveChapterIndex(book: Book, chapters: BookChapter[]): number {
    if (book.lastChapterItemId) {
      const found = chapters.findIndex((c) => c.itemId === book.lastChapterItemId);
      if (found >= 0) {
        return found;
      }
    }
    const byTitle = book.durChapterTitle || book.latestChapterTitle;
    if (byTitle) {
      const found = chapters.findIndex((c) => c.title === byTitle);
      if (found >= 0) {
        return found;
      }
    }
    return 0;
  }
}

const REQUEST_TIMEOUT_MS = 30_000;
const MAX_REDIRECTS = 5;

export function httpText(
  url: string,
  options: { method: string; headers: Record<string, string>; body?: string },
  redirects = 0
): Promise<{ status: number; body: string }> {
  return new Promise((resolve, reject) => {
    let u: URL;
    try {
      u = new URL(url);
    } catch {
      reject(new Error(`无效 URL: ${url}`));
      return;
    }
    const lib = u.protocol === "https:" ? https : http;
    const req = lib.request(
      {
        protocol: u.protocol,
        hostname: u.hostname,
        port: u.port,
        path: `${u.pathname}${u.search}`,
        method: options.method,
        headers: options.headers,
      },
      (res) => {
        const status = res.statusCode || 0;
        const location = res.headers.location;
        if (status >= 300 && status < 400 && location) {
          res.resume();
          if (redirects >= MAX_REDIRECTS) {
            reject(new Error("重定向过多"));
            return;
          }
          const nextUrl = new URL(location, url).toString();
          const nextMethod = status === 307 || status === 308 ? options.method : "GET";
          const nextBody = nextMethod === "GET" ? undefined : options.body;
          const headers = { ...options.headers };
          if (nextMethod === "GET") {
            delete headers["Content-Length"];
            delete headers["Content-Type"];
          }
          httpText(nextUrl, { method: nextMethod, headers, body: nextBody }, redirects + 1).then(
            resolve,
            reject
          );
          return;
        }
        const chunks: Buffer[] = [];
        res.on("data", (chunk: Buffer) => chunks.push(chunk));
        res.on("end", () => {
          resolve({ status, body: Buffer.concat(chunks).toString("utf8") });
        });
      }
    );
    req.setTimeout(REQUEST_TIMEOUT_MS, () => {
      req.destroy(new Error("请求超时（30s），请检查网络或 API 地址"));
    });
    req.on("error", reject);
    if (options.body) {
      req.write(options.body);
    }
    req.end();
  });
}
