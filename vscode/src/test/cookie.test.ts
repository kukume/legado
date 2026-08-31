import assert from "node:assert/strict";
import { describe, it } from "node:test";
import { isCookieConfigured, sanitizeCookie } from "../cookie";

describe("sanitizeCookie", () => {
  it("trims and strips wrapping quotes", () => {
    assert.equal(sanitizeCookie('  "qttoken=abc; deviceId=1"  '), "qttoken=abc; deviceId=1");
    assert.equal(sanitizeCookie("'qttoken=abc'"), "qttoken=abc");
    assert.equal(sanitizeCookie("qttoken=abc"), "qttoken=abc");
  });

  it("treats blank as unconfigured", () => {
    assert.equal(isCookieConfigured(""), false);
    assert.equal(isCookieConfigured('""'), false);
    assert.equal(isCookieConfigured("qttoken=x"), true);
  });
});

describe("webview loading merge", () => {
  it("clears loading when the host omits the field", () => {
    const prev = { loading: true, error: "", bookshelf: [{ name: "书" }] };
    const rest = { error: "", bookshelf: [{ name: "书" }] };
    const next = { ...prev, ...rest };
    if (!Object.prototype.hasOwnProperty.call(rest, "loading")) {
      next.loading = false;
    }
    assert.equal(next.loading, false);
    assert.equal(next.bookshelf.length, 1);
  });
});
