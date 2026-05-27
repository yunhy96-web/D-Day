/**
 * Part-A 백엔드 계약을 그대로 반영한 API 클라이언트.
 * - POST /api/auth/login            → { accessToken, ... }
 * - POST /api/simulator/run         → 202 { runId, mode, concurrency, initialStock }
 * - GET  /api/simulator/stream/:id?token=  (text/event-stream) → event:log / event:result
 * - GET  /api/simulator/result/:id  (Bearer) → SimulationResult | 404
 */

export const API_BASE: string =
  import.meta.env.VITE_API_BASE ?? 'http://localhost:8092'

export type SimulationMode = 'SYNC' | 'REDIS_LOCK' | 'REDIS_COUNTER'

export interface LoginResponse {
  accessToken: string
  tokenType: string
  userId: string
  role: string
  expiresIn: number
}

export interface RunResponse {
  runId: string
  mode: SimulationMode
  concurrency: number
  initialStock: number
}

/** SimulationResult.java 와 1:1 대응. */
export interface SimulationResult {
  runId: string
  mode: SimulationMode
  concurrency: number
  initialStock: number
  successCount: number
  failCount: number
  finalStock: number
  expectedStock: number
  oversold: boolean
  elapsedMs: number
  startedAt: string
  finishedAt: string
}

export interface RunParams {
  mode: SimulationMode
  concurrency: number
  initialStock: number
}

/** 고정 데모 계정으로 자동 로그인. */
export async function login(): Promise<LoginResponse> {
  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'user', password: '1234' }),
  })
  if (!res.ok) {
    throw new Error(`로그인 실패 (HTTP ${res.status})`)
  }
  return res.json()
}

/** 비동기 run 시작 → runId 반환 (202). */
export async function startRun(token: string, params: RunParams): Promise<RunResponse> {
  const res = await fetch(`${API_BASE}/api/simulator/run`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(params),
  })
  if (!res.ok) {
    throw new Error(`시뮬레이션 시작 실패 (HTTP ${res.status})`)
  }
  return res.json()
}

/** SSE 폴백: 저장된 결과 조회. 미존재 시 null. */
export async function fetchResult(
  token: string,
  runId: string,
): Promise<SimulationResult | null> {
  const res = await fetch(`${API_BASE}/api/simulator/result/${runId}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (res.status === 404) return null
  if (!res.ok) {
    throw new Error(`결과 조회 실패 (HTTP ${res.status})`)
  }
  return res.json()
}

/** SSE 구독 URL (EventSource 는 헤더를 못 보내므로 token 을 쿼리로 전달). */
export function streamUrl(token: string, runId: string): string {
  return `${API_BASE}/api/simulator/stream/${runId}?token=${encodeURIComponent(token)}`
}
