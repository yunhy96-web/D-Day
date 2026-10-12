CREATE TABLE IF NOT EXISTS visit (
  session_id TEXT PRIMARY KEY,
  first_seen TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS pick (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT NOT NULL,
  round      INTEGER NOT NULL,
  winner_id  TEXT NOT NULL,
  loser_id   TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_pick_round_winner ON pick(round, winner_id);
CREATE INDEX IF NOT EXISTS idx_pick_sid_round ON pick(session_id, round);

CREATE TABLE IF NOT EXISTS guestbook (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL,
  message     TEXT NOT NULL,
  pw_hash     TEXT NOT NULL,
  deleted_yn  TEXT NOT NULL DEFAULT 'N',
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS label (
  key  TEXT NOT NULL,
  lang TEXT NOT NULL,
  text TEXT NOT NULL,
  PRIMARY KEY (key, lang)
);

CREATE TABLE IF NOT EXISTS content (
  key        TEXT PRIMARY KEY,
  path       TEXT NOT NULL,
  sort       INTEGER NOT NULL,
  enabled_yn TEXT NOT NULL DEFAULT 'Y',
  icon       TEXT,
  stat_sql   TEXT
);

CREATE TABLE IF NOT EXISTS event (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT NOT NULL,
  name       TEXT NOT NULL,
  meta       TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_event_name ON event(name);

-- 설계 §15-4. 정답(answer_no)은 응답에 절대 포함하지 않는다
CREATE TABLE IF NOT EXISTS quiz (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  question   TEXT NOT NULL,
  choices    TEXT NOT NULL,
  answer_no  INTEGER NOT NULL,
  points     INTEGER NOT NULL CHECK (points BETWEEN 1 AND 5),
  hint       TEXT,
  deleted_yn TEXT NOT NULL DEFAULT 'N',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS quiz_attempt (
  id           TEXT PRIMARY KEY,
  session_id   TEXT NOT NULL,
  level        TEXT NOT NULL,
  quiz_ids     TEXT NOT NULL,
  max_score    INTEGER NOT NULL,
  score        INTEGER,
  submitted_at TEXT,
  created_at   TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS timeline (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  month      TEXT NOT NULL,
  file       TEXT NOT NULL,
  caption    TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_timeline_month ON timeline(month);

-- 설계 §15-6. 1번(sort 최소) = 메인. 원본은 비공개
CREATE TABLE IF NOT EXISTS photo (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  file        TEXT NOT NULL,
  original    TEXT NOT NULL,
  sort        INTEGER NOT NULL,
  worldcup_yn TEXT NOT NULL DEFAULT 'Y',
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

-- 설계 §15-7. guestbook_id NULL = 아직 글에 안 붙은 업로드
CREATE TABLE IF NOT EXISTS guestbook_photo (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  guestbook_id INTEGER,
  file         TEXT NOT NULL,
  created_at   TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_gbp_gb ON guestbook_photo(guestbook_id);
