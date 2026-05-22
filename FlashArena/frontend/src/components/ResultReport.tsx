import type { SimulationResult } from '@/lib/api'
import { useCountUp } from '@/lib/useCountUp'

interface ResultReportProps {
  result: SimulationResult
}

function StatCard({
  label,
  value,
  sub,
  tone = 'default',
}: {
  label: string
  value: string | number
  sub?: string
  tone?: 'default' | 'success' | 'fail' | 'stock'
}) {
  const toneClass = {
    default: 'text-slate-100',
    success: 'text-emerald-400',
    fail: 'text-amber-400',
    stock: 'text-sky-400',
  }[tone]
  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/50 p-4">
      <div className="text-xs font-medium uppercase tracking-wider text-slate-500">{label}</div>
      <div className={`mt-1 text-2xl font-bold tabular-nums ${toneClass}`}>{value}</div>
      {sub && <div className="mt-0.5 text-xs text-slate-500">{sub}</div>}
    </div>
  )
}

export default function ResultReport({ result }: ResultReportProps) {
  const success = useCountUp(result.successCount)
  const fail = useCountUp(result.failCount)
  const finalStock = useCountUp(result.finalStock)
  const seconds = (result.elapsedMs / 1000).toFixed(2)

  return (
    <section className="space-y-4">
      {result.oversold ? (
        <div className="animate-fade-in-up rounded-2xl border-2 border-red-500/60 bg-red-950/40 p-5 text-center shadow-lg shadow-red-900/30">
          <p className="text-lg font-extrabold text-red-300">
            ⚠️ Race Condition 발생 — 재고보다 많이 팔림!
          </p>
          <p className="mt-1 text-sm text-red-400/80">
            락이 없어 Lost Update 가 일어나 초기 재고 {result.initialStock}개보다 많은{' '}
            {result.successCount}건이 판매되었습니다.
          </p>
        </div>
      ) : (
        <div className="animate-fade-in-up rounded-2xl border-2 border-emerald-500/60 bg-emerald-950/30 p-5 text-center shadow-lg shadow-emerald-900/30">
          <p className="text-lg font-extrabold text-emerald-300">
            ✅ 분산 락으로 정확히 처리됨
          </p>
          <p className="mt-1 text-sm text-emerald-400/80">
            동시 요청 {result.concurrency.toLocaleString()}건 중 정확히 재고{' '}
            {result.initialStock}개만 판매되어 오버셀이 없습니다.
          </p>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <StatCard label="총 요청 수" value={result.concurrency.toLocaleString()} />
        <StatCard label="성공 건수" value={success.toLocaleString()} tone="success" />
        <StatCard label="실패 건수" value={fail.toLocaleString()} tone="fail" />
        <StatCard
          label="최종 남은 재고"
          value={finalStock.toLocaleString()}
          sub={`초기 ${result.initialStock}`}
          tone="stock"
        />
        <StatCard label="소요 시간" value={`${result.elapsedMs}ms`} sub={`${seconds}s`} />
        <StatCard
          label="오버셀 여부"
          value={result.oversold ? '발생' : '없음'}
          tone={result.oversold ? 'fail' : 'success'}
        />
      </div>
    </section>
  )
}
