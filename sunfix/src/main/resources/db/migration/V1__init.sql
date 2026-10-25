-- 설계 §9. 시각은 UTC ISO 문자열, 금액은 원 단위 정수.

CREATE TABLE common_code (
  code        TEXT PRIMARY KEY,                -- 'WALLPAPER_AREA_20'
  group_code  TEXT NOT NULL,                   -- CATEGORY | OPT_GROUP | OPT | REGION | REGION_SUB
  parent_code TEXT REFERENCES common_code(code),
  sort_no     INTEGER NOT NULL DEFAULT 0,
  use_yn      TEXT NOT NULL DEFAULT 'Y'
);
CREATE INDEX ix_code_parent ON common_code(parent_code, sort_no);

CREATE TABLE label (
  key  TEXT NOT NULL,                          -- 'home.title', 'code.WALLPAPER'
  lang TEXT NOT NULL,                          -- ko | en
  text TEXT NOT NULL,
  PRIMARY KEY (key, lang)
);

CREATE TABLE vendor (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL,
  name_en     TEXT,
  phone       TEXT,
  address     TEXT NOT NULL,
  intro       TEXT,
  sample_yn   TEXT NOT NULL DEFAULT 'Y',
  use_yn      TEXT NOT NULL DEFAULT 'Y',
  created_at  TEXT NOT NULL
);

CREATE TABLE vendor_region (                   -- 출장 가능 지역. REGION(시/도 전체) 또는 REGION_SUB
  vendor_id   INTEGER NOT NULL REFERENCES vendor(id),
  region_code TEXT NOT NULL REFERENCES common_code(code),
  PRIMARY KEY (vendor_id, region_code)
);

CREATE TABLE vendor_category (
  vendor_id     INTEGER NOT NULL REFERENCES vendor(id),
  category_code TEXT NOT NULL REFERENCES common_code(code),
  base_price    INTEGER,                       -- NULL = 미공개
  PRIMARY KEY (vendor_id, category_code)
);

CREATE TABLE vendor_option_price (
  vendor_id   INTEGER NOT NULL REFERENCES vendor(id),
  option_code TEXT NOT NULL REFERENCES common_code(code),
  price       INTEGER NOT NULL,                -- 추가금. 행이 없으면 미공개
  updated_at  TEXT NOT NULL,
  PRIMARY KEY (vendor_id, option_code)
);

CREATE TABLE inquiry (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  vendor_id     INTEGER NOT NULL REFERENCES vendor(id),
  category_code TEXT NOT NULL,
  option_codes  TEXT NOT NULL,                 -- 정렬된 'A,B,C'
  region_code   TEXT NOT NULL,
  quoted_price  INTEGER,                       -- 문의 시점 서버 계산 총액. 미공개면 NULL
  name          TEXT NOT NULL,
  phone         TEXT NOT NULL,
  memo          TEXT,
  status        TEXT NOT NULL DEFAULT 'NEW',   -- NEW | DONE
  ip            TEXT,
  created_at    TEXT NOT NULL
);

CREATE TABLE quote_report (                    -- 견적 제보(설계 §7)
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  category_code TEXT NOT NULL REFERENCES common_code(code),
  option_codes  TEXT NOT NULL,                 -- 정렬된 'A,B,C' → 같은 조합 비교가 문자열 비교
  region_code   TEXT NOT NULL REFERENCES common_code(code),
  vendor_id     INTEGER REFERENCES vendor(id),
  vendor_name   TEXT,                          -- 직접 입력(비공개)
  price         INTEGER NOT NULL,
  quoted_ym     TEXT NOT NULL,                 -- '2026-10'
  memo          TEXT,
  status        TEXT NOT NULL DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED
  ip            TEXT,
  created_at    TEXT NOT NULL
);
CREATE INDEX ix_report_lookup ON quote_report(category_code, status, option_codes);

CREATE TABLE admin_session (
  token_hash TEXT PRIMARY KEY,                 -- SHA-256(쿠키 토큰)
  expires_at TEXT NOT NULL
);
