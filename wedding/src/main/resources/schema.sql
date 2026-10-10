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
