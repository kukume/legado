import * as vscode from "vscode";
import { sanitizeCookie } from "./cookie";
import { ApiConfig } from "./types";
import { clampChunkSize, DEFAULT_CHUNK_SIZE } from "./chunk";

export function getApiConfig(): ApiConfig {
  const c = vscode.workspace.getConfiguration("legado");
  return {
    address: (c.get<string>("address") || "https://api.langge.cf").trim(),
    cookie: sanitizeCookie(c.get<string>("cookie")),
    enableErrorLog: Boolean(c.get<boolean>("enableErrorLog")),
  };
}

export function getChunkSize(): number {
  const n = vscode.workspace.getConfiguration("legado").get<number>("inlineReadChunkSize");
  return clampChunkSize(n ?? DEFAULT_CHUNK_SIZE);
}

export function getBodyFont(): { color: string; size: number } {
  const c = vscode.workspace.getConfiguration("legado");
  const color = (c.get<string>("textBodyFontColor") || "").trim();
  const size = c.get<number>("textBodyFontSize") || 0;
  return { color, size };
}
