-- =============================================================
-- thaiuniontour 데모 데이터 시드 (개발/미리보기용)
-- 사용: docker exec -i tut-postgres psql -U tut -d thaiuniontour < scripts/seed-demo.sql
-- 주의: 카테고리/상품/일정을 새로 넣는다. 초기화하려면 아래 TRUNCATE 주석 해제.
-- =============================================================

-- (선택) 기존 데이터 초기화
-- TRUNCATE catalog.product RESTART IDENTITY CASCADE;
-- TRUNCATE catalog.category RESTART IDENTITY CASCADE;

-- ── 카테고리: 테마(3단계) ──
INSERT INTO catalog.category(type,level,name,slug,sort) VALUES
  ('THEME',1,'투어','tour',1),
  ('THEME',1,'액티비티·체험','activity',2),
  ('THEME',1,'입장권','ticket',3);
INSERT INTO catalog.category(parent_id,type,level,name,slug,sort)
  SELECT id,'THEME',2,'근교투어','suburb',1 FROM catalog.category WHERE slug='tour' AND type='THEME';
INSERT INTO catalog.category(parent_id,type,level,name,slug,sort)
  SELECT id,'THEME',2,'시티투어','city',2 FROM catalog.category WHERE slug='tour' AND type='THEME';
INSERT INTO catalog.category(parent_id,type,level,name,slug,sort)
  SELECT id,'THEME',3,'아유타야 투어','ayutthaya',1 FROM catalog.category WHERE slug='suburb' AND type='THEME';

-- ── 카테고리: 지역(패싯) ──
INSERT INTO catalog.category(type,level,name,slug,sort) VALUES
  ('DESTINATION',1,'방콕','bangkok',1),
  ('DESTINATION',1,'푸켓','phuket',2),
  ('DESTINATION',1,'치앙마이','chiangmai',3);

-- ── 상품 ──
INSERT INTO catalog.product(name,summary,description,stock_type,base_price,status,thumbnail_url) VALUES
  ('아유타야 1일 투어','방콕 근교 세계문화유산','<p>아유타야 고대 사원 일일 투어</p>','LIMITED',89000,'ON_SALE',NULL),
  ('푸켓 산호섬 스노클링','에메랄드빛 바다 액티비티','<p>산호섬 스노클링 + 점심 포함</p>','LIMITED',65000,'ON_SALE',NULL),
  ('치앙마이 코끼리 보호소','윤리적 코끼리 체험(선착순)','<p>코끼리 보호소 반일 체험</p>','LIMITED',120000,'ON_SALE',NULL),
  ('방콕 왕궁 입장권','상시 구매 가능','<p>왕궁 + 에메랄드 사원 입장권</p>','UNLIMITED',25000,'ON_SALE',NULL);

-- ── 상품-카테고리 매핑(테마 primary + 지역) ──
INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, true FROM catalog.product p, catalog.category c
   WHERE p.name='아유타야 1일 투어' AND c.slug='ayutthaya' AND c.type='THEME';
INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, false FROM catalog.product p, catalog.category c
   WHERE p.name='아유타야 1일 투어' AND c.slug='bangkok' AND c.type='DESTINATION';

INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, true FROM catalog.product p, catalog.category c
   WHERE p.name='푸켓 산호섬 스노클링' AND c.slug='activity' AND c.type='THEME';
INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, false FROM catalog.product p, catalog.category c
   WHERE p.name='푸켓 산호섬 스노클링' AND c.slug='phuket' AND c.type='DESTINATION';

INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, true FROM catalog.product p, catalog.category c
   WHERE p.name='치앙마이 코끼리 보호소' AND c.slug='activity' AND c.type='THEME';
INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, false FROM catalog.product p, catalog.category c
   WHERE p.name='치앙마이 코끼리 보호소' AND c.slug='chiangmai' AND c.type='DESTINATION';

INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, true FROM catalog.product p, catalog.category c
   WHERE p.name='방콕 왕궁 입장권' AND c.slug='ticket' AND c.type='THEME';
INSERT INTO catalog.product_category(product_id,category_id,is_primary)
  SELECT p.id, c.id, false FROM catalog.product p, catalog.category c
   WHERE p.name='방콕 왕궁 입장권' AND c.slug='bangkok' AND c.type='DESTINATION';

-- ── 날짜별 재고/일정 ──
INSERT INTO catalog.product_schedule(product_id,sale_date,stock,price,status)
  SELECT id, d::date, 5, 89000, 'OPEN' FROM catalog.product, generate_series(DATE '2026-07-10', DATE '2026-07-14', '1 day') d
   WHERE name='아유타야 1일 투어';
INSERT INTO catalog.product_schedule(product_id,sale_date,stock,price,status)
  SELECT id, d::date, 10, 65000, 'OPEN' FROM catalog.product, generate_series(DATE '2026-07-10', DATE '2026-07-12', '1 day') d
   WHERE name='푸켓 산호섬 스노클링';
-- 선착순(재고 2)
INSERT INTO catalog.product_schedule(product_id,sale_date,stock,price,status)
  SELECT id, DATE '2026-08-01', 2, 120000, 'OPEN' FROM catalog.product WHERE name='치앙마이 코끼리 보호소';
-- 무제한(stock NULL)
INSERT INTO catalog.product_schedule(product_id,sale_date,stock,price,status)
  SELECT id, d::date, NULL, 25000, 'OPEN' FROM catalog.product, generate_series(DATE '2026-07-10', DATE '2026-07-20', '1 day') d
   WHERE name='방콕 왕궁 입장권';

SELECT (SELECT count(*) FROM catalog.product) AS products,
       (SELECT count(*) FROM catalog.category) AS categories,
       (SELECT count(*) FROM catalog.product_schedule) AS schedules;
