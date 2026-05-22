interface RunningGaugeProps {
  concurrency: number
}

/**
 * 실행 중 표시: 요청이 쏟아지는 느낌의 indeterminate 진행 바 + 펄스 텍스트.
 * (요청별 진행률은 스트리밍되지 않으므로 indeterminate 로 표현)
 */
export default function RunningGauge({ concurrency }: RunningGaugeProps) {
  return (
    <section className="rounded-2xl border border-yellow-500/30 bg-slate-900/40 p-5">
      <div className="flex items-center justify-between">
        <span className="animate-pulse-glow text-sm font-semibold text-yellow-300">
          ⚡ {concurrency.toLocaleString()}건의 동시 요청을 발사하는 중…
        </span>
        <span className="text-xs text-slate-500">실시간 로그 ↓</span>
      </div>
      <div className="relative mt-3 h-2.5 w-full overflow-hidden rounded-full bg-slate-800">
        <div className="animate-indeterminate absolute top-0 h-full rounded-full bg-gradient-to-r from-yellow-400 to-orange-500" />
      </div>
    </section>
  )
}
