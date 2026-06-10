# 01. 요구사항 정의 (Requirements)

태국 여행상품을 온라인으로 탐색·예약·결제하는 커머스 웹사이트 **thaiuniontour**.

---

## 1. 비즈니스 목표
- 태국 여행상품(투어·패키지)을 온라인 판매.
- 한정 좌석/날짜별 재고를 **oversell 없이** 안전하게 판매(선착순).
- 결제 수단은 **계좌이체로 시작**하되, 추후 간편결제·카드로 **무중단 확장** 가능하게 설계.

## 2. 사용자 & 권한 (Role 기반)
회원은 **권한(Role)별로 분리**한다. 최소 구성:

| Role | 설명 | 접근 |
|------|------|------|
| `GUEST` | 비로그인 방문자 | 상품 탐색·상세 조회 |
| `MEMBER` | 일반 회원 | 예약·결제·내 예약 조회 |
| `ADMIN` | 운영자 | admin 모듈(상품 등록/수정, 주문·결제 관리) |

> 권한은 공통코드 또는 enum + `member.role` 컬럼으로 관리. (확장: `PARTNER` 등)

## 3. 기능 요구사항

### 3-1. 상품 (Product) — 재고형 vs 비재고형
- **핵심**: 상품을 **재고 관리형 / 비재고형**으로 구분한다. → `product` 테이블에 구분 컬럼(`stock_type`).
  - 예) `LIMITED`(한정 재고: 특정 날짜에 N개, 팔리면 소진) / `UNLIMITED`(상시 판매).
- 투어 상품은 **날짜별 재고**가 핵심. "특정 날짜에 1개" 같은 운영을 위해
  `product`(상품 메타) + **`product_schedule`(날짜별 재고/가격)** 분리 구조로 설계.
- 한정 재고 상품은 마지막 1개가 팔리면 해당 날짜가 **매진(품절)** 처리.
- 선착순 차감은 `docs/design/reference-patterns.md` 의 **원자 UPDATE**(`WHERE stock > 0`) 전략 사용.
  - 재고 1개 상품: 두 명이 동시에 눌러도 정확히 한 명만 성공.

### 3-2. 예약 (Reservation) — 비회원 예약 허용
- 회원/**비회원 모두** 상품/날짜 선택 → 예약 생성 → 결제로 연결.
- **게스트 체크아웃**: 비회원은 이메일/연락처로 예약. `reservation.member_id`는 nullable, 예약자 정보(`guest_email`, `guest_phone`)를 함께 저장.
- 예약 생성 시 좌석/재고를 차감하고 **Transactional Outbox** 로 결제 이벤트 발행(이벤트 기반).
- 상태 흐름: `PENDING`(결제대기/입금대기) → `CONFIRMED`(입금확인 완료) → `CANCELLED`.

### 3-3. 결제 (Payment) — 공통코드로 수단 관리 + 확장성
- **현재 실제 연동 가능: 계좌이체(BANK_TRANSFER) 1종.**
- 단, **결제수단을 공통코드(`PAYMENT_METHOD` 코드그룹)로 관리**해
  네이버페이/카카오페이/토스페이/카드 등을 **코드 추가 + PG 어댑터 추가만으로** 확장.
- 설계 원칙:
  - 결제수단별 처리는 **전략(Strategy)/어댑터 패턴** — `PaymentMethod` 코드 → 해당 `PaymentGateway` 구현 선택.
  - 신규 수단 추가 = 공통코드 1건 + 어댑터 클래스 1개 (기존 코드 무수정).
  - 결제 처리는 이벤트 기반 + **멱등 Consumer**(reference-patterns.md). PG 웹훅/콜백도 멱등.
- **계좌이체 입금확인: 수동(MVP).** 예약=PENDING → 관리자가 입금 확인 후 admin에서 **수동 승인 → CONFIRMED**.
  - 승인 시점에 결제완료 이벤트 발행 → 멱등 결제이력 적재.
  - 구조는 자동 확인(펌뱅킹/가상계좌 웹훅) 어댑터를 추후 추가 가능하게 유지.
- 통화: **KRW(원화) 단일 기준.** 금액은 `BIGINT`(원 단위).

### 3-4. 다국어 (i18n)
- **전용 라벨 테이블**로 관리. 지원 언어: **한국어(ko) / 영어(en)** 2종만.
- 통화는 다국어와 무관하게 KRW 고정.
- 라벨 테이블 구조(초안): `label(code, locale, text)` — `code+locale` UNIQUE.
- 상품명·설명 등 콘텐츠성 다국어는 상품 번역 테이블(`product_i18n`) 또는 라벨 테이블 재사용 — 3단계 ERD에서 확정.

### 3-5. Admin 모듈 (athometrip 참고, Thymeleaf 서버렌더)
- athometrip처럼 **별도 `admin` 모듈** 추가, **Thymeleaf 서버렌더** 방식(고객 Next.js와 분리).
- 운영자가 **직접 상품 등록 / 상품 상세페이지 콘텐츠 작성 / 날짜별 재고 설정 / 주문·결제 조회**.
- **계좌이체 입금확인 수동 승인** 화면 포함(PENDING 주문 → 승인 → CONFIRMED).
- athometrip의 product 하위 구조(option·component·badge·tag 등)와
  Controller→Service→Mapper 패턴을 참고하되 thaiuniontour 범위로 단순화.
- 상품 상세 콘텐츠는 리치 에디터(이미지 포함) 입력을 고려.
- ⇒ backend 구성: `api`(고객 REST) + `admin`(운영 Thymeleaf) 분리. 프론트 Next.js는 **고객 대면 전용**.

## 4. 비기능 요구사항
- **동시성 안전**: 한정 재고 oversell 0% (reference-patterns.md 전략).
- **이벤트 결제 신뢰성**: 이벤트 유실 0%(Outbox) + 중복 결제 0%(Inbox UNIQUE).
- **확장성**: 결제수단·언어를 코드/어댑터 추가만으로 확장.
- **SEO**: 상품 탐색/상세는 SSR(Next.js)로 검색 노출 고려.
- **배포**: Docker(postgres·redis·backend·frontend·nginx).

## 5. MVP 범위
**포함(v1)**: 회원가입/로그인(Role), 상품 목록·상세, 날짜별 재고 예약, 계좌이체 결제(이벤트 기반), admin 상품 등록/상세 작성·재고 설정·주문 조회, 한/영 라벨.
**추후(v2+)**: 간편결제(네이버/카카오/토스)·카드 PG 연동, 좌석 임시점유(TTL), 리뷰, 쿠폰, 파트너.

## 6. 확정된 결정
- DB: PostgreSQL / 메시징·락: Redis / 통화: KRW 단일.
- 결제수단: 공통코드(`PAYMENT_METHOD`) + 어댑터 패턴, 현재 BANK_TRANSFER만 활성.
- **계좌이체 입금확인: 수동 승인(admin)** — PENDING → 승인 → CONFIRMED, 자동 확인 어댑터는 추후.
- 다국어: ko/en 전용 라벨 테이블.
- 재고: `product.stock_type`(LIMITED/UNLIMITED) + 날짜별 재고(`product_schedule`).
- 회원: Role 기반(GUEST/MEMBER/ADMIN), **비회원 예약 허용(게스트 체크아웃)**.
- **Admin: 별도 `admin` 모듈, Thymeleaf 서버렌더(athometrip 동일).** 고객 Next.js와 분리.
- backend 구성: `api`(고객 REST) + `admin`(Thymeleaf) + `domain` + `common` (+ 추후 `scheduler`/`externalapi`).

## 7. 다음 단계(2~3단계)에서 확정할 항목
- [ ] **날짜별 재고 모델**: `product_schedule(product_id, sale_date, stock, price)` 구조 — 3단계 ERD.
- [ ] 공통코드 테이블 구조(athometrip `tb_common_code` 차용, JPA vs MyBatis) — 3단계 ERD.
- [ ] 상품 다국어를 라벨 테이블 재사용 vs `product_i18n` 별도 — 3단계 ERD.
- [ ] 회원 가입 수단(이메일 기본, 소셜은 v2) — 2단계 도메인에서 정리.
- [ ] 이벤트 카탈로그 확정(`RESERVATION_CONFIRMED` 발행 시점 = 입금 수동승인 시) — 2단계 도메인.
