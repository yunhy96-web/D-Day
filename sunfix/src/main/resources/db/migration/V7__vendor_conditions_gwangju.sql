-- 업체 공통 조건(설계 §5-1): 가격 못지않게 업체를 고르는 기준. NULL = 업체가 밝히지 않음(화면에 안 보임)
ALTER TABLE vendor ADD COLUMN vat        TEXT;     -- INCL 포함 | EXCL 별도
ALTER TABLE vendor ADD COLUMN trip_fee   INTEGER;  -- 출장비. 0 = 무료
ALTER TABLE vendor ADD COLUMN fail_fee   INTEGER;  -- 작업 불가·미해결 시 비용. 0 = 못 고치면 0원
ALTER TABLE vendor ADD COLUMN surcharge  TEXT;     -- 야간·주말 할증 Y | N
ALTER TABLE vendor ADD COLUMN extra_note TEXT;     -- 그 밖의 추가금(업소·층고·신형 기종 등)

-- 예시 업체에도 조건을 조금씩(화면 확인용). 실제 업체(sample_yn='N')는 건드리지 않는다
UPDATE vendor SET
  vat = CASE id % 3 WHEN 0 THEN 'INCL' WHEN 1 THEN 'EXCL' END,
  trip_fee = CASE id % 4 WHEN 0 THEN 0 WHEN 1 THEN 20000 WHEN 2 THEN 0 END,
  fail_fee = CASE WHEN id % 2 = 0 THEN 0 ELSE 20000 END,
  surcharge = CASE id % 3 WHEN 0 THEN 'N' WHEN 2 THEN 'Y' END,
  extra_note = CASE id % 5 WHEN 0 THEN '업소·음식점 +2만 원, 층고 3m 이상 +2만 원' WHEN 3 THEN '신형(무풍·스마트) 기종 +1~2만 원' END
WHERE sample_yn = 'Y';

-- 광주광역시(운영자 부모님 거주지)
INSERT INTO common_code(code, group_code, parent_code, sort_no) VALUES
  ('GWANGJU', 'REGION', NULL, 6),
  ('GWANGJU_DONG', 'REGION_SUB', 'GWANGJU', 1), ('GWANGJU_SEO', 'REGION_SUB', 'GWANGJU', 2), ('GWANGJU_NAM', 'REGION_SUB', 'GWANGJU', 3),
  ('GWANGJU_BUK', 'REGION_SUB', 'GWANGJU', 4), ('GWANGJU_GWANGSAN', 'REGION_SUB', 'GWANGJU', 5);
INSERT OR IGNORE INTO label(key, lang, text) VALUES
  ('code.GWANGJU', 'ko', '광주'), ('code.GWANGJU', 'en', 'Gwangju'),
  ('code.GWANGJU_DONG', 'ko', '동구'), ('code.GWANGJU_DONG', 'en', 'Dong-gu'),
  ('code.GWANGJU_SEO', 'ko', '서구'), ('code.GWANGJU_SEO', 'en', 'Seo-gu'),
  ('code.GWANGJU_NAM', 'ko', '남구'), ('code.GWANGJU_NAM', 'en', 'Nam-gu'),
  ('code.GWANGJU_BUK', 'ko', '북구'), ('code.GWANGJU_BUK', 'en', 'Buk-gu'),
  ('code.GWANGJU_GWANGSAN', 'ko', '광산구'), ('code.GWANGJU_GWANGSAN', 'en', 'Gwangsan-gu');
