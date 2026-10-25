-- 시장 공개 가격표(2026-10-25 조사, docs/private/leads-2026-10.md)에 옵션 구조를 맞춘다.
-- 코드는 지우지 않는다(제보·문의가 옵션 코드를 문자열로 들고 있음). 이름 변경은 라벨 UPDATE.

-- ── 에어컨 ───────────────────────────────────────────────
-- 천장형을 1~2way / 4way 로. 거의 모든 업체가 따로 매긴다(보통 4~6만 원 차이)
UPDATE label SET text = '천장형 1~2way' WHERE key = 'code.AIRCON_TYPE_CEIL' AND lang = 'ko';
UPDATE label SET text = 'Ceiling 1–2 way' WHERE key = 'code.AIRCON_TYPE_CEIL' AND lang = 'en';
INSERT INTO common_code(code, group_code, parent_code, sort_no) VALUES ('AIRCON_TYPE_CEIL4', 'OPT', 'AIRCON_TYPE', 5);
INSERT OR IGNORE INTO label(key, lang, text) VALUES ('code.AIRCON_TYPE_CEIL4', 'ko', '천장형 4way'), ('code.AIRCON_TYPE_CEIL4', 'en', 'Ceiling 4 way');

-- '일반 세척 / 완전 분해' → 실제 업체들이 나누는 경계인 송풍팬 분해 여부
UPDATE label SET text = '송풍팬 분해' WHERE key = 'code.AIRCON_LEVEL' AND lang = 'ko';
UPDATE label SET text = 'Blower fan' WHERE key = 'code.AIRCON_LEVEL' AND lang = 'en';
UPDATE label SET text = '분해 세척(송풍팬 제외)' WHERE key = 'code.AIRCON_LEVEL_BASIC' AND lang = 'ko';
UPDATE label SET text = 'Standard (fan not removed)' WHERE key = 'code.AIRCON_LEVEL_BASIC' AND lang = 'en';
UPDATE label SET text = '송풍팬까지 분해' WHERE key = 'code.AIRCON_LEVEL_FULL' AND lang = 'ko';
UPDATE label SET text = 'Including blower fan' WHERE key = 'code.AIRCON_LEVEL_FULL' AND lang = 'en';

-- 실외기: 기본가에 포함한 업체가 없어 따로 고른다
INSERT INTO common_code(code, group_code, parent_code, sort_no) VALUES
  ('AIRCON_OUTDOOR', 'OPT_GROUP', 'AIRCON', 3),
  ('AIRCON_OUTDOOR_NO', 'OPT', 'AIRCON_OUTDOOR', 1),
  ('AIRCON_OUTDOOR_YES', 'OPT', 'AIRCON_OUTDOOR', 2);
INSERT OR IGNORE INTO label(key, lang, text) VALUES
  ('code.AIRCON_OUTDOOR', 'ko', '실외기'), ('code.AIRCON_OUTDOOR', 'en', 'Outdoor unit'),
  ('code.AIRCON_OUTDOOR_NO', 'ko', '안 함'), ('code.AIRCON_OUTDOOR_NO', 'en', 'Not included'),
  ('code.AIRCON_OUTDOOR_YES', 'ko', '실외기도 세척'), ('code.AIRCON_OUTDOOR_YES', 'en', 'Clean outdoor unit too');
UPDATE label SET text = text || ', 1way, 4way, 천장형, 시스템, 실외기, 송풍팬, 무풍' WHERE key = 'kw.AIRCON' AND lang = 'ko';
UPDATE label SET text = text || ', ceiling, cassette, outdoor unit, fan' WHERE key = 'kw.AIRCON' AND lang = 'en';

-- ── 배수구·변기 막힘 ─────────────────────────────────────
UPDATE label SET text = '배수구·변기 막힘' WHERE key = 'code.DRAIN' AND lang = 'ko';
UPDATE label SET text = 'Clogged drain / toilet' WHERE key = 'code.DRAIN' AND lang = 'en';
UPDATE label SET text = '고압 세척(시작가)' WHERE key = 'code.DRAIN_LEVEL_JET' AND lang = 'ko';
UPDATE label SET text = 'High-pressure jetting (from)' WHERE key = 'code.DRAIN_LEVEL_JET' AND lang = 'en';
-- 변기 이물질 회수(탈거·재설치)는 일반 뚫음과 가격대가 완전히 다르다(15~30만 원)
INSERT INTO common_code(code, group_code, parent_code, sort_no) VALUES ('DRAIN_LEVEL_REMOVE', 'OPT', 'DRAIN_LEVEL', 3);
INSERT OR IGNORE INTO label(key, lang, text) VALUES
  ('code.DRAIN_LEVEL_REMOVE', 'ko', '변기 이물질 회수(탈거)'), ('code.DRAIN_LEVEL_REMOVE', 'en', 'Toilet object removal');
UPDATE label SET text = '고압 세척(시작가)' WHERE key = 'code.KITCHEN_DRAIN_LEVEL_JET' AND lang = 'ko';
UPDATE label SET text = 'High-pressure jetting (from)' WHERE key = 'code.KITCHEN_DRAIN_LEVEL_JET' AND lang = 'en';
UPDATE label SET text = text || ', 이물질, 휴지, 물티슈, 장난감' WHERE key = 'kw.DRAIN' AND lang = 'ko';

-- ── 예시 업체(sample_yn='Y') 가격을 시장 중앙값에 맞춤 ─────────
-- 업체별 ±10% 남짓 차이: 0.90 + (id*37 % 25)/100. 1000원 단위. 실제 업체(sample_yn='N')는 건드리지 않는다
-- 에어컨: 기본가 0 + 종류(벽걸이 7만·스탠드 12만·2in1 17만·1~2way 9만·4way 14만) + 송풍팬 3만 + 실외기 5.5만
UPDATE vendor_option_price SET updated_at = '2026-10-25T00:00:00Z', price = CAST(ROUND(
    (CASE option_code WHEN 'AIRCON_TYPE_WALL' THEN 70000 WHEN 'AIRCON_TYPE_STAND' THEN 120000 WHEN 'AIRCON_TYPE_TWO' THEN 170000
                      WHEN 'AIRCON_TYPE_CEIL' THEN 90000 WHEN 'AIRCON_LEVEL_BASIC' THEN 0 WHEN 'AIRCON_LEVEL_FULL' THEN 30000 END)
    * (0.90 + (vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER)
WHERE option_code IN ('AIRCON_TYPE_WALL','AIRCON_TYPE_STAND','AIRCON_TYPE_TWO','AIRCON_TYPE_CEIL','AIRCON_LEVEL_BASIC','AIRCON_LEVEL_FULL')
  AND vendor_id IN (SELECT id FROM vendor WHERE sample_yn = 'Y');
-- 새 옵션 가격은 벽걸이 가격을 공개한 예시 업체에만(일부 미공개가 남게)
INSERT OR IGNORE INTO vendor_option_price(vendor_id, option_code, price, updated_at)
SELECT p.vendor_id, o.code, CAST(ROUND(o.base * (0.90 + (p.vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER), '2026-10-25T00:00:00Z'
FROM vendor_option_price p JOIN vendor v ON v.id = p.vendor_id AND v.sample_yn = 'Y'
JOIN (SELECT 'AIRCON_TYPE_CEIL4' AS code, 140000 AS base UNION ALL SELECT 'AIRCON_OUTDOOR_NO', 0 UNION ALL SELECT 'AIRCON_OUTDOOR_YES', 55000) o
WHERE p.option_code = 'AIRCON_TYPE_WALL';
UPDATE vendor_category SET base_price = 0
WHERE category_code = 'AIRCON' AND base_price IS NOT NULL AND vendor_id IN (SELECT id FROM vendor WHERE sample_yn = 'Y');

-- 배수구·변기: 기본가 5만 + 바닥 1만 / 고압 6만 / 이물질 회수 15만
UPDATE vendor_category SET base_price = CAST(ROUND(50000 * (0.90 + (vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER)
WHERE category_code = 'DRAIN' AND base_price IS NOT NULL AND vendor_id IN (SELECT id FROM vendor WHERE sample_yn = 'Y');
UPDATE vendor_option_price SET updated_at = '2026-10-25T00:00:00Z', price = CAST(ROUND(
    (CASE option_code WHEN 'DRAIN_PLACE_BASIN' THEN 0 WHEN 'DRAIN_PLACE_TOILET' THEN 0 WHEN 'DRAIN_PLACE_FLOOR' THEN 10000
                      WHEN 'DRAIN_LEVEL_BASIC' THEN 0 WHEN 'DRAIN_LEVEL_JET' THEN 60000 END)
    * (0.90 + (vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER)
WHERE option_code IN ('DRAIN_PLACE_BASIN','DRAIN_PLACE_TOILET','DRAIN_PLACE_FLOOR','DRAIN_LEVEL_BASIC','DRAIN_LEVEL_JET')
  AND vendor_id IN (SELECT id FROM vendor WHERE sample_yn = 'Y');
INSERT OR IGNORE INTO vendor_option_price(vendor_id, option_code, price, updated_at)
SELECT p.vendor_id, 'DRAIN_LEVEL_REMOVE', CAST(ROUND(150000 * (0.90 + (p.vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER), '2026-10-25T00:00:00Z'
FROM vendor_option_price p JOIN vendor v ON v.id = p.vendor_id AND v.sample_yn = 'Y'
WHERE p.option_code = 'DRAIN_LEVEL_BASIC';

-- 싱크대 막힘: 기본가 5.5만 + 고압 6만
UPDATE vendor_category SET base_price = CAST(ROUND(55000 * (0.90 + (vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER)
WHERE category_code = 'KITCHEN_DRAIN' AND base_price IS NOT NULL AND vendor_id IN (SELECT id FROM vendor WHERE sample_yn = 'Y');
UPDATE vendor_option_price SET updated_at = '2026-10-25T00:00:00Z', price = CAST(ROUND(60000 * (0.90 + (vendor_id * 37 % 25) / 100.0) / 1000) * 1000 AS INTEGER)
WHERE option_code = 'KITCHEN_DRAIN_LEVEL_JET' AND vendor_id IN (SELECT id FROM vendor WHERE sample_yn = 'Y');

-- 기존 에어컨 제보·문의는 실외기를 말하지 않았으니 '안 함' 으로 보고 조합 키에 끼운다(정렬 키 유지: LEVEL < OUTDOOR < TYPE)
UPDATE quote_report SET option_codes = REPLACE(option_codes, ',AIRCON_TYPE', ',AIRCON_OUTDOOR_NO,AIRCON_TYPE')
WHERE category_code = 'AIRCON' AND option_codes NOT LIKE '%AIRCON_OUTDOOR%';
UPDATE inquiry SET option_codes = REPLACE(option_codes, ',AIRCON_TYPE', ',AIRCON_OUTDOOR_NO,AIRCON_TYPE')
WHERE category_code = 'AIRCON' AND option_codes NOT LIKE '%AIRCON_OUTDOOR%';
