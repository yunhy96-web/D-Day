-- 설계 §9. 시각은 전부 UTC ISO-8601('2026-10-14T05:00:00Z'), 날짜만 있는 값은 'yyyy-MM-dd'
-- 이 파일은 고치지 않는다. 변경은 V2__..., V3__... 로 추가.

CREATE TABLE family (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  name       TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);

CREATE TABLE member (
  id                INTEGER PRIMARY KEY AUTOINCREMENT,
  login_id          TEXT NOT NULL UNIQUE,
  password_hash     TEXT NOT NULL,
  name              TEXT NOT NULL,
  avatar_key        TEXT,
  lang              TEXT NOT NULL DEFAULT 'ko',          -- ko | th
  theme             TEXT NOT NULL DEFAULT 'system',      -- system | light | dark
  font_scale        TEXT NOT NULL DEFAULT 'md',          -- sm | md | lg
  font_scale_set_yn TEXT NOT NULL DEFAULT 'N',           -- 첫 로그인 글씨 크기 선택을 했는지
  color             TEXT NOT NULL DEFAULT 'blue',        -- 가족 캘린더에서 이 사람 색
  timezone          TEXT NOT NULL DEFAULT 'Asia/Seoul',
  must_change_pw    TEXT NOT NULL DEFAULT 'N',
  last_seen_at      TEXT,
  legacy_consent_at TEXT,                                -- 설계 §15-3
  created_at        TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);

CREATE TABLE family_member (
  family_id  INTEGER NOT NULL REFERENCES family(id),
  member_id  INTEGER NOT NULL REFERENCES member(id),
  status     TEXT NOT NULL DEFAULT 'PENDING',            -- PENDING | ACTIVE | BLOCKED
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
  PRIMARY KEY (family_id, member_id)
);

CREATE TABLE role (
  code      TEXT PRIMARY KEY,
  label_key TEXT NOT NULL
);
CREATE TABLE role_permission (
  role_code  TEXT NOT NULL REFERENCES role(code),
  permission TEXT NOT NULL,                              -- '*' = 전부
  PRIMARY KEY (role_code, permission)
);
CREATE TABLE member_role (
  family_id  INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  role_code  TEXT NOT NULL REFERENCES role(code),
  granted_by INTEGER,
  granted_at TEXT NOT NULL,
  PRIMARY KEY (family_id, member_id, role_code)
);

INSERT INTO role(code, label_key) VALUES ('ADMIN', 'role.ADMIN'), ('MEMBER', 'role.MEMBER'), ('VIEWER', 'role.VIEWER');
INSERT INTO role_permission(role_code, permission) VALUES
  ('ADMIN', '*'),
  ('MEMBER', 'post.write'), ('MEMBER', 'comment.write'), ('MEMBER', 'media.upload'), ('MEMBER', 'shift.write');

CREATE TABLE audit_log (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id  INTEGER,
  member_id  INTEGER,
  action     TEXT NOT NULL,
  target     TEXT,
  ip         TEXT,
  created_at TEXT NOT NULL
);
CREATE INDEX ix_audit ON audit_log(family_id, created_at DESC);

CREATE TABLE session (
  token_hash TEXT PRIMARY KEY,                           -- 쿠키 원문의 SHA-256
  member_id  INTEGER NOT NULL,
  family_id  INTEGER NOT NULL,
  expires_at TEXT NOT NULL
);
CREATE INDEX ix_session_member ON session(member_id);

CREATE TABLE label (
  key  TEXT NOT NULL,
  lang TEXT NOT NULL,
  text TEXT NOT NULL,
  PRIMARY KEY (key, lang)
);

CREATE TABLE post (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id  INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  kind       TEXT NOT NULL,                              -- moment | diary
  title      TEXT,
  body       TEXT NOT NULL DEFAULT '',
  visibility TEXT NOT NULL DEFAULT 'family',             -- family | private
  post_date  TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT,
  deleted_at TEXT,
  deleted_by INTEGER
);
CREATE INDEX ix_post_feed ON post(family_id, post_date DESC, id DESC) WHERE deleted_at IS NULL;

CREATE TABLE media (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id   INTEGER NOT NULL,
  member_id   INTEGER NOT NULL,
  post_id     INTEGER,                                   -- NULL = 글 저장 전(하루 지나면 정리)
  kind        TEXT NOT NULL,                             -- photo | audio | video
  file        TEXT NOT NULL,                             -- uuid
  original    TEXT NOT NULL,                             -- uuid.ext
  mime        TEXT NOT NULL,
  size_bytes  INTEGER NOT NULL,
  width       INTEGER,
  height      INTEGER,
  duration_ms INTEGER,
  sha256      TEXT NOT NULL,
  taken_at    TEXT,
  sort        INTEGER NOT NULL DEFAULT 0,
  created_at  TEXT NOT NULL
);
CREATE INDEX ix_media_post ON media(post_id);
CREATE INDEX ix_media_album ON media(family_id, kind, taken_at DESC);
CREATE INDEX ix_media_sha ON media(family_id, sha256);

CREATE TABLE comment (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  post_id    INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  body       TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT,
  deleted_at TEXT,
  deleted_by INTEGER
);
CREATE INDEX ix_comment_post ON comment(post_id);

CREATE TABLE reaction (
  post_id    INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  created_at TEXT NOT NULL,
  PRIMARY KEY (post_id, member_id)
);

CREATE TABLE shift_type (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id  INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  name       TEXT NOT NULL,
  short      TEXT NOT NULL,
  color      TEXT NOT NULL,
  start_time TEXT,
  end_time   TEXT,
  sort       INTEGER NOT NULL DEFAULT 0,
  deleted_at TEXT
);
CREATE INDEX ix_shift_type_member ON shift_type(family_id, member_id);

CREATE TABLE shift_setting (
  family_id     INTEGER NOT NULL,
  member_id     INTEGER NOT NULL,
  system        TEXT NOT NULL,                           -- 2 | 3 | 4 | custom
  pattern       TEXT,                                    -- JSON [shift_type.id, ...]
  pattern_start TEXT,
  updated_at    TEXT NOT NULL,
  PRIMARY KEY (family_id, member_id)
);

CREATE TABLE shift_day (
  family_id     INTEGER NOT NULL,
  member_id     INTEGER NOT NULL,
  day           TEXT NOT NULL,
  shift_type_id INTEGER,
  note          TEXT,
  updated_at    TEXT NOT NULL,
  updated_by    INTEGER NOT NULL,
  PRIMARY KEY (family_id, member_id, day)
);
CREATE INDEX ix_shift_family_day ON shift_day(family_id, day);
