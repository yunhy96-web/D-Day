# CLAUDE.md — PackFit

물류 상자 적재 시뮬레이터. 상자 규격 + 물건 규격 입력 → 3D bin packing(EP-BFD) 계산 → Three.js 로 배치 시각화.

## 현재 단계
**M4 완료(다중 정렬·fragile·maxWeight·표준 상자 추천)**. 다음: M5 Phase 3 GA(선택). 설계는 `.claude/skills/design/SKILL.md` 가 단일 소스 — 구현 전 반드시 읽을 것.

## 스택
Vite + TypeScript + Three.js. 프레임워크·백엔드·DB 없음. 테스트는 vitest, 알고리즘(`src/pack.ts`)만 대상.

## 규칙
- 단위 mm/g 정수. 좌표 원점 = 상자 내벽 좌-앞-바닥 모서리.
- 간격은 알고리즘 밖에서 처리: 상자는 `wall` 만큼 축소, 물건은 `gap` 만큼 팽창. `pack()` 은 간격 개념을 모른다.
- `pack()` 은 순수 함수·결정적. DOM/Three.js import 금지.
- 알고리즘 수정 = 테스트 먼저. 마일스톤당 커밋 1개.
