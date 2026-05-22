# FlashArena ⚡

**동시성 & 분산 시스템 장애 대응 패턴 시뮬레이터 대시보드**

선착순 플래시 세일을 무대로, "락 없이 동시 요청을 날리면 재고가 음수로 깨지고 /
Redis 분산 락을 걸면 정확히 막힌다"를 면접관에게 **실시간 로그로 시각적으로 증명**하는
1인 개발 포트폴리오 프로젝트.

## 증명하려는 것
- 🏁 **Race Condition** — No-Lock 모드에서 재고 오버셀/음수 발생 연출
- 🔒 **Redis 분산 락** — 정해진 재고만큼만 차감, 나머지는 정확히 실패 처리
- ✉️ **Transactional Outbox** — 주문 트랜잭션과 이벤트를 원자적으로 기록 후 비동기 발행
- ♻️ **소비자 멱등성** — 같은 주문 이벤트 중복 수신 시 결제 1회만 처리

## 스택
- Backend: Java 21 / Spring Boot 3.x  (port 8092)
- Frontend: Vite + React + TS + Tailwind
- Redis 1대 (분산 락 + 메시지 큐 겸용) · PostgreSQL 1 인스턴스(3 스키마 격리)

## 아키텍처 제약
단일 물리 DB 안에서 `auth` / `order` / `payment` 스키마로 도메인을 격리하고,
스키마 간 FK·JOIN·공통 트랜잭션을 금지해 MSA 환경을 흉내 낸다. 모든 PK는 UUID.

## 진행 상황
- [x] **Phase 1** — 초기 설정 가이드 & DB DDL → [`docs/PHASE1-SETUP.md`](docs/PHASE1-SETUP.md)
- [x] **Phase 2** — auth: JWT 공용 계정(user/1234) 로그인 + 인가 인터셉터 (`backend/`)
- [x] **Phase 3** — order: 동시성 시뮬레이터 (SYNC 오버셀 연출 / REDIS_LOCK 정확 차감), `POST /api/simulator/run`
- [x] **Phase 4** — Transactional Outbox(`order.outbox`) + Redis Streams 릴레이어 + payment 멱등 소비(`payment.payment_history`)
- [x] **Phase 5** — Frontend Playground 대시보드(`frontend/`, Vite+React+Tailwind) + 백엔드 SSE 실시간 로그(`/api/simulator/stream/{runId}`)

## 실행 방법 (로컬)
> 사전: 공용 PostgreSQL(`checkstock-postgres`)·Redis(`flasharena-redis`) 컨테이너가 떠 있어야 함.
1. 백엔드: `cd backend && ./gradlew bootRun`  (port 8092)
2. 프론트: `cd frontend && npm install && npm run dev` → 브라우저에서 안내된 주소(기본 5173, 점유 시 5174/5175) 열기
3. 자동 로그인(user/1234) → 모드 선택 → 동시 요청/재고 입력 → 🚀 시작 → 실시간 로그 + 결과 리포트

## 빠른 시작
[`docs/PHASE1-SETUP.md`](docs/PHASE1-SETUP.md) 참고.
