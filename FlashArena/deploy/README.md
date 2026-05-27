# FlashArena 배포 가이드

배포 단위는 셋이다: **정적 프론트(Vite dist) / 백엔드(Spring Boot) / 인프라(PostgreSQL + Redis)**.

## 두 가지 토폴로지

### 방식 A — 분리 배포 (서로 다른 도메인, CORS 필요)

```
방문자 ─HTTPS─▶ app.example.com   (정적 프론트: Vercel/Netlify/S3 등)
   └────HTTPS───▶ api.example.com   (백엔드)  ──▶ Postgres + Redis
```

- 프론트 빌드: `VITE_API_BASE=https://api.example.com`
- 백엔드: `CORS_ALLOWED_ORIGINS=https://app.example.com`

### 방식 B — 같은 도메인 + 리버스 프록시 (CORS 불필요, 추천)

```
방문자 ─HTTPS─▶ flasharena.example.com (nginx)
                  ├─ /      → 정적 프론트 파일
                  └─ /api/  → 백엔드(:8092) 프록시  ──▶ Postgres + Redis
```

- nginx 설정: [`nginx/flasharena.conf`](./nginx/flasharena.conf)
- 프론트 빌드: `VITE_API_BASE=`  ← **빈 값**으로 두면 `/api/...` 상대경로로 호출(같은 origin)
- 같은 origin 이라 `CORS_ALLOWED_ORIGINS` 불필요
- **SSE 는 `proxy_buffering off` 필수** (conf 에 이미 반영)

## 백엔드 환경변수 (배포 시 반드시 override)

| 환경변수 | 용도 | dev 기본값(절대 운영 금지) |
|----------|------|------------------------------|
| `CORS_ALLOWED_ORIGINS` | 허용 origin(콤마 구분) | `http://localhost:*,http://127.0.0.1:*` |
| `JWT_SECRET` | JWT 서명 키(32byte+) | dev 더미 키 |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | PostgreSQL 접속 | `...20251220` |
| `AUTH_PW` | 데모 계정 비밀번호 | `1234` |
| `REDIS_HOST` / `REDIS_PORT` | Redis 접속 | `127.0.0.1:6379` |

## 배포 체크리스트

1. [ ] 프론트·백엔드 **둘 다 HTTPS** (mixed content 차단 방지 — SSE 포함)
2. [ ] `VITE_API_BASE` 설정 후 `npm run build`
3. [ ] 백엔드를 **상시 서버**에 배포 (서버리스/엣지는 장시간 SSE 연결을 못 버틸 수 있음)
4. [ ] 방식 A면 `CORS_ALLOWED_ORIGINS`, 방식 B면 nginx 프록시
5. [ ] 위 표의 env 전부 override (특히 `JWT_SECRET`)
6. [ ] 프록시 뒤라면 SSE 엔드포인트 `proxy_buffering off` 확인
