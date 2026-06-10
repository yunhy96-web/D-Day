# 06. 아키텍처 & 인프라 (Docker)

**전부 Docker 기반**(DB·Redis 포함). 개발=로컬 docker-compose, 배포 타깃=**라즈베리 파이(arm64/Linux)**.
배포 세부(도메인·TLS·CI/CD)는 **실제 배포 시 확정** — 본 문서는 컨테이너 구성과 로컬 개발 기준까지.

## 시스템 구성도
```mermaid
flowchart TB
  U[고객 브라우저] --> NG[nginx 리버스 프록시]
  A[운영자] --> NG
  NG -->|/| FE[frontend\nNext.js 14]
  NG -->|/api| API[backend-api\nSpring Boot REST]
  NG -->|/admin| ADM[backend-admin\nThymeleaf]
  FE -->|SSR fetch| API

  API --> PG[(PostgreSQL\n스키마-per-도메인)]
  ADM --> PG
  API -->|outbox INSERT same tx| PG
  RLY[Relayer\n@Scheduled] -->|READY 폴링 SKIP LOCKED| PG
  RLY -->|XADD| RD[(Redis\nStreams)]
  RD -->|XREAD consumer group| PAY[결제 Consumer]
  PAY -->|payment_history UNIQUE| PG

  subgraph backend[backend JVM 컨테이너]
    API
    RLY
    PAY
  end
```
> MVP는 Relayer·결제 Consumer를 **api 컨테이너 내부 빈**(`@Scheduled`/스트림 리스너)으로 둠 → 컨테이너 수 최소화(Pi 자원 절약).
> 트래픽 증가 시 `scheduler` 모듈을 **별도 컨테이너로 분리**(seam, 코드 무변경).

## 컨테이너 구성

| 서비스 | 이미지(베이스) | 역할 | 포트(내부) |
|---|---|---|---|
| `nginx` | nginx:alpine | 리버스 프록시·정적·TLS종단(배포시) | 80/443 |
| `frontend` | node(슬림) → Next standalone | 고객 UI(SSR) | 3000 |
| `backend-api` | eclipse-temurin:17-jre | 고객 REST + Relayer + 결제 Consumer | 8080 |
| `backend-admin` | eclipse-temurin:17-jre | 운영 Thymeleaf | 8081 |
| `postgres` | postgres:16-alpine | DB(스키마-per-도메인) | 5432 |
| `redis` | redis:7-alpine | 이벤트 Stream + 동시성 락/카운터 | 6379 |

- **nginx 라우팅**: `/` → frontend, `/api/` → backend-api, `/admin/` → backend-admin. 고객/운영 트래픽 분리.
- **api·admin 분리**(athometrip식 모듈 분리). Pi 자원 빠듯하면 한 JVM에 합치는 것도 가능(seam).

## nginx 라우팅(스케치)
```nginx
server {
  listen 80;
  location /api/  { proxy_pass http://backend-api:8080; }
  location /admin/{ proxy_pass http://backend-admin:8081; }
  location /      { proxy_pass http://frontend:3000; }   # Next SSR
}
```

## docker-compose (로컬 개발 기준 스케치)
```yaml
# docker/docker-compose.yml  (실제 파일은 구현 단계에서 작성)
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: thaiuniontour
      POSTGRES_USER: tut
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    ports: ["5432:5432"]          # 로컬 개발 편의(배포시 미노출)

  redis:
    image: redis:7-alpine
    command: ["redis-server", "--appendonly", "yes"]
    volumes: [ "redisdata:/data" ]
    ports: ["6379:6379"]

  backend-api:
    build: { context: ../backend, dockerfile: api/Dockerfile }
    environment:
      SPRING_PROFILES_ACTIVE: ${PROFILE:-local}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/thaiuniontour
      SPRING_DATASOURCE_USERNAME: tut
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      JAVA_OPTS: "-Xms128m -Xmx384m"        # Pi 메모리 고려
    depends_on: [ postgres, redis ]

  backend-admin:
    build: { context: ../backend, dockerfile: admin/Dockerfile }
    environment: { SPRING_PROFILES_ACTIVE: ${PROFILE:-local}, ... }
    depends_on: [ postgres ]

  frontend:
    build: { context: ../frontend }
    environment:
      NEXT_PUBLIC_API_BASE: /api/v1
    depends_on: [ backend-api ]

  nginx:
    image: nginx:alpine
    volumes: [ "./nginx/default.conf:/etc/nginx/conf.d/default.conf:ro",
               "uploads:/var/www/uploads:ro" ]
    ports: ["80:80"]
    depends_on: [ frontend, backend-api, backend-admin ]

volumes: { pgdata: , redisdata: , uploads: }
```

## DB 마이그레이션
- **Flyway**(backend 내장): `V1__schema_and_common` … `V6__seed`(03-erd.md 기준). api 부팅 시 자동 적용.
- 컨테이너 기동 순서: postgres healthcheck → backend(Flyway 마이그레이션) → frontend.

## 이미지 업로드 저장소
- MVP: **로컬 볼륨**(`uploads:`) 바인드 + nginx가 `/uploads/` 정적 서빙. admin이 상품 이미지 업로드.
- seam: 추후 S3 호환 오브젝트 스토리지(MinIO 등)로 교체(인터페이스 `FileStorage` 포트).

## 환경 분리
| 프로필 | 용도 | DB/Redis | 비고 |
|---|---|---|---|
| `local` | 로컬 개발 | compose 컨테이너 | 포트 노출, 디버그 로그 |
| `prod` | 라즈베리 파이 | compose(파이 내부) | 포트 미노출, TLS, 시크릿 외부주입 |
- 시크릿(`DB_PASSWORD`, JWT secret)은 `.env`(git 제외) / 배포시 파이 환경변수·secret 파일.
- compose override: `docker-compose.prod.yml`로 prod 차이만 덮어쓰기.

## 라즈베리 파이(배포 타깃) 고려 — 개요만
- **arm64 아키텍처**: 사용 이미지(postgres/redis/nginx/temurin/node) 모두 arm64 지원 ✅. 앱 이미지는 **`buildx` 멀티아치 빌드**(x86 개발PC에서 arm64 빌드) 또는 파이에서 직접 빌드.
- **메모리**: JVM 2개(api+admin)가 부담 → `-Xmx` 제한, 필요시 api/admin 단일 JVM 통합. Pi는 8GB 모델 권장.
- **저장소**: SD카드 수명 → DB 볼륨은 **SSD(USB) 권장**, 로그 회전.
- **전원/가용성**: 단일 노드라 무중단 아님. 재시작 정책 `restart: unless-stopped`.
- ⚠️ 위 항목은 **실제 배포 시 상세 확정**(도메인/DDNS, TLS(Let's Encrypt), 백업, CI/CD, 모니터링).

## 이벤트 릴레이어 운영(기본값, 추후 튜닝)
| 항목 | 기본 | 비고 |
|---|---|---|
| 릴레이 주기 | 500ms `@Scheduled` | 부하 보고 조정 |
| 배치 크기 | 100 | |
| 재시도 상한 | 10 → 초과 시 `FAILED`(준-DLQ) | admin서 재처리 화면 seam |
| Redis Stream 키 | `tut:reservation-events` | |
| Consumer group | `payment-workers` | 수평 확장 대비 |

## 결정 & 근거
- **DB도 컨테이너**: 개발/배포 환경 동일성(파이에서도 동일 compose). 데이터는 볼륨 영속.
- **Relayer/Consumer를 api 내부에**: Pi 자원 절약. 분리는 seam으로 열어둠.
- **api/admin 컨테이너 분리**: 트래픽·보안 경계 분리(athometrip식). 자원 빠듯 시 통합 가능.
- **배포 상세는 보류**: 요청대로 로컬 개발 우선, 파이 배포는 착수 시 별도 확정.

## 다음 단계(7단계 백로그)로 넘길 포인트
- 위 컨테이너/모듈을 구현 작업 단위로 분해, MVP→v1→v2 우선순위·마일스톤, 구현 착수 지점.
