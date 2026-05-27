import ZoomableDiagram from '@/components/ZoomableDiagram'
import type { DesignNote } from '@/lib/designNotes'

/** 설계 의도 노트 한 장. 문제 → 결정 → 이유 3단 + 선택적 다이어그램. */
export default function DesignNoteCard({ note }: { note: DesignNote }) {
  return (
    <article className="rounded-2xl border border-slate-800 bg-slate-900/40 p-5">
      <div className="mb-3 flex flex-wrap items-center gap-x-3 gap-y-1">
        <h3 className="text-base font-semibold text-slate-100">{note.title}</h3>
        <time className="font-mono text-xs text-slate-500">{note.date}</time>
        {note.tags?.map((tag) => (
          <span
            key={tag}
            className="rounded-full border border-slate-700 bg-slate-800/60 px-2 py-0.5 text-[11px] text-slate-400"
          >
            {tag}
          </span>
        ))}
      </div>

      <dl className="space-y-3 text-sm leading-relaxed">
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-rose-400/80">문제</dt>
          <dd className="mt-0.5 text-slate-300">{note.problem}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-sky-400/80">결정</dt>
          <dd className="mt-0.5 text-slate-300">{note.decision}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-emerald-400/80">이유 (의도)</dt>
          <dd className="mt-0.5 text-slate-300">{note.why}</dd>
        </div>
      </dl>

      {note.chart && (
        <div className="mt-4">
          <ZoomableDiagram chart={note.chart} />
        </div>
      )}
    </article>
  )
}
