# yunhy96-web — Side Projects Monorepo

백엔드 실무(결제·외부 연동·이벤트 아키텍처)에서 다룬 주제를 설계 → 개발 → 인프라 → 배포까지 1인으로 재구현한 프로젝트 모음입니다.

| 프로젝트 | 한 줄 요약 | 스택 | 상태 |
|---|---|---|---|
| [FlashArena](./FlashArena) | 선착순 세일에서 No-Lock 재고 음수 발생 vs Redis 분산 락 정확 차감을 SSE 실시간 로그로 비교 증명. Transactional Outbox + Kafka(orderId 파티션 키·acks=all·수동 오프셋 커밋), UNIQUE 멱등 Consumer, 스키마 격리로 MSA 제약 재현 | Java 21 · Spring Boot 3 · Kafka · Redis · PostgreSQL · React | 완료 |
| [ThaiUnionTour](./thaiuniontour) | 이벤트 기반 여행 예약 커머스. `UPDATE … WHERE stock >= qty` 원자 차감, `FOR UPDATE SKIP LOCKED` Relayer → Redis Streams → 멱등 Consumer 결제 파이프라인. 설계 문서 8종 선작성 | Java 17 · Gradle 멀티모듈(DDD) · PostgreSQL · Flyway · Next.js 14 | 개발 중 (M2) |
| D-Day ([Back](./DdayBack) · [Front](./DdayFront) · [Scheduler](./DdayScheduler)) | 한·태 다국어 커뮤니티. 트랜잭션 경계 밖 비동기 OpenAI 번역 파이프라인, DB 기반 동적 RBAC, 재배포 없는 동적 스케줄러, 재시작 안전한 크롤러 | Spring Boot 3.2 · PostgreSQL · OpenAI API · Expo · Docker · EC2 | 운영 중 |
| [PackFit](./PackFit) | 3D 상자 적재 시뮬레이터. EP-BFD bin packing 직접 구현(6방향 회전·지지율·fragile·무게 제약), 불변식 기반 테스트, Three.js 단계별 시각화 | TypeScript · Three.js · Vite · Vitest | 완료 |
| CheckStock ([Backend](./checkstock-backend) · [Server](./checkstock-server) · [App](./CheckStock)) | 재입고 모니터링. 스냅샷 diff 변경 감지 → Expo Push·Telegram 멀티채널 알림 | Spring Boot · Python(Selenium) · Jsoup · Expo | 실사용 |
| [Hauly](./Hauly) · [CarNote](./CarNote) | 기타 실험 프로젝트 | — | 보관 |

상세 설계 포인트·실행 방법은 FlashArena · thaiuniontour · DdayFront 폴더의 README에 있습니다.

- 연락처: yunhy96@naver.com
