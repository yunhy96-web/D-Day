import {
  CONCURRENCY_SYNC,
  COUNTER_GATEKEEPING,
  EVENT_PIPELINE,
  MODULAR_MONOLITH,
} from '@/lib/diagrams'

/**
 * 설계 의도 노트(개발 저널).
 *
 * 👉 기능을 추가할 때마다 이 배열 맨 위(최신순)에 항목 하나를 추가하면
 *    "설계 의도" 화면에 자동으로 카드가 한 장 늘어난다.
 *
 * 각 항목은 "어떤 문제(problem) → 무엇을 했나(decision) → 왜(why)" 3단으로 적는다.
 */
export interface DesignNote {
  /** 안정적인 고유 키 (kebab-case). */
  id: string
  /** 추가/결정한 시점 (YYYY-MM-DD). 최신이 위로. */
  date: string
  /** 한 줄 제목. */
  title: string
  /** 분류 태그 (선택). */
  tags?: string[]
  /** 마주한 문제 / 요구사항. */
  problem: string
  /** 내린 결정 / 구현한 것. */
  decision: string
  /** 그 결정을 한 이유(의도) — 트레이드오프 포함하면 좋다. */
  why: string
  /** 선택: 함께 보여줄 Mermaid 다이어그램. */
  chart?: string
}

export const DESIGN_NOTES: DesignNote[] = [
  {
    id: 'redis-counter-mode',
    date: '2026-05-26',
    title: 'Redis 카운터(DECR) 모드 — 락의 직렬화 비용을 인메모리 게이트로 대체',
    tags: ['Redis', '동시성', '성능', '플래시세일'],
    problem:
      '분산 락은 정확하지만 모든 요청(당첨이든 낙첨이든)을 단일 락에 줄세워 직렬화한다. 경쟁이 심할수록(동시요청 ≫ 재고) 한 명씩 처리하느라 느려지고, DB 가 곧 병목이 된다. 재고 100에 1만 명이 몰리면 9900명도 락을 잡아 "품절"을 확인해야 한다.',
    decision:
      'Redis 원자 DECR 로 게이트키핑하는 REDIS_COUNTER 모드를 추가했다. 시작 전 카운터를 초기 재고로 적재하고, 각 요청은 DECR 한 번으로 당첨/낙첨이 갈린다(잔여 >= 0 이면 당첨). 당첨자만 DB 에 쓰며, 그 차감마저 원자 UPDATE 라 동시 당첨자끼리도 lost-update 가 없다.',
    why: '락의 "기다림"을 인메모리 원자 연산 속도로 대체한다 — 낙첨자는 DB·락을 전혀 거치지 않고 DECR 한 번에 즉시 거절되고, DB 는 당첨자(재고 수)만큼만 부하를 받는다. 정확성은 그대로 유지하면서 대규모 트래픽에서 락보다 훨씬 빠르다. 시뮬레이터에서 REDIS_LOCK 과 elapsedMs 를 직접 비교해 "정확성의 비용"과 "그 비용을 줄이는 법"을 한 화면에서 보여준다.',
    chart: COUNTER_GATEKEEPING,
  },
  {
    id: 'archunit-boundary-enforcement',
    date: '2026-05-23',
    title: 'ArchUnit — 모듈 경계를 빌드 타임에 강제',
    tags: ['ArchUnit', '아키텍처', '테스트'],
    problem:
      '모듈 경계가 규약(주석·관습)으로만 존재하면, 누군가 무심코 다른 도메인의 내부 클래스를 import 해도 컴파일이 통과해 경계가 서서히 무너진다. 실제로 order(시뮬레이터)가 payment 의 Repository 를 직접 호출하는 위반이 한 곳 있었다.',
    decision:
      'ArchUnit 규칙 5개를 테스트로 추가했다 — (1) payment 는 order 의존 금지, (2) order 는 payment 를 공개 application 포트로만 접근, (3) auth 는 독립, (4) domain 은 infra/presentation 의존 금지, (5) 도메인 슬라이스 순환 금지. 동시에 위반 1곳을 PaymentResetService(공개 포트)로 추출해 제거했다.',
    why: '경계를 "지키자고 합의한 규약"에서 "위반하면 빌드가 실패하는 불변식"으로 격상했다. 이제 "모듈러 모놀리스"라는 라벨이 말이 아니라 통과하는 테스트로 증명된다.',
  },
  {
    id: 'modular-monolith-msa',
    date: '2026-05-22',
    title: 'MSA가 아니라 모듈러 모놀리스 — "쪼갤 수 있게" 경계만 그어둠',
    tags: ['아키텍처', 'MSA', '모듈러 모놀리스'],
    problem:
      '진짜 MSA(독립 배포·네트워크 경계)는 운영 복잡도(분산 트랜잭션, 서비스 디스커버리, 인프라)가 크다. 1인 사이드 프로젝트에서 이를 다 짊어지면 본질(동시성·이벤트 설계)에 집중할 수 없다.',
    decision:
      '단일 Spring Boot 앱(모듈러 모놀리스)으로 두되, MSA로 분리할 때 필요한 경계는 미리 지킨다 — (1) 도메인을 패키지+DB 스키마로 분리, (2) 스키마 간 물리 FK 금지(ID로만 참조), (3) 도메인 간 통신은 직접 호출이 아니라 이벤트(Redis Stream)로만. payment 는 order 스키마를 절대 조회하지 않는다.',
    why: '이 세 가지만 지키면 나중에 각 모듈을 독립 서비스로 "추출"하는 게 안전해진다. 즉 지금은 단순함을 얻고, 미래의 확장 경로는 닫지 않는다. 이 경계는 규약이 아니라 ArchUnit 테스트로 강제된다 — 다른 도메인의 내부를 import 하면 빌드가 실패한다.',
    chart: MODULAR_MONOLITH,
  },
  {
    id: 'idempotent-consumer',
    date: '2026-05-22',
    title: '멱등 소비자 — at-least-once 환경에서 중복 결제 방지',
    tags: ['Redis Stream', '멱등성', 'payment'],
    problem:
      'Redis Stream 은 메시지를 한 번 이상 전달할 수 있다(at-least-once). 같은 주문 이벤트가 두 번 도착하면 결제가 중복 적재될 수 있다.',
    decision:
      'payment_history 의 order_id 에 UNIQUE 제약을 걸고, 소비자는 (1) existsByOrderId 빠른 경로 → (2) INSERT 시도 → UNIQUE 충돌이면 "이미 처리됨"으로 간주해 스킵한다. 성공 시에만 XACK.',
    why: 'UNIQUE 제약을 "최종 방어선"으로 두면 애플리케이션 레벨 체크에 경합이 있어도 DB 가 중복을 막아준다. 비-중복 오류는 XACK 하지 않아 재처리되게 한다.',
    chart: EVENT_PIPELINE,
  },
  {
    id: 'transactional-outbox',
    date: '2026-05-21',
    title: 'Transactional Outbox — dual-write 문제 제거',
    tags: ['이벤트', '트랜잭션', 'order'],
    problem:
      'DB 에 주문을 쓰면서 동시에 메시지 브로커로 이벤트를 발행하면, 한쪽만 성공해 "주문은 있는데 이벤트는 사라진" 상태가 생길 수 있다(dual-write).',
    decision:
      '재고 차감 + 주문 INSERT + outbox INSERT 를 같은 로컬 트랜잭션에서 원자적으로 커밋한다. Redis 는 이 트랜잭션 안에서 절대 건드리지 않고, 별도 릴레이어가 outbox 를 폴링해 발행한다.',
    why: '비즈니스 데이터와 "발행할 이벤트"를 한 커밋으로 묶으면 이벤트 유실이 원천 차단된다. 발행 실패는 릴레이어가 재시도하므로 at-least-once 가 보장된다.',
  },
  {
    id: 'sse-streaming',
    date: '2026-05-20',
    title: 'SSE 실시간 스트리밍 — 버퍼 + 리플레이',
    tags: ['SSE', '비동기'],
    problem:
      '동시요청 수천 건 시뮬레이션은 수 초가 걸린다. 요청 스레드를 잡아둘 수 없고, 구독이 실행보다 늦게 붙으면 초반 로그를 놓친다.',
    decision:
      'POST /run 은 즉시 202 + runId 를 돌려주고 백그라운드로 실행한다. 클라이언트는 runId 로 SSE 를 구독하고, 서버는 인메모리 버퍼에 쌓인 로그를 먼저 리플레이한 뒤 라이브로 전환한다. 종료 시 result 이벤트로 닫는다.',
    why: 'WebSocket 대신 단방향 SSE 로 충분하고 더 가볍다. 버퍼+리플레이로 "구독 전에 시작된 run"의 로그 유실을 막고, 연결이 끊겨도 결과는 폴링으로 받을 수 있게 폴백을 뒀다.',
  },
  {
    id: 'concurrency-modes',
    date: '2026-05-20',
    title: '동시성 비교 모드 — 버그를 숫자로 증명',
    tags: ['동시성', 'Redisson', '시연'],
    problem:
      '"분산 락이 왜 필요한가"를 말로 설명하는 대신 데이터로 보여주고 싶었다.',
    decision:
      '동일한 "재고 1개 구매" 로직을 SYNC(락 없음)와 REDIS_LOCK(Redisson 분산락) 두 모드로 실행한다. CountDownLatch 시작 게이트로 워커들을 동시에 출발시켜 경합을 극대화한다.',
    why: 'SYNC 는 lost update 로 재고가 음수가 되는 oversell 을 그대로 노출하고, REDIS_LOCK 은 직렬화로 정확히 재고만큼만 판매한다. 같은 로직의 두 결과를 나란히 보여줘 락의 효과를 증명한다.',
    chart: CONCURRENCY_SYNC,
  },
  {
    id: 'osiv-disabled',
    date: '2026-05-20',
    title: 'OSIV 비활성화 — stale read 차단',
    tags: ['JPA', 'Hibernate'],
    problem:
      'Open-Session-In-View 가 켜져 있으면 요청 스레드의 영속성 컨텍스트가 끝까지 열려, 워커 트랜잭션이 커밋한 재고 변경을 1차 캐시가 가려 최종 재고를 stale 하게 읽는다.',
    decision: 'spring.jpa.open-in-view=false 로 설정했다.',
    why: '시뮬레이션은 여러 워커 트랜잭션이 커밋한 최신 재고를 정확히 읽어야 한다. OSIV 를 끄면 요청 스레드가 매번 DB 의 최신 상태를 조회한다.',
  },
]
