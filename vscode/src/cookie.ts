/** Strip wrapping quotes that Settings JSON sometimes keeps around a pasted cookie. */
export function sanitizeCookie(raw: string | undefined | null): string {
  let cookie = (raw ?? "").trim();
  if (
    (cookie.startsWith('"') && cookie.endsWith('"')) ||
    (cookie.startsWith("'") && cookie.endsWith("'"))
  ) {
    cookie = cookie.slice(1, -1).trim();
  }
  return cookie;
}

export function isCookieConfigured(raw: string | undefined | null): boolean {
  return sanitizeCookie(raw).length > 0;
}
