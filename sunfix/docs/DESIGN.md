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
| 10-25 | 이름·도메인 | **Sunfix 썬픽스**(sunflower + fix, "숨은 견적을 햇빛 아래로"), `sunfix.thaiuniontour.com` |
| 10-25 | 운영자 | 엄마(sunflower). 관리자 아이디 `sunflower` |
| 10-25 | 지도 | 지도 SDK 안 씀. 주소 옆 카카오맵 링크(`map.kakao.com/link/search/{주소}`) |

## 2. 스택 (family 재사용)
- Spring Boot 3 / Java 17 단일 모듈 `D-Day/sunfix/`. JdbcTemplate + SQLite(`data/sunfix.db`) + Flyway(`V1__init.sql` 스키마, `V2__seed.sql` 예시 데이터 1회, `R__labels.sql` 화면 문구 `INSERT OR IGNORE` — 관리자가 고친 문구를 덮지 않으므로 기존 문구 수정은 관리자 라벨 화면에서).
- Thymeleaf + 미리 빌드한 Tailwind(`static/tw.css`) + vanilla JS. 다크모드는 family §7 방식 그대로.
- family 에서 **복사**: `LabelService`(th → en 으로만 변경), `RateLimiter`, `ClientIp`, `SecurityHeadersFilter`, `OriginInterceptor`, `Valid`, `ApiExceptionHandler`, 관리자 로그인(BCrypt + `session` 테이블).
- 추가 금지: Spring Security, JPA, Redis, MessageSource, 프론트 빌드 도구, 지도 SDK.
- 배포: Pi docker-compose + `tut-cloudflared` ingress 한 줄(`sunfix.thaiuniontour.com` → `http://sunfix-app:8080`).

## 3. 화면 흐름 (사용자)
```
/ 홈 ─ 카테고리 칩 그리드(도배·장판·욕실·누수·에어컨청소…)
        지역 타일(서울·경기·인천·부산·대구 + 전체)
        "가격 공개 업체 N곳 / 비교 가능 카테고리 M개" 숫자
   │
/c/{category} 옵션 선택 ─ 한 페이지에 옵션 그룹을 위→아래로(단계 표시 1/3, 2/3…)
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

## 4. 공통코드
한 테이블에 그룹(`group_code`)과 부모(`parent_code`)로 트리를 만든다. 코드는 전역 유일 → 라벨 키를 `code.{code}` 하나로 쓸 수 있다.

| group_code | 의미 | parent_code | 예 |
|---|---|---|---|
| `CATEGORY` | 수리 카테고리 | — | `WALLPAPER`(도배) |
| `OPT_GROUP` | 옵션 그룹 | 카테고리 | `WALLPAPER_AREA`(시공 면적) |
| `OPT` | 옵션 값 | 옵션 그룹 | `WALLPAPER_AREA_20`(20평대) |
| `REGION` | 시/도 | — | `SEOUL` |
| `REGION_SUB` | 시/군/구 | 시/도 | `SEOUL_GANGNAM` |

- `sort_no` 로 정렬, `use_yn='N'` 이면 사용자 화면에서 숨김(업체 가격 행은 남김).
- 이름은 `label(key='code.WALLPAPER', lang, text)`. 관리자 화면에서 코드 추가할 때 ko·en 이름을 같이 입력.
- 옵션 그룹은 전부 **필수 단일 선택**. 복수 선택·수량 입력은 백로그.
- 코드 조회는 앱 기동 시 메모리 캐시, 관리자 저장 시 무효화.

시드 카테고리(예): 도배, 장판, 욕실 리모델링, 누수 탐지, 에어컨 청소, 보일러 교체, 줄눈 시공, 방충망 교체. 카테고리당 옵션 그룹 2~3개.

## 5. 가격 계산
```
총액 = vendor_category.base_price + Σ vendor_option_price.price (선택한 옵션마다)
```
- 업체가 선택한 옵션 중 **하나라도 가격이 없으면** 그 업체는 "가격 미공개" 칸으로 간다.
- 금액은 원 단위 정수(`INTEGER`). 영어 화면에서도 KRW 표시(`₩1,200,000`), 환율 변환 안 함.
- 면적처럼 곱셈이 자연스러운 옵션도 구간(10평대·20평대…)으로 나눠 추가금으로 표현한다.
  `ponytail: 합산 모델만 지원, 평당 단가×면적이 필요하면 OPT 에 calc_type(ADD|MULTIPLY) 추가`
- 결과 쿼리는 SQL 한 번(카테고리·지역으로 업체를 거르고 옵션 가격을 `GROUP BY vendor` 로 합산, `COUNT = 선택 옵션 수` 로 공개/미공개 구분).

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
- 로그인: 아이디 `sunflower`, 비밀번호 해시는 `.env` `ADMIN_PW_HASH`. 관리자 1명뿐이라 역할·권한 테이블 없음.
- 화면:
  1. **공통코드** — 그룹 탭(카테고리/지역). 카테고리를 누르면 옵션 그룹 → 옵션 값이 트리로. 행마다 코드·ko 이름·en 이름·순서·사용여부를 인라인 편집.
  2. **업체** — 목록/등록/수정. 기본 정보, 출장 지역(시/도 전체 또는 시/군/구 체크), 카테고리 선택.
  3. **가격표** — 업체 × 카테고리 하나를 열면 기본가 + 옵션 값 전부가 표로, 칸에 금액 입력(빈 칸 = 미공개). 칸을 벗어나면 자동 저장.
  4. **라벨** — key·ko·en 검색/수정.
  5. **문의** — 최신순 목록, 처리 상태(NEW/DONE) 토글.
  6. **견적 제보** — 대기 목록 우선, 승인/반려 버튼, 이상치 경고 배지.
- 관리자 화면도 라벨 사용(ko/en).

## 9. 데이터 모델
```sql
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
- 카테고리 8개 × 옵션, 서울·경기·인천 시/군/구, 예시 업체 20곳(가상의 상호, `sample_yn='Y'`, 전화번호 `010-0000-xxxx`). 일부 업체는 일부 옵션 가격을 비워 "미공개" 칸이 보이게. 승인된 예시 제보 30건(미공개 업체 대상 포함).
- 실존 업체명·상표 사용 금지.

## 12. 백로그
옵션 복수 선택·수량 입력, 곱셈 가격(§5), 지도 임베드·거리순 정렬, 제보 사진 첨부·휴대폰 인증, 가격 변경 이력·알림, 파트너 셀프 등록, 리뷰·평점.
