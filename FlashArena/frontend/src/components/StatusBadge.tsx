export type ConnState = 'connecting' | 'online' | 'error'

interface StatusBadgeProps {
  state: ConnState
  onRetry: () => void
}

export default function StatusBadge({ state, onRetry }: StatusBadgeProps) {
  const config = {
    connecting: { dot: 'bg-yellow-400 animate-pulse', text: '연결 중…', color: 'text-yellow-300' },
    online: { dot: 'bg-emerald-400', text: '백엔드 연결됨', color: 'text-emerald-300' },
    error: { dot: 'bg-red-500', text: '연결 실패', color: 'text-red-300' },
  }[state]

  return (
    <div className="flex items-center gap-2 rounded-full border border-slate-700 bg-slate-900/60 px-3 py-1.5 text-xs">
      <span className={`h-2 w-2 rounded-full ${config.dot}`} />
      <span className={config.color}>{config.text}</span>
      {state === 'error' && (
        <button
          type="button"
          onClick={onRetry}
          className="ml-1 rounded bg-slate-700 px-2 py-0.5 text-slate-200 hover:bg-slate-600"
        >
          재시도
        </button>
      )}
    </div>
  )
}
