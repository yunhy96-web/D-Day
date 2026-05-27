import { useCallback, useEffect, useRef, useState } from 'react'
import Mermaid from '@/components/Mermaid'

interface ZoomableDiagramProps {
  chart: string
}

/**
 * Mermaid 다이어그램을 가로폭에 꽉 차게 보여주고, 클릭하면 전체화면 모달에서
 * 확대/축소(휠·버튼) + 드래그 이동(pan)으로 크게 볼 수 있는 래퍼.
 */
export default function ZoomableDiagram({ chart }: ZoomableDiagramProps) {
  const [open, setOpen] = useState(false)

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        title="클릭하면 크게 보기"
        className="group relative block w-full cursor-zoom-in rounded-xl border border-slate-800 bg-black/30 p-3 [&_svg]:!max-w-none [&_svg]:w-full [&_svg]:h-auto"
      >
        <Mermaid chart={chart} className="overflow-x-auto" />
        <span className="pointer-events-none absolute right-2 top-2 rounded-md bg-slate-800/80 px-2 py-1 text-[11px] text-slate-300 opacity-0 transition group-hover:opacity-100">
          🔍 확대
        </span>
      </button>

      {open && <DiagramModal chart={chart} onClose={() => setOpen(false)} />}
    </>
  )
}

const MIN_SCALE = 0.4
const MAX_SCALE = 6

function DiagramModal({ chart, onClose }: { chart: string; onClose: () => void }) {
  const [scale, setScale] = useState(1)
  const [offset, setOffset] = useState({ x: 0, y: 0 })
  const dragRef = useRef<{ x: number; y: number } | null>(null)

  const reset = useCallback(() => {
    setScale(1)
    setOffset({ x: 0, y: 0 })
  }, [])

  const zoomBy = useCallback((delta: number) => {
    setScale((s) => Math.min(MAX_SCALE, Math.max(MIN_SCALE, +(s + delta).toFixed(2))))
  }, [])

  // ESC 로 닫기 + 열려 있는 동안 배경 스크롤 잠금.
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [onClose])

  const onWheel = (e: React.WheelEvent) => {
    e.preventDefault()
    zoomBy(e.deltaY < 0 ? 0.2 : -0.2)
  }

  const onPointerDown = (e: React.PointerEvent) => {
    dragRef.current = { x: e.clientX - offset.x, y: e.clientY - offset.y }
    ;(e.target as Element).setPointerCapture?.(e.pointerId)
  }
  const onPointerMove = (e: React.PointerEvent) => {
    if (!dragRef.current) return
    setOffset({ x: e.clientX - dragRef.current.x, y: e.clientY - dragRef.current.y })
  }
  const onPointerUp = () => {
    dragRef.current = null
  }

  return (
    <div
      className="fixed inset-0 z-50 flex flex-col bg-black/85 backdrop-blur-sm"
      onClick={onClose}
    >
      {/* 컨트롤 바 */}
      <div
        className="flex items-center justify-end gap-2 p-3"
        onClick={(e) => e.stopPropagation()}
      >
        <span className="mr-auto px-2 font-mono text-xs text-slate-500">
          휠/버튼 확대 · 드래그 이동 · ESC 닫기
        </span>
        <ToolButton label="−" onClick={() => zoomBy(-0.2)} />
        <span className="w-14 text-center font-mono text-sm text-slate-300">
          {Math.round(scale * 100)}%
        </span>
        <ToolButton label="+" onClick={() => zoomBy(0.2)} />
        <ToolButton label="리셋" onClick={reset} />
        <ToolButton label="✕ 닫기" onClick={onClose} />
      </div>

      {/* 캔버스 */}
      <div
        className="flex-1 cursor-grab overflow-hidden active:cursor-grabbing"
        onClick={(e) => e.stopPropagation()}
        onWheel={onWheel}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerLeave={onPointerUp}
      >
        <div
          className="flex h-full w-full items-center justify-center"
          style={{
            transform: `translate(${offset.x}px, ${offset.y}px) scale(${scale})`,
            transition: dragRef.current ? 'none' : 'transform 0.1s ease-out',
          }}
        >
          <div className="[&_svg]:!max-w-none [&_svg]:h-auto">
            <Mermaid chart={chart} className="" />
          </div>
        </div>
      </div>
    </div>
  )
}

function ToolButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="rounded-lg border border-slate-700 bg-slate-800/80 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-700"
    >
      {label}
    </button>
  )
}
