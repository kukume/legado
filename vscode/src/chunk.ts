export const DEFAULT_CHUNK_SIZE = 80;
export const PRELOAD_REMAINING_CHUNKS = 2;

export function clampChunkSize(n: number | undefined): number {
  if (n == null || !Number.isFinite(n) || n < 1 || n > 500) {
    return DEFAULT_CHUNK_SIZE;
  }
  return Math.trunc(n);
}

export function plainBody(raw: string): string {
  return raw.replace(/\r\n/g, "\n").replace(/\r/g, "\n").trim();
}

export function maxChunkIndex(text: string, chunkSize: number): number {
  if (!text) {
    return 0;
  }
  return Math.floor((text.length - 1) / chunkSize);
}

export function chunkText(text: string, chunkIndex: number, chunkSize: number): string {
  if (!text) {
    return "";
  }
  const max = maxChunkIndex(text, chunkSize);
  const index = Math.min(Math.max(chunkIndex, 0), max);
  const start = index * chunkSize;
  const end = Math.min(start + chunkSize, text.length);
  return text.slice(start, end).replace(/\n/g, " ");
}

export function formatChunkLabel(text: string, chunkIndex: number, chunkSize: number): string {
  const body = plainBody(text);
  const chunk = chunkText(body, chunkIndex, chunkSize);
  if (!chunk.trim()) {
    return "（无内容）";
  }
  const max = maxChunkIndex(body, chunkSize);
  return `${chunk} ·${chunkIndex + 1}/${max + 1}`;
}
