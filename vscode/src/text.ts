/** HTML / 实体 → 纯文本，对齐 IntelliJ ApiUtils.toPlainText */
export function toPlainText(content: string): string {
  if (!content) {
    return content;
  }
  if (!content.includes("<")) {
    return decodeEntities(content);
  }
  return decodeEntities(
    content
      .replace(/<br\s*\/?>/gi, "\n")
      .replace(/<\/p\s*>/gi, "\n")
      .replace(/<\/div\s*>/gi, "\n")
      .replace(/<[^>]+>/g, "")
  )
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

function decodeEntities(content: string): string {
  return content
    .replace(/&nbsp;/g, " ")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&")
    .replace(/&quot;/g, '"');
}

export function textOf(node: unknown, ...keys: string[]): string {
  if (!node || typeof node !== "object") {
    return "";
  }
  const obj = node as Record<string, unknown>;
  for (const key of keys) {
    const child = obj[key];
    if (child == null) {
      continue;
    }
    const value = String(child);
    if (value.trim()) {
      return value;
    }
  }
  return "";
}

export function longOf(node: unknown, key: string): number | undefined {
  if (!node || typeof node !== "object") {
    return undefined;
  }
  const child = (node as Record<string, unknown>)[key];
  if (child == null) {
    return undefined;
  }
  if (typeof child === "number" && Number.isFinite(child)) {
    return child;
  }
  const n = Number(String(child));
  return Number.isFinite(n) ? n : undefined;
}

export function intOf(node: unknown, key: string): number | undefined {
  const n = longOf(node, key);
  return n == null ? undefined : Math.trunc(n);
}
