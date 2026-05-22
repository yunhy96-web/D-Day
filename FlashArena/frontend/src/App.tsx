import { useCallback, useEffect, useRef, useState } from 'react'
import Controls from '@/components/Controls'
import LogTerminal from '@/components/LogTerminal'
import ResultReport from '@/components/ResultReport'
import RunningGauge from '@/components/RunningGauge'
import StatusBadge, { type ConnState } from '@/components/StatusBadge'
import {
  fetchResult,
  login,
  startRun,
  streamUrl,
  type SimulationMode,
  type SimulationResult,
} from '@/lib/api'

export default function App() {
  const [token, setToken] = useState<string | null>(null)
  const [conn, setConn] = useState<ConnState>('connecting')
  const [loginError, setLoginError] = useState<string | null>(null)

  const [mode, setMode] = useState<SimulationMode>('SYNC')
  const [concurrency, setConcurrency] = useState(1000)
  const [initialStock, setInitialStock] = useState(100)

  const [running, setRunning] = useState(false)
  const [logs, setLogs] = useState<string[]>([])
  const [result, setResult] = useState<SimulationResult | null>(null)
  const [runError, setRunError] = useState<string | null>(null)

  const esRef = useRef<EventSource | null>(null)
  const runIdRef = useRef<string | null>(null)

  const doLogin = useCallback(async () => {
    setConn('connecting')
    setLoginError(null)
    try {
      const res = await login()
      setToken(res.accessToken)
      setConn('online')
    } catch (e) {
      setConn('error')
      setLoginError(e instanceof Error ? e.message : '알 수 없는 오류')
    }
  }, [])

  useEffect(() => {
    doLogin()
    return () => {
      esRef.current?.close()
    }
  }, [doLogin])

  const closeStream = useCallback(() => {
    esRef.current?.close()
    esRef.current = null
  }, [])

  const handleRun = useCallback(async () => {
    if (!token || running) return
    setRunError(null)
    setResult(null)
    setLogs([])
    closeStream()
    setRunning(true)

    try {
      const run = await startRun(token, { mode, concurrency, initialStock })
      runIdRef.current = run.runId

      const es = new EventSource(streamUrl(token, run.runId))
      esRef.current = es

      es.addEventListener('log', (ev) => {
        setLogs((prev) => [...prev, (ev as MessageEvent).data as string])
      })

      es.addEventListener('result', (ev) => {
        try {
          const parsed = JSON.parse((ev as MessageEvent).data as string) as SimulationResult
          setResult(parsed)
        } catch {
          /* 무시 */
        }
        setRunning(false)
        closeStream()
      })

      es.onerror = async () => {
        // SSE 가 끊겼는데 아직 결과가 없으면 폴링 폴백.
        closeStream()
        if (runIdRef.current) {
          try {
            const fallback = await fetchResult(token, runIdRef.current)
            if (fallback) setResult(fallback)
          } catch {
            /* 무시 */
          }
        }
        setRunning(false)
      }
    } catch (e) {
      setRunError(e instanceof Error ? e.message : '실행 실패')
      setRunning(false)
    }
  }, [token, running, mode, concurrency, initialStock, closeStream])

  return (
    <div className="mx-auto min-h-full max-w-5xl px-4 py-8 sm:px-6">
      <header className="mb-8 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="bg-gradient-to-r from-yellow-300 via-orange-400 to-yellow-300 bg-clip-text text-4xl font-black tracking-tight text-transparent">
            FlashArena ⚡
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            동시성 버그 vs 분산 락 — 재고 오버셀을 실시간으로 재현하는 시뮬레이터
          </p>
        </div>
        <StatusBadge state={conn} onRetry={doLogin} />
      </header>

      {loginError && (
        <div className="mb-4 rounded-xl border border-red-500/40 bg-red-950/30 px-4 py-3 text-sm text-red-300">
          로그인 실패: {loginError} — 백엔드(8092)가 떠 있는지 확인 후 재시도하세요.
        </div>
      )}

      <div className="space-y-5">
        <Controls
          mode={mode}
          concurrency={concurrency}
          initialStock={initialStock}
          running={running}
          canRun={conn === 'online'}
          onModeChange={setMode}
          onConcurrencyChange={setConcurrency}
          onInitialStockChange={setInitialStock}
          onRun={handleRun}
        />

        {runError && (
          <div className="rounded-xl border border-red-500/40 bg-red-950/30 px-4 py-3 text-sm text-red-300">
            {runError}
          </div>
        )}

        {running && <RunningGauge concurrency={concurrency} />}

        {result && <ResultReport result={result} />}

        <LogTerminal logs={logs} />
      </div>

      <footer className="mt-10 text-center text-xs text-slate-600">
        FlashArena — Spring Boot · Redisson · SSE · Vite + React + Tailwind
      </footer>
    </div>
  )
}
