import type { SimulationMode } from '@/lib/api'

interface ControlsProps {
  mode: SimulationMode
  concurrency: number
  initialStock: number
  running: boolean
  canRun: boolean
  onModeChange: (mode: SimulationMode) => void
  onConcurrencyChange: (value: number) => void
  onInitialStockChange: (value: number) => void
  onRun: () => void
}

interface ModeCardProps {
  active: boolean
  disabled: boolean
  title: string
  desc: string
  accent: string
  onClick: () => void
}

function ModeCard({ active, disabled, title, desc, accent, onClick }: ModeCardProps) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className={[
        'flex-1 rounded-xl border p-4 text-left transition-all',
        disabled ? 'cursor-not-allowed opacity-50' : 'cursor-pointer hover:border-slate-500',
        active
          ? `border-transparent ring-2 ${accent} bg-slate-800/80 shadow-lg`
          : 'border-slate-700 bg-slate-900/40',
      ].join(' ')}
    >
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-slate-100">{title}</span>
        <span
          className={[
            'h-3 w-3 rounded-full',
            active ? 'bg-yellow-400' : 'bg-slate-600',
          ].join(' ')}
        />
      </div>
      <p className="mt-1 text-xs leading-relaxed text-slate-400">{desc}</p>
    </button>
  )
}

function NumberField({
  label,
  value,
  disabled,
  onChange,
}: {
  label: string
  value: number
  disabled: boolean
  onChange: (v: number) => void
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs font-medium text-slate-400">{label}</span>
      <input
        type="number"
        min={0}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(Number(e.target.value))}
        className="w-full rounded-lg border border-slate-700 bg-slate-900/60 px-3 py-2 text-sm text-slate-100 outline-none transition focus:border-yellow-400/60 focus:ring-1 focus:ring-yellow-400/40 disabled:opacity-50"
      />
    </label>
  )
}

export default function Controls({
  mode,
  concurrency,
  initialStock,
  running,
  canRun,
  onModeChange,
  onConcurrencyChange,
  onInitialStockChange,
  onRun,
}: ControlsProps) {
  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5 backdrop-blur">
      <h2 className="mb-4 text-sm font-semibold uppercase tracking-wider text-slate-400">
        시나리오 설정
      </h2>

      <div className="flex flex-col gap-3 sm:flex-row">
        <ModeCard
          active={mode === 'SYNC'}
          disabled={running}
          title="일반 동시성 모드 (No Lock)"
          desc="락 없이 read-modify-write — Lost Update 로 재고보다 많이 팔리는 Race Condition 을 그대로 노출합니다."
          accent="ring-red-400/70"
          onClick={() => onModeChange('SYNC')}
        />
        <ModeCard
          active={mode === 'REDIS_LOCK'}
          disabled={running}
          title="Redis 분산 락 모드"
          desc="상품별 Redisson 분산 락으로 임계영역을 직렬화 — 정확하지만 한 명씩 줄세워 느립니다."
          accent="ring-emerald-400/70"
          onClick={() => onModeChange('REDIS_LOCK')}
        />
        <ModeCard
          active={mode === 'REDIS_COUNTER'}
          disabled={running}
          title="Redis 카운터 모드 (DECR)"
          desc="원자 DECR 로 게이트키핑 — 락/대기 없이 당첨자만 DB 에 씁니다. 정확하면서도 빠릅니다."
          accent="ring-sky-400/70"
          onClick={() => onModeChange('REDIS_COUNTER')}
        />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <NumberField
          label="동시 요청 건수"
          value={concurrency}
          disabled={running}
          onChange={onConcurrencyChange}
        />
        <NumberField
          label="초기 재고"
          value={initialStock}
          disabled={running}
          onChange={onInitialStockChange}
        />
      </div>

      <button
        type="button"
        disabled={!canRun || running}
        onClick={onRun}
        className={[
          'mt-5 w-full rounded-xl px-5 py-3.5 text-base font-bold transition-all',
          !canRun || running
            ? 'cursor-not-allowed bg-slate-800 text-slate-500'
            : 'bg-gradient-to-r from-yellow-400 to-orange-500 text-slate-900 shadow-lg shadow-orange-500/20 hover:scale-[1.01] hover:shadow-orange-500/40 active:scale-100',
        ].join(' ')}
      >
        {running ? '시뮬레이션 진행 중…' : '🚀 시뮬레이션 시작'}
      </button>
    </section>
  )
}
