# FlashArena ⚡ — Frontend (Playground 대시보드)

동시성 시뮬레이터 백엔드(Phase 1–5A)를 위한 Vite + React + TypeScript + Tailwind v3 대시보드.

## 실행

전제: `flasharena` DB + Redis 가 떠 있어야 합니다.

```bash
# 1) 백엔드 (포트 8092)
cd ../backend
./gradlew bootRun

# 2) 프런트엔드 (포트 5173)
cd ../frontend
npm install
npm run dev
# 브라우저에서 http://localhost:5173 열기
```

앱 마운트 시 고정 데모 계정(`user`/`1234`)으로 자동 로그인합니다.

## 환경 변수

- `VITE_API_BASE` — 백엔드 베이스 URL. 미설정 시 `http://localhost:8092`.

## 백엔드 계약

- `POST /api/auth/login` → `{ accessToken, ... }`
- `POST /api/simulator/run` (Bearer) → `202 { runId, mode, concurrency, initialStock }`
- `GET  /api/simulator/stream/{runId}?token=<jwt>` (text/event-stream) → `event: log` … `event: result`
- `GET  /api/simulator/result/{runId}` (Bearer) → `SimulationResult` | `404`
