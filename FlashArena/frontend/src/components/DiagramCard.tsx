import type { ReactNode } from 'react'
import ZoomableDiagram from '@/components/ZoomableDiagram'

interface DiagramCardProps {
  title: string
  /** Mermaid 다이어그램 정의 (diagrams.ts 상수). */
  chart: string
  /** 다이어그램 아래 설명. 문자열 또는 JSX. */
  children?: ReactNode
}

/**
 * 포트폴리오 사이드바용 카드: 제목 + Mermaid 다이어그램 + 설명.
 * 다크(slate) 테마에 맞춘 패널 스타일.
 */
export default function DiagramCard({ title, chart, children }: DiagramCardProps) {
  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">
      <h3 className="mb-4 text-sm font-semibold tracking-wide text-slate-200">{title}</h3>
      <ZoomableDiagram chart={chart} />
      {children && (
        <div className="mt-4 space-y-2 text-sm leading-relaxed text-slate-400">{children}</div>
      )}
    </section>
  )
}
