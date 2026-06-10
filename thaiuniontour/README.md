# thaiuniontour

태국 여행상품(투어·패키지) 판매 커머스. 설계 문서는 [`docs/design/`](docs/design/00-OVERVIEW.md) 참조.

## 스택
- 백엔드: Spring Boot 3.2 / Java 17 / Gradle 멀티모듈(`common · domain · api · admin`)
- 프론트: Next.js 14 (App Router) / React 18 / TS / Tailwind
- DB: PostgreSQL 16 (스키마-per-도메인) · 메시징/락: Redis
- 배포: Docker (라즈베리파이 arm64 타깃)

## 로컬 실행

### 1) 인프라 (PostgreSQL · Redis)
```bash
cd docker
cp .env.example .env          # 최초 1회
docker compose up -d          # postgres:5433, redis:6380 (호스트 포트)
```

### 2) 백엔드 (api: 8080 / admin: 8081)
```bash
cd backend
./gradlew :api:bootRun        # Flyway가 스키마 자동 마이그레이션
./gradlew :admin:bootRun      # (별도 터미널) 운영 콘솔
# 확인: curl http://localhost:8080/api/v1/ping
```

### 3) 프론트엔드 (3000)
```bash
cd frontend
cp .env.example .env.local     # 최초 1회
pnpm install
pnpm dev                       # http://localhost:3000  (/api → 8080 프록시)
```

## 포트
| 서비스 | 호스트 포트 | 비고 |
|---|---|---|
| PostgreSQL | 5433 | 컨테이너 내부 5432 (다른 프로젝트 충돌 회피) |
| Redis | 6380 | 컨테이너 내부 6379 |
| api | 8080 | 고객 REST + Relayer + 결제 Consumer |
| admin | 8081 | Thymeleaf 운영 콘솔 |
| frontend | 3000 | Next.js |

## 진행 현황
- ✅ 설계 7단계 완료 (`docs/design/`)
- ✅ **M0 스캐폴딩** — 멀티모듈 골격 · docker-compose · Flyway DDL(V1~V6) · 공통응답/예외 · DDD 베이스
- ▶ 다음: M1 (Catalog & 탐색) — `docs/design/07-backlog.md`
