# thaiuniontour 설계 개요 (Design Overview)

태국 여행상품 판매 웹사이트 **thaiuniontour** 의 설계 진행 현황 인덱스.
설계는 `/oh-my-claudecode` 환경에서 **design 스킬**로 단계별 진행한다.

## 프로젝트 한 줄 정의
> 태국 여행상품(투어·패키지)을 온라인으로 탐색·예약·결제할 수 있는 커머스 웹사이트.

## 기술 스택
- **백엔드**: Spring Boot 3.x / Java 17 / Gradle 멀티모듈(`api · domain · common`)
- **프론트**: Next.js 14 (App Router) / React 18 / TypeScript / Tailwind
- **DB**: PostgreSQL 16 (UUID · TIMESTAMPTZ · JSONB · 스키마-per-도메인)
- **메시징/락**: Redis (Streams + 선착순 동시성 제어)
- **배포**: Docker / docker-compose
- **구조**: 모노레포 (`backend/ · frontend/ · docker/ · docs/`)
- **레퍼런스**: athometrip(커머스/계층 패턴) + FlashArena(동시성·이벤트 결제) → `reference-patterns.md`

## 핵심 비기능 요구
- **선착순/동시성 안전**: 한정 좌석·특가 상품 oversell 방지 (원자 차감 / Redis 카운터 게이트)
- **이벤트 기반 결제**: Transactional Outbox → Redis Streams → 멱등 Consumer (유실 0% + exactly-once 효과)
- **도메인 격리**: 스키마-per-도메인, 이벤트로만 통신

## 설계 단계 체크리스트

| # | 단계 | 산출물 | 상태 |
|---|------|--------|------|
| 0 | 레퍼런스 패턴(동시성·이벤트결제) | `reference-patterns.md` | ✅ 완료 |
| 1 | 요구사항 정의 | `01-requirements.md` | ✅ 완료 |
| 2 | 도메인 모델 & 용어사전 (DDD) | `02-domain.md` | ✅ 완료 |
| 3 | 데이터 모델(ERD) | `03-erd.md` | ✅ 완료 |
| 4 | API 설계 | `04-api.md` | ✅ 완료 |
| 5 | 화면/IA 설계 | `05-frontend.md` | ✅ 완료 |
| 6 | 아키텍처 & 인프라 | `06-infra.md` | ✅ 완료 |
| 7 | 백로그 & 마일스톤 | `07-backlog.md` | ✅ 완료 |

> 상태 표기: ⬜ 대기 / 🟡 진행중 / ✅ 완료

## 설계 표준
- **선택적 DDD**: 예약·결제 = 전술적 DDD(Aggregate·VO·Domain Event·Repository 포트), 전시/라벨/공통코드 = 경량.
- **확장 seam(코드 작성 시 열어둘 곳)**: 결제수단(`PaymentGateway`+공통코드)/입금확인(`PaymentConfirmation`)/재고차감(`StockDeductionStrategy`)/이벤트 payload `version`/경량 컨텍스트 승격(package-by-context)/좌석 hold·TTL.

## 다음 액션
**🎉 설계 7단계 전부 완료.** 다음은 **구현 단계** — `07-backlog.md`의 **M0 스캐폴딩**부터(모노레포 골격 + compose(postgres·redis) + Flyway DDL). 세로 슬라이스(상품 등록→예약→결제완료 관통) 권장.

> 배포 상세(파이 arm64·TLS·CI/CD·백업)는 요청대로 **실제 배포 착수 시** 06-infra를 확장하여 확정.

## 프론트 UI 방침
athometrip-front 참고하되 **사용성 개선**: 데이터 기반 테마 3단계 카테고리(메가메뉴) + 글로벌 검색 + 지역/가격 필터 + 브레드크럼. 하드코딩 라우트 금지(동적 `/c/[...slug]`, `/search`).
