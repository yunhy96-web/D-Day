# thaiuniontour 진행 상황 (Resume Doc)

세션이 끊겨도 이어서 작업하기 위한 현황 문서. 최종 업데이트: 2026-06 (M2 완료 시점).

---

## 한눈에 보기
- **설계**: 0~8단계 전부 완료 → `docs/design/` (요구사항·도메인·ERD·API·화면·인프라·백로그·정책)
- **구현**: M0(스캐폴딩) · M1(상품/탐색) · **M2 코어(예약·이벤트결제) 완료**
- **다음**: M2 잔여(자동만료·환불), M3(회원/마이페이지), 그 외

```
✅ M0 스캐폴딩        멀티모듈·compose·Flyway·DDD베이스·확장seam
✅ M1 Catalog/탐색    상품·카테고리(admin) / 검색·필터·상세·카테고리랜딩(고객)
✅ M2 예약·결제 코어  선착순 재고차감·Outbox·Relayer·멱등Consumer·입금승인·예약UI
🟡 M2 잔여           입금기한 자동만료 / 취소 환불정책 산정
⬜ M3+               회원·로그인·마이페이지 / 연관상품·취소정책 admin / 검색 고도화
```

---

## 로컬 실행 방법

### 사전 준비 (이미 충족)
Docker, Java 17(또는 21), Node, pnpm.

### 1) 인프라 (PostgreSQL 5433 · Redis 6380)
```bash
cd thaiuniontour/docker
cp .env.example .env        # 최초 1회
docker compose up -d
```
> 포트는 다른 프로젝트 충돌 회피로 **5433/6380** 사용(컨테이너 내부는 5432/6379).

### 2) 데모 데이터 시드 (선택, 화면 미리보기용)
```bash
docker exec -i tut-postgres psql -U tut -d thaiuniontour < thaiuniontour/scripts/seed-demo.sql
```
> 상품 4종 + 카테고리(테마 3단계/지역) + 날짜별 재고(선착순·무제한 포함)가 들어감.
> 비우려면: `docker exec -i tut-postgres psql -U tut -d thaiuniontour -c "TRUNCATE catalog.product, catalog.category RESTART IDENTITY CASCADE;"`

### 3) 백엔드 (api 8080, admin 8081)
```bash
cd thaiuniontour/backend
./gradlew :api:bootRun        # 고객 REST + Outbox Relayer + 결제 Consumer (Flyway 자동 마이그레이션)
./gradlew :admin:bootRun      # (다른 터미널) 운영 콘솔
```

### 4) 프론트엔드 (3000)
```bash
cd thaiuniontour/frontend
cp .env.example .env.local     # 최초 1회
pnpm install
pnpm dev                       # http://localhost:3000
```

### 접속 URL
| 화면 | URL |
|---|---|
| 고객 사이트 | http://localhost:3000 |
| 운영(admin) | http://localhost:8081/admin |
| API 헬스 | http://localhost:8080/api/v1/ping |

### 둘러보기 시나리오
1. (데모 시드 후) http://localhost:3000 → 홈 상품 그리드 → 검색/카테고리(`/c/tour`)/필터(지역·가격)
2. 상품 상세 → **예약하기**(날짜 선택→예약자 입력) → 예약완료 페이지(계좌 안내 + 상태 폴링)
3. http://localhost:8081/admin → 예약/입금 승인 → **입금 승인** 클릭
4. 고객 예약완료 페이지가 잠시 후 **결제 완료(PAID)** 로 전환 (이벤트 파이프라인)
5. 상품/일정은 admin `/admin/products`, `/admin/products/{id}/schedules` 에서 직접 등록 가능

---

## 아키텍처 요약 (어디에 뭐가 있나)

### 백엔드 (`backend/`, Gradle 멀티모듈, Java17/Spring Boot 3.2)
| 모듈 | 책임 | 핵심 위치 |
|---|---|---|
| `common` | 공통응답·에러·예외·JWT·DDD베이스(Money/DomainEvent/AggregateRoot)·FileStorage포트 | `com.thaiuniontour.common` |
| `domain` | 엔티티·리포지토리·도메인서비스·전략포트 (컨텍스트별 패키지) | `com.thaiuniontour.domain.{catalog,reservation,payment,member}` |
| `api` | 고객 REST + Outbox Relayer + 결제 Consumer | `com.thaiuniontour.api.*` |
| `admin` | 운영 Thymeleaf(상품·일정·카테고리·예약/입금승인) | `com.thaiuniontour.admin.*` |

- **스키마-per-도메인**: `catalog / reservation / payment / member / common` (교차 스키마 FK 없음, UUID 논리참조)
- **마이그레이션**: `api/src/main/resources/db/migration/V1~V7` (Flyway, api만 소유)
- **DB**: 전 엔티티 UUID PK. 예약번호(`reservation_no`)는 표시용 랜덤코드, 매직링크(`lookup_token`).

### 프론트 (`frontend/`, Next.js 14 App Router)
- 페이지: `app/page`(홈) `app/c/[...slug]`(카테고리+필터) `app/search` `app/products/[id]`(BookingPanel) `app/reservations/[id]`(완료/폴링) `app/reservations/lookup` `app/reservations/token/[token]`
- API 레이어: `lib/api.ts` (서버 fetch=API_INTERNAL_BASE, 클라 fetch=/api 프록시)
- 테마: Tailwind 토큰 `brand`(코랄)·`cream`(배경)·`ink`(브라운), `tailwind.config.ts`

### 핵심 동시성·이벤트 흐름 (M2)
```
예약생성: 재고 원자차감(UPDATE WHERE stock>=qty) + Reservation INSERT  [1 트랜잭션]  → PENDING
입금승인(admin): Reservation CONFIRMED + Outbox INSERT  [1 트랜잭션]  → 이벤트 발행 보장
Relayer(@Scheduled): outbox READY → FOR UPDATE SKIP LOCKED → Redis XADD → PUBLISHED
결제 Consumer: Redis Stream 구독 → payment_history INSERT(UNIQUE 멱등) → XACK
고객: GET /payments/by-reservation/{id} 폴링 → PAID
```
상세 패턴: `docs/design/reference-patterns.md`

---

## 완료한 작업 (커밋 순)
| 커밋 | 내용 |
|---|---|
| 설계 7단계 + reference-patterns | 요구사항~백로그, 동시성·이벤트 패턴 |
| 08-discovery-and-policy | 검색·연관상품·취소/환불정책·예약확정 라이프사이클 |
| M0 스캐폴딩 (+확장성 보강) | 멀티모듈·compose·Flyway·포트 seam·AggregateRoot |
| 비회원 재진입 | reservation_no + lookup_token(매직링크) |
| M1 백엔드 | catalog 도메인 + 검색/상세 API(Specification) |
| M1 admin+프론트 | 상품/카테고리 등록 + 홈/검색/상세 |
| M1 카테고리 랜딩 | `/c/[...slug]` + 필터(지역·가격) + ProductCard 'use client' 버그픽스 |
| 테마 적용 | 라이트 웜(코랄/크림/브라운) |
| M2 백엔드 코어 | Reservation·Outbox·Relayer·Consumer·재고전략 |
| M2 schedules+bankInfo | 날짜별 재고 조회 + 예약상세 계좌안내 |
| M2 UI | admin 입금승인·일정 + 프론트 예약 플로우 |

**검증 완료(실제 end-to-end)**: 선착순 oversell 0%(10동시→1성공), 이벤트 유실0%·중복결제0%(멱등), admin 입금승인→PAID, 취소→재고복원.

---

## 남은 작업 (TODO)

### M2 잔여
- [ ] **입금기한 자동 만료**: `ReservationExpirySweeper`(@Scheduled) — PENDING + `payment_deadline < now` → cancel(EXPIRED) + 재고복원. (스키마·도메인·`findByStatusAndPaymentDeadlineBefore` 준비됨)
- [ ] **취소 환불정책 산정**: CONFIRMED 취소 시 `cancellation_policy` 규칙으로 환불액 계산 → `payment.refund` 생성, admin 환불 수동완료(`/admin/refunds`).
- [ ] (사소) 예약완료 페이지에 **상품명** 표시 — 현재 예약상세 API가 productId만 반환. ResDetail에 productName 조인 또는 프론트에서 getProduct 추가 호출.

### M1 잔여
- [ ] 연관상품 큐레이션 admin + `/products/{id}/related` (스키마 `product_related` 준비됨)
- [ ] 취소정책 CRUD admin + 상품 연결 + 상세 노출 (스키마 준비됨)
- [ ] 상품 이미지 다중 업로드(`product_image` + `FileStorage` 로컬볼륨 구현)
- [ ] 검색 고도화: tsvector 랭킹·자동완성(`/products/autocomplete`)·가격/날짜 패싯 통합

### M3 (v1)
- [ ] 회원가입/로그인/JWT(refresh)·`/auth/me` (스키마 `member` 준비됨)
- [ ] 마이페이지(내 예약/취소)
- [ ] 다국어 라벨(ko/en) Provider + `/labels`
- [ ] Spring 프로필 분리(application-local/prod), 컨테이너화(backend/admin/nginx compose)

### M4 (v2, seam만 열림)
- [ ] 간편결제/카드 PG(`PaymentGateway` 어댑터 + 공통코드 활성화), 자동입금확인(`FirmBankingConfirmation`)
- [ ] 좌석 임시점유(hold/TTL), 폭주 특가(`RedisCounterStrategy`)
- [ ] 파이 배포 확정(arm64 buildx·TLS·CI/CD·백업)

> 우선순위/의존관계 상세: `docs/design/07-backlog.md`

---

## 알아둘 점 (gotcha)
- 포트: postgres **5433**, redis **6380** (충돌 회피). application.yml 기본값에 반영됨.
- admin은 Flyway 비활성(api가 마이그레이션 소유). 새 백엔드 모듈 추가 시 `flyway.enabled:false` 필수.
- 엔티티는 전역 default_schema 없이 **`@Table(schema=...)` 명시 필수**.
- 프론트 서버컴포넌트 fetch는 `API_INTERNAL_BASE`(절대URL), 클라이언트 fetch는 `/api`(next.config rewrites 프록시).
- 데모 데이터는 `scripts/seed-demo.sql` (재실행 가능). 운영 데이터는 admin에서 직접 등록.

---

## 배포 (친구 Pi + Cloudflare Tunnel) — 2026-09-08 세팅

- 서버: `ssh seonghanim@125.240.159.13` (arm64 Pi 5), 앱 위치 `~/workspace/huiyeong/thaiuniontour/`
- 스택: `docker/docker-compose.prod.yml` (postgres·redis·api·admin·frontend·cloudflared). 호스트 포트 노출 없음. admin만 `127.0.0.1:18081`.
- 시크릿: Pi의 `docker/.env` (`DB_PASSWORD`). 저장소엔 없음.
- 공개 경로: 도메인 → Cloudflare Tunnel → `frontend:3000` (api는 Next rewrite로 컨테이너 내부 통신). admin은 비인증이므로 **외부 미노출**.

### 재배포 (로컬에서)
```bash
cd thaiuniontour/backend && ./gradlew :api:bootJar :admin:bootJar
cd .. && tar czf /tmp/tut-deploy.tgz --exclude=frontend/node_modules --exclude=frontend/.next --exclude='frontend/.env*' \
  backend/Dockerfile backend/api/build/libs/api.jar backend/admin/build/libs/admin.jar frontend docker/docker-compose.prod.yml scripts/seed-demo.sql
scp /tmp/tut-deploy.tgz seonghanim@125.240.159.13:/home/seonghanim/workspace/huiyeong/thaiuniontour/
ssh seonghanim@125.240.159.13 'cd ~/workspace/huiyeong/thaiuniontour && tar xzf tut-deploy.tgz && rm tut-deploy.tgz \
  && cd docker && docker-compose -f docker-compose.prod.yml up -d --build'
```
> Pi는 `docker compose`(플러그인) 없음 → `docker-compose`(v2 standalone) 사용. 프론트 빌드는 Pi에서 약 2~3분.

### 운영 콘솔 접근
```bash
ssh -L 18081:127.0.0.1:18081 seonghanim@125.240.159.13   # 그 후 http://localhost:18081/admin
```

### 터널 (CLI 로컬관리 방식, Zero Trust 카드등록 불필요)
- 터널명 `thaiuniontour` (id 08acb9b7-…), 설정·인증서: Pi `~/workspace/huiyeong/thaiuniontour/cloudflared/` (cert.pem·<uuid>.json·config.yml). 저장소엔 없음.
- DNS: `thaiuniontour.com`, `www` → 터널 CNAME (Cloudflare NS: ed/wally.ns.cloudflare.com). 라우팅은 `config.yml` ingress에서 수정.
- 확인: `docker logs tut-cloudflared | grep Registered`. 호스트 추가 시:
  `docker run --rm -v ~/workspace/huiyeong/thaiuniontour/cloudflared:/home/nonroot/.cloudflared cloudflare/cloudflared:latest tunnel route dns thaiuniontour <host>` 후 config.yml에 ingress 추가 → `docker-compose -f docker-compose.prod.yml restart cloudflared`.
- 미설정: Cloudflare 대시보드 SSL/TLS → Edge Certificates → **Always Use HTTPS** (http→https 리다이렉트).
