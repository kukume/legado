import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { ApiClient } from "../api";
import { toPlainText } from "../text";

const api = new ApiClient(() => ({
  address: "https://example.test",
  cookie: "x=1",
  enableErrorLog: false,
}));

describe("toPlainText", () => {
  it("strips tags and decodes entities", () => {
    assert.equal(toPlainText("<p>你好&nbsp;世界</p><br/>下一行"), "你好 世界\n\n下一行");
  });

  it("keeps plain text", () => {
    assert.equal(toPlainText("abc&amp;d"), "abc&d");
  });
});

describe("parseBook", () => {
  it("maps langge shelf fields", () => {
    const book = api.parseBook({
      id: 9,
      book_name: "书名",
      book_id: "b1",
      author: "作者",
      source: "src",
      tab: "小说",
      last_chapter_title: "第3章",
      last_chapter_item_id: "c3",
      read_status: 1,
    });
    assert.equal(book.shelfId, 9);
    assert.equal(book.name, "书名");
    assert.equal(book.bookId, "b1");
    assert.equal(book.author, "作者");
    assert.equal(book.lastChapterItemId, "c3");
    assert.equal(book.durChapterTitle, "第3章");
  });
});

describe("parseChapter", () => {
  it("falls back to numbered title", () => {
    const ch = api.parseChapter({ item_id: "x" }, 2);
    assert.equal(ch.itemId, "x");
    assert.equal(ch.title, "第3章");
  });
});

describe("extractContentText", () => {
  it("reads nested content", () => {
    assert.equal(api.extractContentText({ content: { text: "正文" } }), "正文");
  });

  it("reads data array", () => {
    assert.equal(api.extractContentText({ data: [{ html: "<p>Hi</p>" }] }), "<p>Hi</p>");
  });
});

describe("resolveChapterIndex", () => {
  const chapters = [
    { itemId: "a", title: "一", index: 0 },
    { itemId: "b", title: "二", index: 1 },
  ];

  it("matches last_chapter_item_id", () => {
    const idx = api.resolveChapterIndex(
      {
        name: "n",
        bookId: "1",
        author: "a",
        source: "s",
        tab: "小说",
        lastChapterItemId: "b",
        durChapterIndex: 0,
        durChapterPos: 0,
      },
      chapters
    );
    assert.equal(idx, 1);
  });

  it("falls back to title then zero", () => {
    assert.equal(
      api.resolveChapterIndex(
        {
          name: "n",
          bookId: "1",
          author: "a",
          source: "s",
          tab: "小说",
          durChapterTitle: "一",
          durChapterIndex: 0,
          durChapterPos: 0,
        },
        chapters
      ),
      0
    );
    assert.equal(
      api.resolveChapterIndex(
        {
          name: "n",
          bookId: "1",
          author: "a",
          source: "s",
          tab: "小说",
          durChapterIndex: 0,
          durChapterPos: 0,
        },
        chapters
      ),
      0
    );
  });
});
