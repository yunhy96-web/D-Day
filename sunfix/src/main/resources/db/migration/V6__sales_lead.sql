-- 영업 리드(설계 §8-1): 가격을 공개한 실제 업체 목록. 실제 연락처라 데이터는 레포에 넣지 않고
-- docs/private/*.json 을 scripts/import-leads.sh 로 관리자 API 에 올린다.
CREATE TABLE sales_lead (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  category_code TEXT NOT NULL,                 -- 리프 카테고리(AIRCON, DRAIN …)
  name          TEXT NOT NULL,
  region        TEXT,                          -- 사업장·출장 지역
  channel       TEXT,                          -- 당근·크몽·홈페이지·본사
  prices        TEXT,                          -- 공개 가격 요약
  conditions    TEXT,                          -- 추가 비용·조건
  contact       TEXT,                          -- 업체가 사업용으로 공개한 연락처만
  source_url    TEXT,
  source_date   TEXT,                          -- 출처 게시·수정일
  priority      INTEGER NOT NULL DEFAULT 2,    -- 1 먼저 연락 | 2 보통 | 3 보류
  reason        TEXT,                          -- 추천·보류 이유
  status        TEXT NOT NULL DEFAULT 'TODO',  -- TODO | CONTACTED | AGREED | DECLINED
  memo          TEXT,                          -- 통화 메모(관리자)
  vendor_id     INTEGER REFERENCES vendor(id), -- 입점하면 연결
  checked_at    TEXT NOT NULL,                 -- 가격 확인일
  updated_at    TEXT NOT NULL,
  UNIQUE (category_code, name)                 -- 가져오기를 다시 돌려도 중복 안 됨
);
