import DiagramCard from '@/components/DiagramCard'
import DesignNoteCard from '@/components/DesignNoteCard'
import { ARCHITECTURE, CONCURRENCY_LOCK, EVENT_PIPELINE, MODULAR_MONOLITH } from '@/lib/diagrams'
import { DESIGN_NOTES } from '@/lib/designNotes'

/**
 * "설계 의도" 화면.
 * 상단: 전체 아키텍처 다이어그램 / 하단: 기능별 설계 노트(개발 저널, 최신순).
 * 노트는 designNotes.ts 배열에 항목을 추가하면 자동으로 늘어난다.
 */
export default function DesignDoc() {
  return (
    <div className="space-y-8">
      <section>
        <h2 className="mb-1 text-lg font-bold text-slate-100">아키텍처 개요</h2>
        <p className="mb-4 text-sm text-slate-400">
          진짜 MSA가 아니라 <strong className="text-slate-200">모듈러 모놀리스</strong>다. 단일 Spring
          Boot 앱이지만 order·payment·auth 를 도메인+DB 스키마로 분리하고, 스키마 간 FK 없이 이벤트(Redis
          Stream)로만 연결해 — 필요해지면 독립 서비스로 쪼갤 수 있는 경계를 미리 그어뒀다.
        </p>
        <div className="space-y-5">
          <DiagramCard title="전체 아키텍처" chart={ARCHITECTURE} />
          <DiagramCard title="모듈러 모놀리스 → MSA 분리 경로" chart={MODULAR_MONOLITH}>
            MSA의 운영 복잡도는 피하되, 분리에 필요한 경계(스키마 격리·FK 없음·이벤트 통신)는 미리
            지켜 미래의 확장 경로를 닫지 않는다.
          </DiagramCard>
          <DiagramCard title="이벤트 파이프라인 — Transactional Outbox" chart={EVENT_PIPELINE}>
            주문과 이벤트를 같은 트랜잭션에 커밋해 dual-write 를 회피하고, 소비자는 UNIQUE 제약으로
            멱등 처리한다.
          </DiagramCard>
          <DiagramCard title="동시성 제어 — REDIS_LOCK" chart={CONCURRENCY_LOCK}>
            Redisson 분산락으로 임계영역을 직렬화하면 정확히 재고 수량만큼만 판매된다.
          </DiagramCard>
        </div>
      </section>

      <section>
        <h2 className="mb-1 text-lg font-bold text-slate-100">설계 노트</h2>
        <p className="mb-4 text-sm text-slate-400">
          기능을 추가할 때마다 "문제 → 결정 → 이유"를 한 장씩 기록한다. (최신순)
        </p>
        <div className="space-y-5">
          {DESIGN_NOTES.map((note) => (
            <DesignNoteCard key={note.id} note={note} />
          ))}
        </div>
      </section>
    </div>
  )
}
