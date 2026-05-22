import { useEffect, useRef } from 'react'
import { classifyLog } from '@/lib/logStyle'

interface LogTerminalProps {
  logs: string[]
}

/** 다크 터미널 스타일 로그 패널. 새 줄이 들어오면 맨 아래로 자동 스크롤. */
export default function LogTerminal({ logs }: LogTerminalProps) {
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = scrollRef.current
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  }, [logs])

  return (
    <section className="overflow-hidden rounded-2xl border border-slate-800 bg-black/60">
      <div className="flex items-center gap-2 border-b border-slate-800 bg-slate-900/70 px-4 py-2.5">
        <span className="h-3 w-3 rounded-full bg-red-500/80" />
        <span className="h-3 w-3 rounded-full bg-yellow-500/80" />
        <span className="h-3 w-3 rounded-full bg-green-500/80" />
        <span className="ml-2 font-mono text-xs text-slate-500">
          flasharena — live simulation log
        </span>
        <span className="ml-auto font-mono text-xs text-slate-600">{logs.length} lines</span>
      </div>
      <div
        ref={scrollRef}
        className="log-scroll h-72 overflow-y-auto px-4 py-3 font-mono text-[13px] leading-6"
      >
        {logs.length === 0 ? (
          <p className="text-slate-600">$ 시뮬레이션을 시작하면 로그가 여기에 실시간으로 흘러내립니다…</p>
        ) : (
          logs.map((line, i) => (
            <div key={i} className={`animate-fade-in-up ${classifyLog(line)}`}>
              <span className="mr-2 select-none text-slate-700">{String(i + 1).padStart(3, '0')}</span>
              {line}
            </div>
          ))
        )}
      </div>
    </section>
  )
}
