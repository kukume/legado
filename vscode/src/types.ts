export interface Book {
  shelfId?: number;
  email?: string;
  name: string;
  bookId: string;
  catalogBookId?: string;
  author: string;
  coverUrl?: string;
  intro?: string;
  source: string;
  tab: string;
  kind?: string;
  latestChapterTime?: number;
  latestChapterTitle?: string;
  lastChapterItemId?: string;
  status?: string;
  readStatus?: number;
  durChapterIndex: number;
  durChapterPos: number;
  durChapterTitle?: string;
}

export interface BookChapter {
  itemId: string;
  title: string;
  index: number;
}

export interface ApiConfig {
  address: string;
  cookie: string;
  enableErrorLog: boolean;
}

export type ChapterOption =
  | { type: "chapter"; index: number; title: string }
  | { type: "morePrev"; remaining: number }
  | { type: "moreNext"; remaining: number };
