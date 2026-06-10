# 05. 화면 / IA 설계 (Next.js 14 App Router)

고객 프론트. **athometrip-front UI 참고 + 사용성 개선**(데이터 기반 카테고리·검색·필터).
admin은 Thymeleaf(별도, `api`와 분리)라 본 문서는 **고객 프론트만** 다룬다.

## 스택/원칙
- Next.js 14 App Router / React 18 / TS / Tailwind. 데이터는 `/api/v1` 소비.
- **SSR/SEO**: 상품 목록·상세·카테고리는 서버 컴포넌트로 렌더(검색 노출). 인터랙션(필터·캘린더·장바구니)은 클라이언트 컴포넌트.
- 다국어 ko/en: 라벨은 `/labels?locale=` 캐시. 통화 KRW 고정.

---

## 1. 사이트맵
```
홈 /
├─ 카테고리  /c/[...slug]          (테마 트리 동적 — 하드코딩 금지)
├─ 검색      /search?q=&...        (검색+필터 결과)
├─ 상품상세  /products/[id]
├─ 예약
│   ├─ 결제대기/안내 /reservations/[id]
│   └─ 비회원 조회   /reservations/lookup
├─ 마이페이지 /mypage              (MEMBER: 내 예약)
│   └─ /mypage/reservations
├─ 인증      /login  /signup
└─ 정적      /company /terms /privacy
```

## 2. App Router 트리
```
app/
├─ layout.tsx                 # Header(GNB+검색) · Footer · 라벨 Provider
├─ page.tsx                   # 홈 (서버: 추천/배너/카테고리 진입)
├─ c/[...slug]/page.tsx       # 카테고리 랜딩 (서버: 목록+필터) ★동적
├─ search/page.tsx            # 검색 결과 (서버: 목록+필터)
├─ products/[id]/
│   ├─ page.tsx               # 상세 (서버: SEO 메타)
│   └─ BookingPanel.tsx       # 캘린더/수량/예약 (클라이언트)
├─ reservations/
│   ├─ [id]/page.tsx          # 예약완료/계좌안내/상태폴링
│   └─ lookup/page.tsx        # 비회원 조회 (클라이언트 폼)
├─ mypage/reservations/page.tsx
├─ (auth)/login/page.tsx  (auth)/signup/page.tsx
└─ components/  (Header, MegaMenu, SearchBar, ProductCard, FilterSidebar, Breadcrumb, ...)
```

> athometrip의 `app/[region]/stay|hotel|...` 고정 라우트를 **`/c/[...slug]` 동적 1개**로 대체. 카테고리는 DB(`category`)에서 옴.

---

## 3. 전역 UI (Header) — athometrip 대비 개선 핵심
```
┌──────────────────────────────────────────────────────────┐
│ LOGO   [투어 ▾][액티비티 ▾][입장권 ▾][교통 ▾][패키지 ▾]   🔍검색바  ko|en  로그인 │
└──────────────────────────────────────────────────────────┘
        └ MegaMenu: 호버 시 2·3단계 펼침 (category?type=THEME)
```
- **`<MegaMenu>`**: `GET /categories?type=THEME` 트리 → 대분류 탭, 호버 시 중·소분류 패널. 서버에서 prefetch(레이아웃 캐시).
- **`<SearchBar>`**: 항상 노출. 입력 시 `/products/autocomplete?q=` 디바운스 → 드롭다운. Enter → `/search?q=`.
- 언어 토글(ko/en), 로그인 상태(Zustand).

---

## 4. 페이지별 설계

### 4-1. 홈 `/`
- 히어로 배너 + 인기/추천 상품 캐러셀(Swiper) + 테마/지역 바로가기.
- 서버 렌더(SEO). 데이터: 추천 상품(목록 API), 카테고리.

### 4-2. 카테고리 `/c/[...slug]` · 검색 `/search`
**공통 레이아웃: 좌측 필터 + 우측 그리드** (athometrip엔 없던 구조)
```
[브레드크럼: 투어 > 근교투어 > 아유타야]
┌─ 필터 ─┐ ┌───── 상품 그리드 ─────┐
│지역 □방콕│ │ [카드][카드][카드]    │
│   □푸켓 │ │ [카드][카드][카드]    │
│가격 ▱●▱ │ │  ... 페이지네이션      │
│날짜 📅  │ └──────────────────────┘
│정렬 ▾  │
└────────┘
```
- `<FilterSidebar>`(클라): 지역(패싯)·가격·날짜·정렬 → URL 쿼리 동기화(`?destinationId=&minPrice=&sort=`).
- `<ProductCard>`: 썸네일·이름·최저가·뱃지(매진/특가)·대표 카테고리.
- `<Breadcrumb>`: 상품 `primary` 카테고리 경로(3단계).
- slug → categoryId 매핑은 서버에서 `category` 조회. 결과는 `GET /products?categoryId=...`.

### 4-3. 상품 상세 `/products/[id]`
```
[갤러리]              [예약 패널 ─ sticky]
이름/요약/뱃지        ┌ 날짜 선택(캘린더) ┐
[상세 HTML]          │ 7/1 ●남음1  7/2 매진│
[지도/안내]          │ 수량 [1] 금액 89,000│
리뷰(v2)             │ [예약하기]          │
                     └────────────────────┘
```
- 서버: `GET /products/{id}` (SEO 메타 — title/desc/OG 이미지).
- `<BookingPanel>`(클라): `GET /products/{id}/schedules` → 날짜별 재고/가격 캘린더. 매진(`stock=0`)은 비활성.
- **재고형/비재고형 분기**: LIMITED는 "남은 좌석" 표시·매진 처리, UNLIMITED는 날짜만 선택.
- 예약하기 → `POST /reservations` → 성공 시 `/reservations/[id]`.

### 4-4. 예약/결제대기 `/reservations/[id]`
- `POST /reservations` 결과(PENDING) 표시: **예약번호 + 계좌이체 안내 + 입금기한**.
- 상태 폴링: `GET /payments/by-reservation/{id}` → PAID되면 "결제완료" 전환.
- 회원=토큰 / 비회원=예약번호로 접근(딥링크 + 이메일 검증).

### 4-5. 비회원 조회 `/reservations/lookup`
- 폼(예약번호 + 이메일) → `GET /reservations/lookup?no=&email=` → 상세.

### 4-6. 마이페이지 `/mypage/reservations` (MEMBER)
- `GET /reservations/me` 목록(상태 뱃지) → 상세/취소.

### 4-7. 인증 `/login` `/signup`
- 로그인 → accessToken(메모리/Zustand) + refresh(httpOnly 쿠키). 비회원도 예약 가능하므로 **로그인 강제 안 함**.

---

## 5. 상태관리 / 데이터
- **서버 상태**: 목록·상세·카테고리는 서버 컴포넌트 fetch(캐시/revalidate). 클라 상호작용은 fetch + (선택) React Query.
- **클라 전역(Zustand)**: 인증(member/token), 언어(locale), (v2) 장바구니.
- 필터 상태 = **URL 쿼리**(공유·뒤로가기 자연스럽게). 컴포넌트는 URL을 단일 출처로.

## 6. 다국어 (ko/en)
- 라벨: `GET /labels?locale=` → 레이아웃 Provider 주입. `t('product.book')`.
- 상품 콘텐츠 다국어는 v2(현재 ko 우선, 구조는 label/`product_i18n` seam).
- URL 로케일 전략: 쿠키/헤더 기반(경로 prefix 없이 시작), 필요시 `/en/...` 도입 seam.

## 7. SEO
- 상품/카테고리: 서버 렌더 + `generateMetadata`(title·description·canonical·OG).
- `sitemap.ts`(상품/카테고리 동적), `robots.ts`. 구조화데이터(JSON-LD `Product`/`Offer`).

## 8. athometrip 대비 개선 요약
| athometrip | thaiuniontour |
|---|---|
| `/[region]/stay` 하드코딩 | `/c/[...slug]` 동적(DB 카테고리) |
| 지역에 갇힘 | 테마 주축 + 지역 패싯 |
| 검색 없음 | 글로벌 검색바 + 자동완성 |
| 필터 없음 | 필터 사이드바(지역·가격·날짜·정렬) |
| 브레드크럼 약함 | 3단계 브레드크럼 |

## 다음 단계(6단계 인프라)로 넘길 포인트
- Next.js(SSR) ↔ `api`(REST) ↔ admin(Thymeleaf) ↔ postgres/redis 컨테이너 구성.
- 빌드/배포, 환경변수(API base URL), nginx 라우팅(고객/admin 분리), 이미지 업로드 저장소.
