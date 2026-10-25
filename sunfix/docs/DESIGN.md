# 집수리 가격비교 Sunfix 썬픽스 — 설계

구현 중 설계와 달라지면 이 문서부터 고친다.

## 0. 한 줄 요약
"가격은 문의하세요" 대신 **업체별 가격을 공개해 단가 경쟁**을 만드는 집수리 가격비교 사이트. 사용자는 수리 카테고리 → 옵션 → 지역을 고르면, 그 조건의 업체별 총액이 싼 순서로 나온다. UX 레퍼런스는 모두의성지(modusj.com): 큰 카테고리 칩, 지역 타일, 단계별 선택, 가격 카드 목록.

포트폴리오용이다. 파트너 셀프 등록·결제·리뷰는 만들지 않고, 업체·가격은 관리자가 넣는다(초기값은 **예시 데이터** 표시). 여기에 사용자가 **실제로 받은 견적을 제보**해 업체가 공개하지 않은 가격도 드러나게 한다(§7).

운영자 컨셉: 엄마(닉네임 **sunflower**)가 운영하는 서비스. 관리자 아이디 `sunflower`, 푸터에 "운영 Sunflower".

## 1. 결정 사항
| 날짜 | 항목 | 결정 |
|---|---|---|
| 10-25 | 범위 | 사용자 화면 + 관리자 화면. 파트너 어드민 없음(업체·가격은 관리자 입력) |
| 10-25 | 카테고리·옵션·지역 | 전부 **공통코드**(`common_code`) 한 테이블, 부모 코드로 트리(§4) |
| 10-25 | 언어 | 한국어(ko)·영어(en). 화면 문구·코드명 전부 `label` 테이블 |
| 10-25 | 가격 모델 | 업체별 **기본가 + 옵션별 추가금** 합산(§5) |
| 10-25 | 가격 미공개 업체 | 숨기지 않고 목록 아래 "가격 미공개(문의 필요)"로 따로 노출 — 공개 업체가 위로 오는 것 자체가 경쟁 유인 |
| 10-25 | 문의 | 결과 화면에서 업체에 "견적 문의" → `inquiry` 저장, 관리자 화면에서 확인. 문자·메일 발송 없음 |
| 10-25 | 견적 제보 | 1차 범위에 포함. 비회원 제보 → 관리자 승인 후 공개(§7) |
| 10-25 | 이름·도메인 | **Sunfix 썬픽스**(sunflower + fix, "숨은 견적을 햇빛 아래로"), `sunfix.union-selim.com` |
| 10-25 | 카테고리 트리 | 카테고리 아래 하위 카테고리를 몇 단계든(공간 → 세부 항목). 옵션 그룹·업체 가격은 맨 아래(리프)에만(§4) |
| 10-25 | 검색 | 홈 검색창. 리프 카테고리 경로 + 검색어(`kw.{코드}` 라벨)를 브라우저에서 필터(§3-1). 에어컨·보일러는 '가전·냉난방' 공간으로 분리 |
| 10-25 | 운영자 | 엄마(sunflower). 관리자 아이디 `sunflower` |
| 10-25 | 지도 | 지도 SDK 안 씀. 주소 옆 카카오맵 링크(`map.kakao.com/link/search/{주소}`) |

## 2. 스택 (family 재사용)
- Spring Boot 3 / Java 17 단일 모듈 `D-Day/sunfix/`. JdbcTemplate + SQLite(`data/sunfix.db`) + Flyway(`V1__init.sql` 스키마, `V2__seed.sql` 예시 데이터 1회, `R__labels.sql` 화면 문구 `INSERT OR IGNORE` — 관리자가 고친 문구를 덮지 않으므로 기존 문구 수정은 관리자 라벨 화면에서).
- Thymeleaf + 미리 빌드한 Tailwind(`static/tw.css`) + vanilla JS. 다크모드는 family §7 방식 그대로.
- family 에서 **복사**: `LabelService`(th → en 으로만 변경), `RateLimiter`, `ClientIp`, `SecurityHeadersFilter`, `OriginInterceptor`, `Valid`, `ApiExceptionHandler`, 관리자 로그인(BCrypt + `session` 테이블).
- 추가 금지: Spring Security, JPA, Redis, MessageSource, 프론트 빌드 도구, 지도 SDK.
- 배포: Pi docker-compose + `tut-cloudflared` ingress 한 줄(`sunfix.union-selim.com` → `http://sunfix-app:8080`).

## 3. 화면 흐름 (사용자)
```
/ 홈 ─ 검색창(§3-1) + 차별점 섹션(보통의 견적 앱: 요청서·연락처 → 연락 대기 → 통화 후 가격 / Sunfix: 조건 고르기 → 총액 나란히 → 고른 곳만 문의) + 실제 데이터 예시 카드(자주 찾는 수리의 첫 옵션 조합 상위 3곳) + 공간 그리드(방·욕실·주방·창문·가전·냉난방·집 전체), 아이콘은 관리자가 정한 이모지
        지역 타일(서울·경기·인천·부산·대구 + 전체)
        "가격 공개 업체 N곳 / 비교 가능 카테고리 M개" 숫자
   │
/c/{공간} 하위 카테고리 그리드(욕실 → 욕조·수도·배수구·전체 리모델링). 리프에 닿을 때까지 반복, 위에 경로(처음 › 욕실 › 배수구)
   │
/c/{리프} 옵션 선택 ─ 한 페이지에 옵션 그룹을 위→아래로(단계 표시 1/3, 2/3…)
        각 그룹은 큰 버튼 중 하나 고르기(라디오). 고르면 다음 그룹으로 스크롤
        마지막은 지역(시/도 → 시/군/구)
        하단 고정 바: "조건에 맞는 업체 N곳 비교하기" (N 은 고를 때마다 갱신, /api/count)
   │
/c/{category}/result?o=CODE,CODE&r=REGION 결과 ─ URL 만으로 재현·공유 가능
        상단 요약: 최저가 · 평균가 · 최고가, 선택 조건 칩(눌러서 수정)
        정렬: 가격낮은순(기본) · 가격높은순
        업체 카드: 업체명 · 총액(큰 글씨) · 내역 펼치기(기본가 + 옵션별 추가금)
                   · 주소 + 카카오맵 링크 · 전화 · [견적 문의]
        아래: "가격 미공개 업체" 회색 카드(가격 대신 '문의 필요')
        그 아래: "실제 견적 제보" — 같은 카테고리·시/도의 승인된 제보 목록
                 (같은 옵션 조합 제보가 3건 이상이면 중간값도 표시) + [내 견적 제보하기]
   │
/report 견적 제보 ─ 카테고리 → 옵션 → 지역(옵션 선택 화면 재사용) + 업체(목록에서 고르기 or 직접 입력)
        + 견적 금액 · 견적 받은 달 · 한 줄 메모(선택)
   │
/v/{id} 업체 상세 ─ 소개, 서비스 카테고리별 가격표 전체, 출장 지역
```
- 문의 모달: 이름 · 연락처 · 메모. 선택 조건과 계산된 총액을 스냅샷으로 함께 저장. IP 당 5회/시간.
- 모든 화면 헤더: 로고 · 언어(ko/en) · 테마 토글.
- 모바일 우선. 카드 1열, 데스크톱 2열.

## 3-1. 검색
공간부터 고르면 "에어컨은 어디?"처럼 헤매는 경우가 생겨서 홈 히어로 아래 검색창을 둔다.
- 대상은 사용 중인 리프 카테고리만. 결과는 경로(`가전·냉난방 › 에어컨 청소`)로 보여 주고 누르면 옵션 선택으로. 엔터 = 첫 결과.
- 검색어(동의어·증상·활용형)는 라벨 `kw.{코드}`(ko·en, 쉼표 구분). 관리자 카테고리 화면 리프 행에서 수정. 이름 경로는 자동 포함.
- 매칭: 입력을 공백으로 나눈 단어마다 (이름·검색어에 포함) 또는 (단어가 2자 이상 검색어를 포함: '막혔어요' ⊃ '막혔') 이면 맞음. 맞은 단어 수 순으로 6개.
- 서버 검색 없음: 리프 목록·검색어 JSON 을 홈에 넣고 `search.js` 가 거른다. `ponytail: 리프가 수백 개를 넘으면 /api/search`.
- 결과 없으면 "받은 견적 제보하기"로. 검색창 아래 "자주 찾는 수리" 칩 = 승인 제보가 많은 리프 4개.
- 백로그: 결과 없는 검색어 로그(카테고리 추가 근거), 초성 검색.

## 4. 공통코드
한 테이블에 그룹(`group_code`)과 부모(`parent_code`)로 트리를 만든다. 코드는 전역 유일 → 라벨 키를 `code.{code}` 하나로 쓸 수 있다.

| group_code | 의미 | parent_code | 예 |
|---|---|---|---|
| `CATEGORY` | 공간·수리 항목 | — 또는 카테고리 | `BATHROOM`(욕실) → `DRAIN`(배수구 막힘) |
| `OPT_GROUP` | 옵션 그룹 | 카테고리 | `WALLPAPER_AREA`(시공 면적) |
| `OPT` | 옵션 값 | 옵션 그룹 | `WALLPAPER_AREA_20`(20평대) |
| `REGION` | 시/도 | — | `SEOUL` |
| `REGION_SUB` | 시/군/구 | 시/도 | `SEOUL_GANGNAM` |

지역: 서울·경기·인천·부산·대구 + V7 광주광역시(동·서·남·북·광산구, 운영자 부모님 거주지).

- **카테고리 트리**: 카테고리는 카테고리 아래로 몇 단계든 내려간다. 한 카테고리는 **하위 카테고리 또는 옵션 그룹 중 한 종류만** 가진다. 옵션 그룹을 가진 맨 아래 카테고리(리프)만 업체 가격·제보·문의 대상(`vendor_category`·`quote_report.category_code` 는 리프 코드). 업체 가격이 붙은 리프 아래에는 하위 카테고리를 못 만든다(가격이 갈 곳을 잃음). 상위가 `use_yn='N'` 이면 아래 전부 숨김.
- 카테고리 아이콘은 `common_code.icon`(이모지 하나), 관리자 화면에서 수정.
- `sort_no` 로 정렬, `use_yn='N'` 이면 사용자 화면에서 숨김(업체 가격 행은 남김).
- 이름은 `label(key='code.WALLPAPER', lang, text)`. 관리자 화면에서 코드 추가할 때 ko·en 이름을 같이 입력.
- 옵션 그룹은 전부 **필수 단일 선택**. 복수 선택·수량 입력은 백로그.
- 코드 조회는 앱 기동 시 메모리 캐시, 관리자 저장 시 무효화.

V5(2026-10-25) 시장 공개가에 맞춤: 에어컨 종류에 천장형 4way 추가(1~2way 와 분리), '일반/완전 분해' → '송풍팬 분해'(업체들이 실제로 가르는 경계), 실외기 옵션 추가. 배수구는 '배수구·변기 막힘'으로 이름 바꾸고 변기 이물질 회수 단계 추가, 고압 세척은 '시작가' 표기(업체마다 8~30만으로 편차 큼). 예시 업체 가격도 시장 중앙값으로. 업체 공통 조건은 V7(§5-1).

시드 트리: 방(도배·장판) / 욕실·화장실(욕조·수도·수전·배수구 막힘·욕실 전체 리모델링) / 주방(싱크 수전·싱크대 막힘) / 창문·유리(유리 교체·방충망) / 가전·냉난방(에어컨 청소·보일러) / 집 전체(누수 탐지·줄눈). 리프당 옵션 그룹 2~3개.

## 5. 가격 계산
```
총액 = vendor_category.base_price + Σ vendor_option_price.price (선택한 옵션마다)
```
- 업체가 선택한 옵션 중 **하나라도 가격이 없으면** 그 업체는 "가격 미공개" 칸으로 간다.
- 금액은 원 단위 정수(`INTEGER`). 영어 화면에서도 KRW 표시(`₩1,200,000`), 환율 변환 안 함.
- 면적처럼 곱셈이 자연스러운 옵션도 구간(10평대·20평대…)으로 나눠 추가금으로 표현한다.
  `ponytail: 합산 모델만 지원, 평당 단가×면적이 필요하면 OPT 에 calc_type(ADD|MULTIPLY) 추가`
- 결과 쿼리는 SQL 한 번(카테고리·지역으로 업체를 거르고 옵션 가격을 `GROUP BY vendor` 로 합산, `COUNT = 선택 옵션 수` 로 공개/미공개 구분).

## 5-1. 업체 공통 조건
조사해 보니(§8-1) 업체 고를 때 가격 못지않게 조건을 본다. 업체마다 표기가 제각각이라 칸을 정해 둔다.
- `vendor.vat`(INCL 포함 / EXCL 별도), `trip_fee`(출장비, 0 = 무료), `fail_fee`(작업 불가·미해결 시 비용, 0 = 못 고치면 0원), `surcharge`(야간·주말 할증 Y/N), `extra_note`(업소·층고·신형 기종 등 자유 문구).
- **NULL = 업체가 밝히지 않음** → 화면에 아무것도 안 그린다(추측해서 채우지 않는다). 관리자 업체 화면에서 업체가 밝힌 것만 입력.
- 결과 카드·업체 상세에 칩으로: 소비자에게 좋은 조건(VAT 포함·출장비 무료·못 고치면 0원·할증 없음)은 초록, 나머지는 회색.
- 총액 계산에는 넣지 않는다(조건부 비용이라). `ponytail: 출장비를 총액에 더할지는 실제 업체가 모이면 결정`

## 6. 다국어
family §6 방식 그대로, 언어만 ko·en.
- 템플릿은 `${L['result.lowest']}` 처럼 키로만. 공통코드 이름은 `${L['code.' + c.code]}`.
- 언어 결정: `?lang=` → 쿠키 `lang` → `Accept-Language`(`ko*` 면 ko, 그 외 en).
- 업체가 입력한 데이터(업체명·소개·주소)는 번역하지 않는다. 업체명만 `name_en`(선택) 칸을 두고 en 화면에서 있으면 쓴다.
- 빠진 en 라벨은 기동 시 경고 로그 + ko fallback. 관리자 라벨 화면에 "en 미번역" 필터.

## 7. 견적 제보
업체가 가격을 안 올려도 사용자들이 받은 견적이 쌓이면 시세가 드러난다. "문의하세요" 업체를 압박하는 장치.
- 비회원 제보. 이름·연락처 받지 않음(개인정보 안 모음). IP 당 3회/일.
- 상태 `PENDING` → 관리자 승인 `APPROVED` / 반려 `REJECTED`. 승인된 것만 공개.
- 업체는 등록 업체 중에서 고르거나 이름 직접 입력. 직접 입력한 업체명은 **공개하지 않는다**(검증 안 된 실명 비방 방지) — 화면에는 "미등록 업체".
- 등록 업체 제보는 결과 카드에 "제보 N건 · 제보 평균 ₩…" 표시. 공개가와 제보가가 크게 다르면(±30%) 관리자 화면에 표시만.
- 금액 이상치(같은 옵션 조합 중간값의 1/3 미만·3배 초과)는 관리자 목록에 경고 배지. 자동 반려는 안 함.
- 영수증·견적서 사진 첨부는 백로그(저장소·개인정보 마스킹이 필요).
  `ponytail: 승인제 + IP 제한뿐, 어뷰징이 보이면 휴대폰 인증 추가`

## 8. 관리자 `/admin`
- 로그인: 아이디 `sunflower`. 처음 비밀번호는 `.env` `ADMIN_PW_HASH`, 관리자 화면 **비밀번호 변경**(`/admin/password`: 현재·새·확인, 8자 이상)으로 바꾸면 `admin_account` 해시가 우선. 바꾸면 모든 관리자 세션을 끊고 지금 기기만 새로 로그인. 관리자 1명뿐이라 역할·권한 테이블 없음.
- 화면:
  1. **카테고리·코드** — 카테고리/지역 탭. 카테고리는 최상위 공간별 탭으로 나눠 그 아래 트리(하위 카테고리 → 옵션 그룹 → 옵션)를 들여쓰기로. 행마다 아이콘·ko·en 이름·순서·사용여부 인라인 편집. 카테고리 행에는 규칙에 맞는 버튼만(`+ 하위 카테고리` / `+ 옵션 그룹`), 공간 탭 옆 `+ 최상위 추가`.
  2. **업체** — 목록/등록/수정. 기본 정보, 출장 지역(시/도 전체 또는 시/군/구 체크), 카테고리 선택.
  3. **가격표** — 업체 × 카테고리 하나를 열면 기본가 + 옵션 값 전부가 표로, 칸에 금액 입력(빈 칸 = 미공개). 칸을 벗어나면 자동 저장.
  4. **라벨** — key·ko·en 검색/수정.
  5. **문의** — 최신순 목록, 처리 상태(NEW/DONE) 토글.
  6. **견적 제보** — 대기 목록 우선, 승인/반려 버튼, 이상치 경고 배지.
- 관리자 화면도 라벨 사용(ko/en).

## 8-1. 영업 리드 `/admin/leads`
가격표를 이미 공개한 실제 업체에 "무료로 올려 드리고 손님을 연결해 드린다"고 연락하기 위한 목록(2026-10-25 조사: 에어컨 청소 16곳, 배수구·변기 막힘 8곳).
- 한 장에 업체 하나: 우선순위 배지(먼저 연락 / 보통 / 보류) + **추천 이유**, 공개 가격, 조건, 공개된 사업용 연락처, 출처 링크·확인일, 상태(연락 전 → 연락함 → 입점 동의 / 거절, 자동 저장), 통화 메모(자동 저장).
- 정렬: 거절은 맨 아래, 그다음 우선순위. 카테고리·상태 필터. 위에 "연락할 때 이렇게 말해 보세요" 문구(라벨 `admin.leads.pitch`, 복사 버튼).
- **업체로 등록**: 입점 동의하면 누른다 → 상호·첫 전화번호·지역·카테고리로 `vendor` 생성(`sample_yn='N'`, `use_yn='N'` — 가격을 넣기 전엔 사용자 화면에 안 보임), 리드에 연결하고 업체 편집 화면으로.
- 데이터는 실제 업체 연락처라 **레포에 넣지 않는다**: `docs/private/leads-*.json`(gitignore) → `ADMIN_PW=… scripts/import-leads.sh <json> [base-url]`. 같은 (카테고리, 상호)는 조사 칸만 갱신하고 상태·메모·연결은 유지.
- 우선순위 기준: 서울·경기 1인/동네 업체 + 가격 명확 + 전화 공개 + 최근 수정 → 먼저 연락. 크몽 메시지만 되는 곳·지역 확인 필요 → 보통. 프랜차이즈 본사·지사형(제휴 단계에서), 오래된 글, 중개 플랫폼(경쟁사) → 보류.

## 9. 데이터 모델
```sql
CREATE TABLE common_code (
  code        TEXT PRIMARY KEY,                -- 'WALLPAPER_AREA_20'
  group_code  TEXT NOT NULL,                   -- CATEGORY | OPT_GROUP | OPT | REGION | REGION_SUB
  parent_code TEXT REFERENCES common_code(code),
  icon        TEXT,                            -- 카테고리 이모지
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
  vat TEXT, trip_fee INTEGER, fail_fee INTEGER, surcharge TEXT, extra_note TEXT,   -- V7 공통 조건(§5-1)
  sample_yn   TEXT NOT NULL DEFAULT 'Y',       -- 예시 데이터면 카드에 '예시' 배지
  use_yn      TEXT NOT NULL DEFAULT 'Y',
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE vendor_region (                   -- 출장 가능 지역. 매칭은 region_code IN (고른 시/군/구, 그 시/도)
  vendor_id   INTEGER NOT NULL REFERENCES vendor(id),
  region_code TEXT NOT NULL REFERENCES common_code(code),  -- REGION(시/도 전체) 또는 REGION_SUB
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
  option_code TEXT NOT NULL REFERENCES common_code(code),  -- OPT
  price       INTEGER NOT NULL,                -- 추가금(0 가능). 행이 없으면 미공개
  updated_at  TEXT NOT NULL DEFAULT (datetime('now')),
  PRIMARY KEY (vendor_id, option_code)
);

CREATE TABLE inquiry (
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  vendor_id     INTEGER NOT NULL REFERENCES vendor(id),
  category_code TEXT NOT NULL,
  option_codes  TEXT NOT NULL,                 -- 'A,B,C' 스냅샷
  region_code   TEXT NOT NULL,
  quoted_price  INTEGER,                       -- 문의 시점 계산 총액(미공개면 NULL)
  name          TEXT NOT NULL,
  phone         TEXT NOT NULL,
  memo          TEXT,
  status        TEXT NOT NULL DEFAULT 'NEW',   -- NEW | DONE
  ip            TEXT,
  created_at    TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE quote_report (                    -- 견적 제보(§7)
  id            INTEGER PRIMARY KEY AUTOINCREMENT,
  category_code TEXT NOT NULL REFERENCES common_code(code),
  option_codes  TEXT NOT NULL,                 -- 'A,B,C' (정렬해서 저장 → 같은 조합 비교가 문자열 비교)
  region_code   TEXT NOT NULL REFERENCES common_code(code),  -- REGION_SUB
  vendor_id     INTEGER REFERENCES vendor(id), -- 등록 업체면
  vendor_name   TEXT,                          -- 직접 입력(비공개)
  price         INTEGER NOT NULL,
  quoted_ym     TEXT NOT NULL,                 -- '2026-10'
  memo          TEXT,
  status        TEXT NOT NULL DEFAULT 'PENDING', -- PENDING | APPROVED | REJECTED
  ip            TEXT,
  created_at    TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX ix_report_lookup ON quote_report(category_code, status, option_codes);

CREATE TABLE sales_lead (                      -- V6. 영업 리드(§8-1). 컬럼 설명은 V6__sales_lead.sql
  id INTEGER PRIMARY KEY AUTOINCREMENT, category_code TEXT NOT NULL, name TEXT NOT NULL, region TEXT, channel TEXT,
  prices TEXT, conditions TEXT, contact TEXT, source_url TEXT, source_date TEXT,
  priority INTEGER NOT NULL DEFAULT 2, reason TEXT, status TEXT NOT NULL DEFAULT 'TODO', memo TEXT,
  vendor_id INTEGER REFERENCES vendor(id), checked_at TEXT NOT NULL, updated_at TEXT NOT NULL,
  UNIQUE (category_code, name)
);

CREATE TABLE admin_account (                   -- V3. 화면에서 바꾼 비밀번호. 행이 있으면 .env 보다 우선
  id            INTEGER PRIMARY KEY CHECK (id = 1),
  password_hash TEXT NOT NULL,
  updated_at    TEXT NOT NULL
);

CREATE TABLE admin_session (                   -- 관리자 로그인. 쿠키 토큰의 SHA-256 만 저장
  token_hash TEXT PRIMARY KEY,
  expires_at TEXT NOT NULL
);
```

## 10. URL·API
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/` | 홈 |
| GET | `/c/{category}` | 옵션 선택 |
| GET | `/c/{category}/result?o=&r=&sort=` | 결과(서버 렌더링) |
| GET | `/v/{id}` | 업체 상세 |
| GET | `/api/count?c=&o=&r=` | 고르는 중 하단 바 숫자 |
| POST | `/api/inquiry` | 견적 문의 |
| GET | `/report?c=` | 견적 제보 화면 |
| POST | `/api/report` | 견적 제보 저장(PENDING) |
| GET/POST | `/admin/**` | 관리자 화면·저장(JSON) |

## 11. 시드 데이터
- 공간 6개 → 리프 14개(검색어 포함) × 옵션, 서울·경기·인천 시/군/구, 예시 업체 20곳(가상의 상호, `sample_yn='Y'`, 전화번호 `010-0000-xxxx`). 일부 업체는 일부 옵션 가격을 비워 "미공개" 칸이 보이게. 승인된 예시 제보 30건(미공개 업체 대상 포함).
- 실존 업체명·상표 사용 금지.

## 12. 백로그
옵션 복수 선택·수량 입력, 곱셈 가격(§5), 지도 임베드·거리순 정렬, 제보 사진 첨부·휴대폰 인증, 가격 변경 이력·알림, 파트너 셀프 등록, 리뷰·평점.
