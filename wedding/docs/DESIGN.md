# 모바일 청첩장 — 설계

> 작성일 2026-10-10. 구현 전 설계 문서. 결정은 **근거와 함께** 기록하고, 바뀌면 여기부터 고친다.

## 0. 한 줄 요약

Spring Boot 단일 앱(Thymeleaf + Tailwind + vanilla JS) 하나를 친구 라즈베리파이에 도커로 띄우고,
이미 돌아가는 Cloudflare Tunnel 에 `wedding.thaiuniontour.com` 한 줄만 추가해서 노출한다.
사진은 Pi 디스크에 두고 Cloudflare 캐시로 서빙한다. **월 비용 0원.**

## 1. 목표와 제약

| 항목 | 결정 |
|---|---|
| 목적 | 개발자답게 "직접 만든" 청첩장. 일반 템플릿 청첩장과 다른 경험 제공 |
| 핵심 차별 콘텐츠 | ① 사진 이상형 월드컵(32강 토너먼트) ② 실시간 "지금 N명이 보고 있어요" ③ 방명록 |
| 필수 기능 | 인사말 · 일시/장소 · 오시는 길(카카오맵) · 갤러리(40장) · 방명록 · 계좌번호 · 카카오톡 공유(OG) |
| 언어 | **한국어 · 영어 · 태국어** 3개. 모든 화면 텍스트는 DB `label` 테이블에서 읽고, 상단 토글로 즉시 전환 (§6-2) |
| 도메인 | 새로 사지 않는다. `thaiuniontour.com` 서브도메인 사용. 나중에 도메인 사면 3군데(§10)만 바꾸면 됨 |
| 서버 | 친구 Raspberry Pi 5 (아래 §2). 별도 클라우드 없음 |
| 사진 저장 | S3 안 씀. Pi 로컬 디스크 (§7) |
| 트래픽 가정 | 하객 ~300명, 피크 동시접속 ~50명. 과설계 금지 |
| 기간 | 결혼식 전후 2~3개월만 살아있으면 됨. 운영성보다 "만들어서 띄우는 속도" 우선 |

## 2. 인프라 현황 (2026-10-10 확인)

SSH `seonghanim@125.240.159.13` 접속 정상.

| 항목 | 값 |
|---|---|
| 기기 | Raspberry Pi 5 Model B, **aarch64**, RAM 7.9G (가용 5.5G), NVMe 235G (가용 206G) |
| uptime | 105일. 부하 거의 없음 (load 0.01) |
| 도커 | docker 20.10.24, **docker-compose v1** (`docker compose` 서브커맨드 없음 → `docker-compose` 사용) |
| 기존 스택 | `tut-*`(thaiuniontour: postgres·redis·api·admin·frontend·cloudflared), `bloomi-*`(친구 프로젝트, 80/443 점유), `chatdb` |
| 터널 | `tut-cloudflared` 컨테이너. 로컬 관리 터널 `08acb9b7-…`. ingress: `thaiuniontour.com`, `www.` → `frontend:3000` |
| 네트워크 | tut 스택은 `docker_default` 네트워크 (compose 프로젝트명이 `docker`) |
| 배포 경로 | `~/workspace/huiyeong/thaiuniontour/` (cloudflared 인증서·config.yml 포함) |
| 캐시된 이미지 | `eclipse-temurin:17-jre`(318MB) 이미 있음 → 베이스 이미지 재활용 |

**함의**
- 80/443 은 bloomi-nginx 가 쓰고 있으니 포트 노출은 하지 않는다. 어차피 터널로 나가므로 필요 없음.
- 앱 컨테이너를 `docker_default` 에 붙이고 cloudflared ingress 에 `wedding.thaiuniontour.com → http://wedding-app:8080` 한 줄 추가. DNS CNAME 은 `cloudflared tunnel route dns` 한 번. **도메인 추가비용 0.**
- arm64 → Pi 에서 직접 `docker-compose build` (tut 과 동일 방식). 로컬 Mac 에서 buildx 할 필요 없음.
- Java 앱 1개(~300MB RSS) 추가해도 여유 5GB. 문제없음.

## 3. 기술 스택

| 계층 | 선택 | 왜 |
|---|---|---|
| 백엔드 | Spring Boot 3.x / Java 17 / Gradle **단일 모듈** | 내가 가장 빠른 스택. 기능이 3개(사진·투표·접속자)라 멀티모듈은 과함 |
| 뷰 | Thymeleaf + Tailwind(CDN) + vanilla JS | SPA 빌드 파이프라인 없이 jar 하나로 끝. 청첩장은 페이지 1장 |
| DB | **SQLite** (`sqlite-jdbc`) + `JdbcTemplate` + `schema.sql` | 테이블 6개(visit·pick·guestbook·label·content·event), 쓰기 적음. 파일 하나라 백업 = `cp`. Postgres 컨테이너·JPA 전부 생략 |
| 사진 | 로컬 디렉토리 `/data/photos/*.webp` 를 Spring 정적 리소스로 서빙 | §7 |
| 지도 | Kakao Maps JavaScript SDK (무료) | §8 |
| 실시간 접속자 | 15초 heartbeat 폴링 | §6. WebSocket/SSE/Redis 전부 불필요 |
| 보안 | Cloudflare(WAF·Rate Limit·Turnstile) + 앱 필터 1개 + 인터셉터 1개 | §13. Spring Security 미도입 |
| 다국어 | DB `label` 테이블 → 페이지에 JSON 임베드 → JS `data-i18n` 치환 | §6-2. Spring MessageSource·i18n 라이브러리 불필요 |
| 배포 | Dockerfile(jar) + docker-compose.yml 1개 + 기존 cloudflared | §9 |

> ponytail: SQLite 는 동시 쓰기가 직렬화됨. 하객 300명 규모엔 충분. 수천 명이면 Pi 의 tut-postgres 에 DB 하나 추가하면 됨(드라이버·URL 만 교체).

## 4. 정보구조(IA)와 화면 흐름

디자인 컨셉은 §4-4 "두 가닥의 실, 하나의 매듭"(한국 × 태국). 여기서는 구조와 동선만 고정한다.

### 4-1. 마케터 관점: 왜 분리하고, 첫 화면은 무엇인가

하객이 이 링크를 여는 상황은 **카톡에서 탭 → 3초 안에 "누구, 언제, 어디"** 를 확인하는 것. 청첩장은 랜딩이고 월드컵·방명록은 "머무는 이유"다. 둘을 한 페이지에 다 넣으면 스크롤이 길어져 핵심 정보(일시·장소)가 묻히고, 컨텐츠는 "지나치다 본 것"이 된다. 그래서:

| 원칙 | 적용 |
|---|---|
| **1 화면 = 1 목적** | `/` 는 "초대"만. 컨텐츠는 각자 URL(`/c/{key}`)로 분리. 각 컨텐츠 페이지는 자기 CTA 하나만 가짐 |
| **첫 3초** | 풀스크린 메인 사진 + 두 이름 + 날짜 + D-day. 다른 UI 없음. 언어 토글만 우상단 |
| **핵심 정보는 스크롤 1~2회 안에** | 인트로 → 인사말 → 일시/장소 요약(지도 썸네일 + "오시는 길" 버튼). 여기까지가 "청첩장" |
| **그 다음이 놀이터** | "함께 즐겨요" 컨텐츠 허브: 카드 2~4개. 카드엔 참여 수 같은 **사회적 증거**("312명이 골랐어요") 표시 |
| **참여 → 공유 루프** | 월드컵 결과 화면에 "내 1위 사진 카톡으로 자랑하기". 공유 링크는 `/c/worldcup?from=share` 로 유입 추적 |
| **다음 행동 유도** | 모든 컨텐츠 종료 화면에 "다른 컨텐츠 보기" + "청첩장으로" 두 버튼. 막다른 길 없음 |
| **급한 사람 우선** | 상단 고정 미니바(스크롤 후 노출): 날짜 · 길찾기 · 계좌. 컨텐츠 안 봐도 용건 끝남 |
| **측정** | 유입(공유/직접) · 컨텐츠 진입 · 완료를 `event` 로 기록(§9). 결혼식 후 "몇 명이 뭘 했나" 볼 수 있음 |

**첫 화면 와이어(세로, 모바일)**
```
┌──────────────────────────┐
│ [KO EN TH]               │ ← 언어 토글만
│                          │
│     (메인 사진 풀스크린)   │
│                          │
│    희영  &  ○○           │ ← 세리프, 크게
│    2027. 0. 00  SAT 12:00 │
│         D-120             │ ← 카운트 애니메이션
│          ⌄ scroll         │
├──────────────────────────┤
│ 인사말 + 양가 성함         │
├──────────────────────────┤
│ 📅 날짜/시간  📍 식장명    │
│ [지도 썸네일]  [오시는 길 >]│ ← 상세는 /c/map 또는 본문 하단
├──────────────────────────┤
│ 갤러리 미리보기 6장 [더보기]│
├──────────────────────────┤
│ 함께 즐겨요                │
│ ┌ 🏆 사진 월드컵 ───────┐ │
│ │ "우리 최고의 순간은?"   │ │
│ │ 312명 참여 · 약 2분    │ │ ← 사회적 증거 + 소요시간
│ └───────────── [시작] ─┘ │
│ ┌ ✍ 방명록 ────────────┐ │
│ │ 최신 글 1줄 미리보기    │ │
│ └───────────── [쓰기] ─┘ │
├──────────────────────────┤
│ 마음 전하실 곳 (접기)      │
├──────────────────────────┤
│ 지금 7명 함께 보는 중 · 공유 │
└──────────────────────────┘
```

### 4-2. 화면(라우트) 목록

| 라우트 | 역할 | 비고 |
|---|---|---|
| `/` | 청첩장 본문 + 컨텐츠 허브 | 위 와이어. 서버 렌더링 1장 |
| `/c/worldcup` | 사진 월드컵 (§5) | 진행·결과 모두 이 URL. 결과는 `#result` |
| `/c/guestbook` | 방명록 전체 (§6-1) | `/` 에는 최신 1~2줄 미리보기만 |
| `/c/gallery` | 사진 40장 전체 + 전체화면 스와이프 | `/` 에는 6장 미리보기만 |
| `/c/map` | 오시는 길 상세: 카카오맵 + 딥링크 + 대중교통/주차 (§8) | `/` 에는 요약 + 버튼 |
| `/api/**` | JSON (§10) | |

공통 레이아웃: 상단 뒤로가기(→ `/`) + 언어 토글, 하단 "다른 컨텐츠" 바. Thymeleaf 레이아웃 fragment 1개.

### 4-3. 컨텐츠 모듈 확장 구조

컨텐츠가 더 생겨도 **`/` 를 안 고치도록** 허브 카드는 DB `content` 테이블에서 읽는다.

```sql
CREATE TABLE IF NOT EXISTS content (
  key        TEXT PRIMARY KEY,         -- 'worldcup' | 'guestbook' | 'gallery' | 'map' | …
  path       TEXT NOT NULL,            -- '/c/worldcup'
  sort       INTEGER NOT NULL,
  enabled_yn TEXT NOT NULL DEFAULT 'Y',
  icon       TEXT,                     -- 이모지 또는 아이콘 키
  stat_sql   TEXT                      -- 카드에 보여줄 숫자. NULL 이면 미표시
);
-- 카드 문구는 label 테이블: content.{key}.title / .desc / .cta  (3개 언어)
```

- 허브는 `enabled_yn='Y' ORDER BY sort` 로 카드 렌더. 카드 숫자(참여 수 등)는 `stat_sql` 결과 1개 값. 예: worldcup → `SELECT COUNT(DISTINCT session_id) FROM pick WHERE round=5`.
- **새 컨텐츠 추가 절차** (코드 3곳 + SQL 2줄):
  1. 패키지 `content/<key>/` 에 Controller + (필요 시) 테이블 `schema.sql` 추가
  2. 템플릿 `templates/c/<key>.html` (공통 레이아웃 fragment 사용)
  3. `labels.sql` 에 `content.<key>.*` 3개 언어
  4. `content` 테이블 INSERT 1행 → 허브에 자동 노출. 끄고 싶으면 `enabled_yn='N'`
- 컨텐츠 간 결합 금지: 서로의 테이블·서비스를 참조하지 않는다. 공유하는 건 `sid`·`label`·`event` 뿐.
- 이벤트 기록은 공통 1줄: `events.log(sid, "worldcup.finish", meta)`.

> ponytail: `stat_sql` 을 DB 에 두는 건 SQL-in-DB 라 보통 안 하지만, 여기선 작성자가 나 하나고 컨텐츠 추가 시 Java 재배포 없이 카드 숫자를 바꾸기 위한 의도적 선택. 외부 입력이 절대 들어가지 않는 컬럼이며 읽기 전용 쿼리만 허용(`SELECT` 로 시작하는지 검사, 커넥션 `readOnly`). 불편해지면 `ContentStat` 인터페이스로 승격.

**후보 컨텐츠(백로그, 구조에 그대로 꽂힘)**
| key | 내용 | 테이블 | 마케팅 포인트 |
|---|---|---|---|
| `quiz` | **구현됨 → §15-2** | `quiz`, `quiz_attempt` | 공유 유도 2번째 루프 |
| `rsvp` | 참석·식사 인원 | `rsvp` | 운영 실익. 허브 최상단 고정 후보 |
| `song` | 축가/입장곡 투표 | `song_vote` | 참여 낮은 비용 |
| `timeline` | **구현됨 → §15-3** | `timeline` | 체류시간 |

## 4-4. UI 디자인: 한국 × 태국

두 문화의 결혼 상징 중 **"실(thread)로 두 사람을 잇는다"** 가 정확히 겹친다. 한국의 **청실홍실**(靑紅絲, 함에 넣는 파랑·빨강 실타래), 태국의 **몽콜**(มงคล, 예식에서 신랑신부 머리에 씌우는 흰 실로 이어진 두 고리). 이 하나의 모티프로 전체를 꿴다.

> **2026-10-11 개정 (최종)**: 컨셉을 **"구름 같은 하늘색"** 으로 확정. 네이비·각진 모서리·금선 전부 폐기.
> - 색: 바탕 `mist` #F4F9FE 위에 블러된 하늘빛 라디얼 그라데이션 3개(구름), 면은 흰색 `jasmine`, 포인트 `sky` #4FA3E0 하나, 연한 면 `cloud` #DCEEFB. 금·홍 사용 안 함.
> - 형태: 카드 `rounded-2xl/3xl`, 버튼 `rounded-full`. **테두리 선 없음** — 흰 면 + 하늘빛 그림자(`shadow-cloud`, `shadow-soft`)로 구분.
> - 버튼: 주 = `bg-sky text-white shadow-cloud`, 보조 = `bg-cloud text-sky`. 입력창은 `bg-cloud/60 rounded-xl`, 밑줄 없음.
> - 허브 카드는 사진 배경 + 제목(아이콘·통계 없음). 구분선은 양끝이 번지는 가는 선. 다크는 깊은 밤하늘(#0F1B2A) + 같은 구름.
> - 아래 "한국 × 태국" 원안은 기록용. 실제 남은 요소는 문구(두 가닥의 실)·이름 세리프·조각보형 갤러리 그리드뿐.

### 컨셉 한 줄
**"두 가닥의 실, 하나의 매듭"** — 한지 위에 그려진 한국의 청홍 실이 태국의 금빛 선문양을 따라 흘러 두 이름을 잇는다.

### 팔레트 (Tailwind `theme.extend.colors`)
| 토큰 | HEX | 유래 | 쓰임 |
|---|---|---|---|
| `mist` | `#EEF5FB` | 하늘빛 한지 | 전체 배경. 미세한 종이 질감(SVG noise 1장, 2KB). **사용자 요청: 스카이블루 메인 테마** (2026-10-10) |
| `ink` | `#1B2430` | 먹(푸른 기) | 본문 텍스트 |
| `sky` | `#4A9BDC` | 스카이블루 · 청실 | 신랑 측, 1차 포인트, 링크, 버튼 |
| `yeonji` | `#C25B5B` | 연지 · 홍실 | 신부 측, 2차 포인트, 강조 |
| `gold` | `#C9A24D` | 태국 금(ทอง), 사원 장식 | 구분선, 선문양, 버튼 테두리, 숫자(D-day) |
| `marigold` | `#E8A33D` | 태국 금잔화(ดาวเรือง) 화환 | 호버/활성 상태, 월드컵 선택 피드백 |
| `jasmine` | `#F8FBFE` | 재스민(มะลิ) 흰 꽃, 몽콜 흰 실 | 카드 배경 |

하늘·홍은 한국 청실홍실, 금·주황은 태국. 전부 **연한 하늘빛 바탕 위에서만** 쓰고 포인트 면적은 10% 이하 → 사진이 주인공이라는 원칙 유지. 금은 밝은 바탕에서 살기 때문에 어두운 테마는 쓰지 않는다.

### 타이포
- 제목: `Noto Serif KR` (한·영) / `Noto Serif Thai` (태국어) — 같은 Noto Serif 계열이라 전환해도 무게가 같음. 두 이름은 `font-weight 300`, 자간 넓게.
- 본문: `Pretendard` / `Noto Sans Thai`. `:lang(th)` 셀렉터로 교체, 태국어는 행간 1.8(성조 기호 겹침 방지).
- 숫자(D-day·카운트다운): `Noto Serif KR` 금색, `font-variant-numeric: tabular-nums`.

### 모티프 (전부 inline SVG, 이미지 파일 없음)
| 모티프 | 어디에 | 구현 |
|---|---|---|
| ~~실 라인~~ | 인트로의 실·매듭 애니메이션은 **사용자 요청으로 제거**(2026-10-11). 모티프는 문구("두 가닥의 실, 하나의 매듭")와 푸터의 청홍 점 2개로만 남김 | |
| **까녹(กนก) 선문양** | 섹션 구분선 | 태국 불꽃형 당초문을 선 1개로 단순화한 금색 path, 폭 120px, 중앙 정렬. 한국 당초문과 형태가 닮아 이질감 없음 |
| **조각보 그리드** | 갤러리 미리보기 6장, 월드컵 결과 TOP3 | 크기 다른 사각형을 이어붙인 한국 조각보 레이아웃(CSS grid `grid-template-areas`). 패치 사이 1px 금색 선 |
| **연꽃 + 모란 라인아트** | 허브 카드 아이콘, 빈 상태(방명록 0건) 일러스트 | 태국 연꽃(บัว)·한국 모란(부귀) 를 같은 선 굵기 1.5px 로. 이모지 대신 사용 |
| **재스민 화환(พวงมาลัย)** | 월드컵 우승 사진 테두리 | 흰 꽃 점 + 금 실 원형 SVG 프레임. 한국식 "1위" 왕관 대신 |
| **보자기 매듭** | 계좌 `<details>` 열림/닫힘 토글 아이콘 | 매듭 풀림 애니메이션 (요약 아이콘 회전 45°) |

### 섹션별 적용
| 섹션 | 한국 | 태국 | 합 |
|---|---|---|---|
| 인트로 | 세리프 이름 | 금색 날짜·D-day | 메인 사진 위 반투명 레이어 + 이름/날짜. 실 애니메이션 없음 |
| 인사말 | 세로쓰기 짧은 구절 1줄(`writing-mode: vertical-rl`, 한국어·영어만. 태국어는 세로쓰기 불가 → 가로 유지) | 양가 성함 위에 작은 금색 연꽃 점 | |
| 일시 | 달력(한국 청첩장 관례) 당일 칸을 연지 원 | 요일·월 금색 세리프 | 태국어 시 불기 연도 병기 `พ.ศ. 2570 (2027)` |
| 오시는 길 | | 태국어 사용자에게 "서울 교통 팁" 안내문 (지하철·택시 앱) 라벨 | 한국 하객만 보는 정보와 태국 하객만 보는 정보를 **라벨로 분기** |
| 월드컵 | 선택 카드 테두리 청/홍 (왼쪽 청, 오른쪽 홍) | 선택 시 금잔화색 플래시 + 승자에 작은 화환 | 결승은 금색 매듭이 두 사진을 잇는 연출 |
| 방명록 | 글 카드를 한지 메모지(약간 기울기 ±1°) | 작성 버튼 금 테두리 | 언어 섞인 글이 자연스럽게 공존 |
| 허브 카드 | 조각보 배치 | 연꽃·모란 아이콘 | |
| 푸터 | "지금 N명" 앞에 작은 청홍 실 점 2개 | 금색 까녹 선 | |

### 모션 원칙
- `prefers-reduced-motion` 존중. 실 애니메이션은 1회만(sessionStorage 플래그), 재방문 시 즉시 완료 상태.
- 스크롤 진입 fade/slide 는 IntersectionObserver + CSS class 토글. 라이브러리 없음.
- 전체 애니메이션 총량 상한: 인트로 2.4s 외엔 300ms 이하 전환만.

### 하지 않는 것
- 태국 사원·코끼리·한복 일러스트 같은 **관광 기념품식 클리셰**. 두 문화 모두 "장식"이 아니라 "상징" 수준으로만.
- 금색 그라데이션·반짝임 효과. 금은 **선(line)** 으로만 쓴다.
- ~~다크 모드~~ → 사용자 요청으로 추가(2026-10-10). 색은 전부 CSS 변수(RGB triplet)라 `html[data-theme=dark]` 변수 세트 하나로 전환. 초기값 localStorage → OS 설정. 갤러리 뷰어는 항상 어둡게(`night` 고정색).

## 5. 사진 월드컵 (토너먼트)

**규칙**
- 사진 40장 중 사용자마다 **랜덤 32장** 뽑아 32강 → 16강 → 8강 → 4강 → 결승 = **총 31번 선택**. 40은 2의 거듭제곱이 아니라 부전승 처리가 지저분함. 랜덤 32장이면 사람마다 다른 대진이라 오히려 재미있음.
- 대진·진행은 **전부 클라이언트** 에서. 서버는 선택 결과만 기록.
- 매 선택마다 `POST /api/pick` (fire-and-forget). 중도 이탈해도 그때까지의 승패는 집계에 포함.
- 결승 선택 시 `round=5` 로 기록 → 이게 "우승" 집계 기준.

**결과 화면**
- 내 우승 사진 크게 + "전체 N명 참여" + 전체 랭킹 TOP 3 (우승 횟수순, 동률이면 승리 횟수) + 다시 하기.

**UX 디테일**
- 다음 매치 사진 2장 `new Image().src` 로 프리로드 → 선택 즉시 전환.
- 진행바 "16강 3/8".
- 세션 ID 는 `sessionStorage` 의 `crypto.randomUUID()`. 로그인 없음. 중복 참여 막지 않음(막을 이유 없음).

**집계 SQL**
```sql
-- 우승 랭킹
SELECT winner_id, COUNT(*) AS wins FROM pick WHERE round = 5 GROUP BY winner_id ORDER BY wins DESC LIMIT 3;
-- 참여자 수
SELECT COUNT(DISTINCT session_id) FROM pick WHERE round = 5;
```

## 6. 실시간 접속자

- 클라이언트: 페이지 로드 직후와 15초마다 `POST /api/heartbeat {sid}`. 응답 `{online, total}` 을 푸터에 렌더.
- 서버: `ConcurrentHashMap<String, Instant>` 에 `sid → lastSeen`. `online` = 최근 40초 내 heartbeat 한 sid 수. heartbeat 때마다 40초 지난 항목 정리(별도 스케줄러 불필요).
- `total`: 처음 보는 sid 면 `visit` 테이블에 1행 insert. `total = COUNT(*)`.
- 탭이 백그라운드 가면 heartbeat 도 멈추므로 자연스럽게 빠짐. `visibilitychange` 로 복귀 시 즉시 1회 전송.

> SSE 로 "즉시" 갱신도 가능하지만 iOS Safari 가 백그라운드에서 연결을 끊고 Cloudflare 프록시 뒤 장기 연결 관리가 번거롭다. 15초 지연은 체감 안 됨. 폴링으로 확정.

## 6-1. 방명록

- 입력: 이름(1~20자) · 메시지(1~300자) · 비밀번호(숫자 4자리, 본인 삭제용). 로그인 없음.
- 저장: `guestbook` 테이블(§9). 비밀번호는 `BCrypt` 해시(spring-security-crypto 만 의존, 전체 Security 는 안 붙임).
- 목록: `GET /api/guestbook?page=0` 최신순 10개씩. 페이지 첫 로드 시 서버 렌더링(Thymeleaf)으로 1페이지 포함, "더보기"만 fetch.
- 삭제: `DELETE /api/guestbook/{id}` + 비밀번호 → 해시 검증. 삭제는 `deleted_yn='Y'` 소프트 삭제(실수 복구용, `sqlite3` 로 되돌림).
- 신랑신부 삭제(욕설 등): 마스터 비밀번호 `GUESTBOOK_MASTER_PW` 환경변수. 어떤 글이든 이 비밀번호로 삭제 가능. 어드민 화면 불필요.
- 스팸 방지: 같은 `sid` 로 30초 내 재작성 거부(서버 메모리 Map, heartbeat 와 같은 패턴). 그 이상은 Cloudflare 에 맡김.
- XSS: Thymeleaf `th:text` 와 JS `textContent` 만 사용. `innerHTML` 금지.

## 6-2. 다국어 (ko · en · th)

**원칙**: 화면에 보이는 모든 문구(버튼·제목·인사말·성함·식장명·안내문)는 코드나 템플릿에 하드코딩하지 않고 **`label` 테이블**에서 온다. 코드에는 키만 있다. 예외: 상세주소·계좌번호는 크롤링 방지를 위해 label 이 아니라 yml + 복사 API(§13-2).

**데이터**
```sql
CREATE TABLE IF NOT EXISTS label (
  key  TEXT NOT NULL,          -- 예 'btn.copy_address', 'intro.greeting', 'venue.name'
  lang TEXT NOT NULL,          -- 'ko' | 'en' | 'th'
  text TEXT NOT NULL,
  PRIMARY KEY (key, lang)
);
```
- 초기값은 `labels.sql` 에 `INSERT OR IGNORE` 로 3개 언어 전부 작성. 기동 시 실행되므로 신규 키 추가 = SQL 한 줄 추가 후 재배포. 문구 수정 = Pi 에서 `sqlite3 data/wedding.db` 로 UPDATE (재배포 불필요).
- 키 네이밍: `<섹션>.<의미>` (`intro.title`, `btn.navigate_kakao`, `guestbook.placeholder_name`, `worldcup.round_16`). 플레이스홀더는 `{0}` 식 (`worldcup.progress = "{0}강 {1}/{2}"`).
- 번역 안 된 키는 서버가 기동 시 로그로 경고하고 화면은 `ko` 로 fallback.

**로딩·전환 흐름**
1. `GET /` 서버가 `label` 전체를 읽어 `{ko:{…}, en:{…}, th:{…}}` JSON 을 `<script id="labels" type="application/json">` 로 임베드. 키 ~150개 × 3 ≈ 15KB. 별도 API 호출 없음.
2. 초기 언어: `localStorage.lang` → 없으면 `navigator.language` (`th*`→th, `en*`→en, 그 외 ko).
3. 템플릿의 텍스트 요소는 `<span data-i18n="btn.copy_address">`. 전환 시 JS 가 `querySelectorAll('[data-i18n]')` 돌며 `textContent` 교체, `placeholder`/`aria-label` 은 `data-i18n-attr` 로 지정. `<html lang>` 도 갱신.
4. 서버 렌더링 시에도 초기 언어 기준으로 텍스트를 채워 넣어(쿠키 `lang`) 깜빡임 방지. 쿠키 없으면 `Accept-Language`.

**언어별 특수 처리**
- 날짜·요일·카운트다운: `Intl.DateTimeFormat(lang)`. `th-TH` 는 자동으로 불기(พ.ศ. 2569) 표시됨 — 원하면 `calendar:'gregory'` 옵션으로 서기 고정. 구현 시 결정.
- 폰트: 한/영은 Pretendard, 태국어는 `Noto Sans Thai`(Google Fonts). `<html lang>` 값으로 CSS `:lang(th)` 셀렉터에서 폰트 스택 교체.
- 지도: 카카오맵 타일은 한국어 고정(SDK 제약). 식장명·주소 오버레이 텍스트만 라벨로 번역. 해외 하객에겐 안내문(`map.tip_body`)으로 카카오맵 앱 설치를 권유.
- 방명록 글은 사용자 입력이므로 번역하지 않음. 라벨은 UI 문구만.
- OG 태그(`og:title/description`)는 서버 렌더링 시 초기 언어 기준. 카톡 공유 미리보기는 공유하는 사람의 언어로 나감.

**yml 과의 역할 분리**: `application.yml` 의 `wedding.*` 에는 번역 불필요한 값만 남긴다 — 예식 일시(ISO), 위경도, 계좌번호, base-url. 텍스트는 전부 `label`.

## 7. 사진 저장 — S3 대안

> 2026-10-12: 파일 배치·업로드 방식은 §15-6 으로 대체. 저장 위치(Pi 로컬 + Cloudflare 캐시) 결정은 유지.

40장 × WebP 1600px 기준 ≈ 300KB = **총 ~12MB**. 썸네일(400px) 별도 생성 시 +2MB.

| 방식 | 비용 | 장단점 |
|---|---|---|
| **Pi 로컬 디스크 + Cloudflare 캐시** (채택) | 0원 | 206GB 남음. 터널 통해 나가는 이미지(`.webp/.jpg`)는 Cloudflare 가 기본 엣지 캐시. Pi 는 첫 요청만 받음. 백업 = 원본은 내 Mac/Google Photos 에 있음 |
| Cloudflare R2 | 10GB 저장·이그레스 무료 | Pi 가 죽어도 사진은 살아있음. 하지만 Pi 죽으면 앱도 죽으므로 의미 없음. 도메인 바꿀 때 CORS 재설정 |
| Cloudinary | 무료 25 credits/월 | URL 파라미터로 리사이즈 편함. 계정 하나 더 관리 |
| AWS S3 + CloudFront | 12MB 면 월 100원 미만 | 비용보다 IAM·버킷 정책·CDN 셋업이 더 큰 비용. 이 규모엔 과함 |

**채택안 절차**
1. 원본 40장 → 로컬에서 `cwebp -q 82 -resize 1600 0` 로 `01.webp … 40.webp`, 썸네일 `t/01.webp`(400px). 스크립트 `scripts/convert.sh` 한 개.
2. `scp -r photos/ seonghanim:~/workspace/huiyeong/wedding/data/photos/`
3. Spring: `spring.web.resources.static-locations=file:/data/photos/,classpath:/static/` + `Cache-Control: max-age=31536000`. 파일명 자체가 사진 ID.
4. **갤러리·월드컵 사진은 업로드 화면 없음.** 40장 한 번 올리고 끝. 사진 교체 = scp. (타임라인 사진만 어드민 업로드, §15-3)

## 8. 카카오맵

- [Kakao Developers](https://developers.kakao.com) 앱 생성 → **JavaScript 키** → 플랫폼 Web 에 도메인 등록: `http://localhost:8080`, `https://wedding.thaiuniontour.com`. (도메인 미등록 시 지도 안 뜸 — 도메인 바꾸면 여기도 수정)
- `<script src="//dapi.kakao.com/v2/maps/sdk.js?appkey=${KAKAO_JS_KEY}">` + 마커 1개 + 줌 컨트롤. 커스텀 오버레이로 식장명.
- JS 키는 어차피 브라우저에 노출되는 키. 도메인 제한이 보호 수단. `application.properties` 의 `kakao.js-key` 로 주입(환경변수 `KAKAO_JS_KEY`).
- 주소는 HTML 에 그대로 표시하고 옆에 "복사" 텍스트 버튼(`data-copy`). 식장 주소는 공개 정보라 숨기지 않는다(2026-10-11 사용자 결정).
- 길찾기 버튼은 **카카오맵 하나만** (사용자 결정 2026-10-10). `https://map.kakao.com/link/search/{검색어}` — 장소명 검색이라 좌표 오차와 무관, 앱 미설치 시 웹으로 열림. 네이버·티맵·구글 버튼은 제거.
- 무료 쿼터 30만/일. 300명이면 무관.

## 9. 데이터 모델 (SQLite, `schema.sql`)

```sql
CREATE TABLE IF NOT EXISTS visit (
  session_id TEXT PRIMARY KEY,
  first_seen TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS pick (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT NOT NULL,
  round      INTEGER NOT NULL,          -- 1=32강 … 5=결승
  winner_id  TEXT NOT NULL,             -- 파일명 stem, 예 '07'
  loser_id   TEXT NOT NULL,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_pick_round_winner ON pick(round, winner_id);

CREATE TABLE IF NOT EXISTS guestbook (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL,
  message     TEXT NOT NULL,
  pw_hash     TEXT NOT NULL,              -- BCrypt(4자리)
  deleted_yn  TEXT NOT NULL DEFAULT 'N',
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);

-- 컨텐츠 허브 (§4-3). content 테이블 DDL 은 §4-3 참조.

CREATE TABLE IF NOT EXISTS event (            -- 가벼운 분석용. PII 없음
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT NOT NULL,
  name       TEXT NOT NULL,                     -- 'visit', 'share.in', 'worldcup.start', 'worldcup.finish', 'guestbook.write' …
  meta       TEXT,                              -- JSON 문자열, 선택
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE INDEX IF NOT EXISTS idx_event_name ON event(name);
```

`visit` 은 `event(name='visit')` 로 흡수 가능하지만 heartbeat 의 `total` 계산을 PK 조회 한 번으로 끝내기 위해 유지.

텍스트 문구(이름·장소명·인사말·버튼)는 `label` 테이블(§6-2). 번역 불필요한 값(예식 일시·위경도·계좌번호·base-url)만 `application.yml` 의 `wedding.*` 프로퍼티. 퀴즈·타임라인 데이터는 어드민(§15)에서 입력.

## 10. API

| Method | Path | Req | Res | 비고 |
|---|---|---|---|---|
| GET | `/` | 쿠키 `lang` | HTML | 청첩장 + 컨텐츠 허브(`content` 테이블). `label` 3개 언어 JSON 임베드 |
| GET | `/c/{key}` | | HTML | 컨텐츠 페이지 (§4-2). 미등록 key → 404 |
| GET | `/photos/{name}.webp` | | image | 정적 리소스. 1년 캐시 |
| POST | `/api/heartbeat` | `{sid}` | `{online, total}` | §6 |
| POST | `/api/pick` | `{sid, round, winnerId, loserId}` | 204 | round 1~5, id 는 존재하는 파일명만 허용 (화이트리스트 검증) |
| GET | `/api/ranking` | | `{participants, top:[{id, wins}]}` | 결승 후 결과 화면 |
| POST | `/api/event` | `{sid, name, meta?}` | 204 | name 은 화이트리스트. 유입 추적용 |
| GET | `/api/labels` | | `{ko:{},en:{},th:{}}` | 페이지에 임베드하므로 보통 불필요. 문구 수정 후 새로고침 없이 반영하고 싶을 때만 |
| GET | `/api/guestbook?page=n` | | `{items:[{id,name,message,createdAt}], hasNext}` | 최신순 10개 |
| POST | `/api/guestbook` | `{sid, name, message, password}` | `{id}` | 길이 검증, 30초 쿨다운 |
| DELETE | `/api/guestbook/{id}` | `{password}` | 204 / 403 | 본인 비번 또는 마스터 비번 |

인증 없음. 입력 검증(round 범위, id 화이트리스트, sid 길이 ≤ 64)만 한다. rate limit 은 Cloudflare 기본 보호에 맡김.

**도메인 바꿀 때 수정할 곳 4군데**: cloudflared `config.yml` ingress hostname · Kakao Developers 플랫폼 도메인 · `wedding.base-url`(og:url·Origin 검사) · Cloudflare Turnstile 사이트 도메인.

## 11. 배포

**로컬 개발**
```bash
./gradlew bootRun            # http://localhost:8080, SQLite ./data/wedding.db, 사진 ./data/photos
```

**Pi 배포 (최초)**
```bash
# 1) 코드 올리기 (tut 과 같은 방식: git pull on Pi)
ssh seonghanim 'cd ~/workspace/huiyeong && git clone <repo> wedding'   # 또는 모노레포 pull 후 wedding/ 사용
# 2) 사진·env
scp -r data/photos seonghanim:~/workspace/huiyeong/wedding/data/
ssh seonghanim 'printf "KAKAO_JS_KEY=...\nGUESTBOOK_MASTER_PW=...\n" > ~/workspace/huiyeong/wedding/.env'
# 3) 빌드·기동 (arm64 는 Pi 에서 빌드)
ssh seonghanim 'cd ~/workspace/huiyeong/wedding && docker-compose up -d --build'
# 4) 터널 ingress 한 줄 추가 + DNS
#    ~/workspace/huiyeong/thaiuniontour/cloudflared/config.yml 의 ingress 맨 앞에
#      - hostname: wedding.thaiuniontour.com
#        service: http://wedding-app:8080
ssh seonghanim 'docker exec tut-cloudflared cloudflared tunnel --origincert /etc/cloudflared/cert.pem route dns 08acb9b7-11aa-48ad-a7f3-29df69c6748a wedding.thaiuniontour.com && docker restart tut-cloudflared'
```

`docker-compose.yml` (wedding/)
```yaml
services:
  app:
    build: .
    image: wedding-app
    container_name: wedding-app
    env_file: .env
    environment:
      APP_DATA_DIR: /data
    volumes: ["./data:/data"]          # sqlite + photos
    networks: [docker_default]         # tut-cloudflared 와 같은 네트워크
    restart: always
networks:
  docker_default:
    external: true
```

`Dockerfile`: 멀티스테이지(gradle 빌드 → `eclipse-temurin:17-jre`, Pi 에 이미 캐시됨). `-Xmx256m`.

**실제 배포 (2026-10-12 완료)**
- 코드: Pi `~/workspace/huiyeong/D-Day` (모노레포 clone), 실행은 `D-Day/wedding`. `.env`·`data/` 는 Pi 에만.
- Pi 의 compose 는 v1 이 아니라 **docker-compose 2.29**. `.env` 의 BCrypt 해시는 작은따옴표로 감싸야 `$` 가 안 풀린다.
- `/tmp` tmpfs 는 `exec` 필요(sqlite-jdbc 네이티브 .so). 최신 temurin 이미지엔 UID 1000(ubuntu) 이 이미 있어 `USER 1000:1000` 만 지정.
- 터널 ingress 는 `thaiuniontour/cloudflared/config.yml` 맨 앞(백업 `config.yml.bak-*`), DNS 는 `route dns` 로 등록됨.
- 사진·퀴즈 등 데이터는 DB 를 옮기지 않고 운영 어드민에서 입력.

**재배포**: `cd ~/workspace/huiyeong/D-Day && git pull && cd wedding && docker-compose up -d --build`. 사진은 어드민에서.

**백업**: `data/wedding.db` 파일 하나. 결혼식 후 `scp` 로 가져와 랭킹 보관.

## 12. 백로그 (요청 시 추가, 지금은 안 만듦)

- Kakao Share SDK 커스텀 공유 버튼 — OG 로 충분하면 생략.
- 어드민 통계 페이지 — `sqlite3 data/wedding.db` 로 직접 보면 됨. 요청 시 basic-auth 페이지 1장.
- 갤러리 사진 업로드 UI — scp 로 충분.
- 컨텐츠: `rsvp` · `song` (§4-3 표). quiz·timeline 은 §15.
- 퀴즈 문제 다국어(en·th) — 지금은 어드민 입력 1개 언어.
- 라벨 편집 어드민 — `sqlite3` UPDATE 로 충분. 번역가(태국어 담당)가 직접 고쳐야 하면 basic-auth 표 편집 페이지 1장.
- 전체 월드컵 결과 공개 랭킹 페이지(40장 전체 승률) — 결과 화면 TOP3 로 충분한지 보고.
- Tailwind CLI 정적 빌드(CSP `unsafe-inline` 제거용) — §13-2.
- 새 도메인 구매 시 이전 — §10 의 3군데.

## 13. 보안 (로그인 없는 공개 서비스)

위협 모델: 하객 외에도 링크가 돌면 **누구나** 접근. 공격자가 노릴 것은 ① 방명록 스팸/욕설/XSS ② 투표·접속자 수 조작 ③ Pi 자원 고갈 ④ 개인정보(계좌·주소·사진) 긁기 ⑤ Pi 자체 침투. 개인 청첩장이라 "완벽"보다 **싸고 확실한 것부터**.

### 13-1. 경계: Cloudflare (무료 플랜, 설정만)
| 설정 | 효과 |
|---|---|
| 터널만 노출, Pi 인바운드 포트 0개 | 원 IP 숨김. 포트스캔 무의미 |
| **Bot Fight Mode** ON | 스크립트 봇 1차 차단 |
| **Rate Limiting rule** (무료 1개): `/api/*` POST 10회/10초/IP → 429 | 투표·방명록·heartbeat 플러딩 |
| WAF Managed Rules(무료 기본) | 흔한 인젝션 페이로드 |
| SSL **Full (strict)** + HSTS + "Always HTTPS" | 평문 차단 |
| 결혼식 당일 급할 때 **Under Attack Mode** 토글 | JS 챌린지로 봇 전부 차단 |
| **Turnstile**(무료 캡차) 위젯 → 방명록 작성 시 토큰 검증 | 스팸 글. 사용자에겐 보통 체크 없이 통과 |

### 13-2. 앱: 입력은 전부 의심
- **모든 요청 바디 상한** `server.max-http-request-size`/`max-http-form-post-size` = 16KB. 파일 업로드 없음 → multipart 비활성.
- **화이트리스트 검증**: `round` 1~5 · `winnerId/loserId` 는 디렉토리 스캔 결과에 있는 파일명만 · `event.name` 은 enum · `sid` 는 UUID 정규식 · 방명록 이름 20자/메시지 300자, 제어문자 제거 · `lang` 은 `ko|en|th`.
- **XSS**: 출력은 `th:text`·`textContent` 만. `th:utext`/`innerHTML` 금지(CLAUDE.md 규칙). CSP 헤더로 2중 방어: `default-src 'self'; script-src 'self' dapi.kakao.com *.daumcdn.net challenges.cloudflare.com cdn.tailwindcss.com; img-src 'self' data: *.daumcdn.net; connect-src 'self' dapi.kakao.com; frame-src challenges.cloudflare.com; style-src 'self' 'unsafe-inline' fonts.googleapis.com; font-src fonts.gstatic.com`. 인라인 스크립트는 nonce 로.
  > Tailwind CDN 은 런타임에 `<style>` 을 주입해 `style-src 'unsafe-inline'` 이 필요. 거슬리면 구현 시 Tailwind CLI 로 정적 CSS 빌드(빌드 1단계 추가). 구현 때 결정.
- **CSRF**: 세션 쿠키 기반 인증이 없으니 전통적 CSRF 영향 없음. 그래도 `/api/**` 는 `Content-Type: application/json` 필수 + `Origin` 이 `wedding.base-url` 과 일치할 때만 허용(한 줄 인터셉터). `lang` 쿠키는 `SameSite=Lax; Secure`.
- **보안 헤더**: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: geolocation=(), camera=()`. Spring Security 없이 `OncePerRequestFilter` 한 개.
- **앱 레벨 레이트리밋**(Cloudflare 뒤 2차): `CF-Connecting-IP` 기준 메모리 토큰버킷. heartbeat 12회/10초, pick 40회/분, guestbook 1회/30초, event 20회/분. 초과 429. 맵은 `ConcurrentHashMap` + 1분 주기 만료 — §6 heartbeat 와 같은 패턴 재사용.
  > `X-Forwarded-For` 는 믿지 않는다. 터널 경유라 `CF-Connecting-IP` 만 신뢰. 로컬 개발은 `remoteAddr` fallback.
- **접속자 수 조작**: 1 IP 당 online 집계 최대 3 sid. 그 이상은 count 안 함. 완벽하지 않지만 "1000명 보는 중" 장난은 막힘.
- **월드컵 조작**: sid 당 round 별 pick 횟수 상한(32강 16·16강 8·8강 4·4강 2·결승 1). 초과 무시. 랭킹은 `round=5` 만 쓰므로 sid 당 우승 1표.
- **방명록**: Turnstile 토큰 서버 검증 → 쿨다운 → BCrypt(strength 10). 마스터 비번은 BCrypt 해시를 환경변수로(평문 X). 삭제 실패 5회/IP/10분 → 잠금. 욕설 필터는 안 만든다(마스터 삭제로 충분).
- **에러 응답**: 스택트레이스 노출 금지. `server.error.include-*=never`, 전역 `@ExceptionHandler` 로 `{code}` 만.
- **정보 노출**: 계좌번호·주소는 HTML 에 그대로 표시한다(2026-10-11 사용자 결정 — 청첩장 관례상 노출이 자연스럽고, 크롤링 방어보다 하객 편의 우선). Actuator 비활성. 계좌·주소 값은 label 이 아니라 yml 에 둔다.
- **로그**: IP 는 로그에 남기지 않음(해시 or 생략). 방명록 본문도 로그 금지. `event.meta` 에 PII 금지.

### 13-3. 컨테이너·Pi
- 컨테이너 **non-root**(`USER 1000`), `read_only: true` + `/tmp` tmpfs, `/data` 만 rw. `cap_drop: [ALL]`. 메모리 `mem_limit: 512m` → 폭주해도 tut/bloomi 안 죽임.
- `.env` 권한 600. 비밀값은 compose `env_file` 로만. 이미지에 굽지 않음. `.gitignore` 에 `.env`, `data/`.
- SQLite 파일 `data/wedding.db` 권한 600, 일 1회 `cron` 으로 `sqlite3 .backup` → `data/backup/`(7일 보관). Pi 디스크 장애 대비 주 1회 내 Mac 으로 `scp`.
- 의존성: Spring Boot 최신 패치 버전 고정, `./gradlew dependencyCheck`(OWASP) 는 과함 → GitHub Dependabot 만 켬.
- **Pi 접속**: 현재 비밀번호 SSH. 친구 기기라 강제는 못 하지만 **키 인증 + `PasswordAuthentication no`** 권장. 최소한 내 키 등록 후 비밀번호는 안 쓰기. 이 문서·메모리에 비밀번호 기록 금지.
- Cloudflare 계정 2FA. Kakao Developers 키는 도메인 제한.
- 결혼식 후: 방명록 export → 컨테이너 내리고 ingress 제거. 방치된 공개 서비스가 제일 위험.

### 13-4. 안 하는 것 (근거)
- 로그인/회원: 하객에게 가입 요구는 이탈 1순위. 익명 + 위 통제로 충분. (어드민 로그인은 나 혼자, §15-1)
- Spring Security 전체 도입: 인증이 없는데 필터체인 수십 개 붙일 이유 없음. 헤더 필터 1개 + 인터셉터 1개로 끝.
- Redis 기반 분산 레이트리밋: 인스턴스 1개.
- 욕설 필터·AI 모더레이션: 마스터 삭제 + Turnstile 로 충분. 문제 생기면 그때.

## 15. 어드민 · 퀴즈 · 타임라인 (2026-10-12 추가)

퀴즈 문제와 타임라인 사진을 화면에서 넣기 위해 어드민을 둔다. §7·§12·§13-4 의 "어드민 없음" 은 이 범위만큼 개정.

### 15-1. 어드민 (`/admin`)
- 사용자는 나 하나. 아이디(`ADMIN_ID`, 기본 `admin`) + 비밀번호 BCrypt 해시를 `.env` 의 `ADMIN_PW_HASH` 로. 비어 있으면 로그인 자체가 막힌다.
- Spring Security 없이 `HttpSession` + 인터셉터 1개(`AdminInterceptor`, `/admin/api/**`). 로그인 성공 시 세션 ID 교체(고정 공격 방지). 세션 쿠키 `HttpOnly; SameSite=Strict`, 운영은 `Secure`(compose 환경변수). 2시간 만료.
- 로그인 시도 5회/10분/IP. 변경 요청은 Origin 검사 + `application/json` 또는 `image/jpeg` 만.
- 화면 문구는 한국어 하드코딩 허용(어드민 전용, 하객 노출 없음). `noindex`.
- 기능: 사진(§15-6), 타임라인, 퀴즈, 방명록 관리(§15-7). 그 외(라벨 편집·통계)는 백로그.

### 15-2. 퀴즈 (`/c/quiz`)
| 난이도 key | 화면(ko) | 출제 배점 |
|---|---|---|
| `easy` | 덜친함 | 1~2점 |
| `normal` | 중간친함 | 3~4점 |
| `hard` | 매우친함 | 4~5점 (4점은 중간친함과 의도적으로 겹침) |

- 문제는 4지선다(보기 2~4개). 배점 1~5. 문제마다 고유 `id`.
- 응시: 난이도 고르면 해당 배점 풀에서 **랜덤 10문제**(부족하면 있는 만큼), **문제 순서·보기 순서 모두 서버에서 섞음**. 보기는 `{no, text}` 로 내려가고 `no` 는 원래 인덱스라 순서로는 정답을 알 수 없다.
- **정답은 어떤 응답에도 넣지 않는다.** 대신 개발자 친구용 `hint` 필드를 응답에만 넣고 화면엔 그리지 않는다(콘솔에 인사 한 줄).
- 채점은 서버에서 한 번에: 응시 시작 시 `quiz_attempt` 1행(출제 id 목록) → 제출은 그 응시 id 로 **1회만**. 응답은 총점·만점·문제별 O/X 만(정답 보기는 알려주지 않음). 문제별 확인 API 가 없으니 보기 하나씩 찔러보기 불가.
- 레이트리밋: 시작·제출 각 20회/10분/IP.

### 15-3. 타임라인 (`/c/timeline`)
- 연애 시작 2025-05-04(`wedding.dating-start`) ~ 예식 월. 월 단위 눈금의 슬라이더(`<input type=range>`, step 1개월).
- 사진 있는 달만 눈금 강조, 드래그하면 **가장 가까운 사진 있는 달로 붙는다**. 그 달 사진(여러 장이면 가로 스와이프)과 한 줄 설명 표시. 상단에 "함께한 지 N일".
- 사진은 어드민에서 업로드. 브라우저가 긴 변 1600px JPEG 로 줄여서 올린다(HEIC 변환을 서버에서 안 하려고). 서버는 5MB 상한·JPEG 디코딩 검증 후 `data/timeline/{uuid}.jpg` 로 저장, `/timeline/**` 1년 캐시.

### 15-4. 테이블
```sql
CREATE TABLE IF NOT EXISTS quiz (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  question   TEXT NOT NULL,
  choices    TEXT NOT NULL,                 -- JSON 배열, 보기 2~4개
  answer_no  INTEGER NOT NULL,              -- choices 의 0-based 인덱스. 응답에 절대 포함 안 함
  points     INTEGER NOT NULL CHECK (points BETWEEN 1 AND 5),
  hint       TEXT,                          -- 개발자 도구로만 보이는 힌트
  deleted_yn TEXT NOT NULL DEFAULT 'N',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS quiz_attempt (
  id           TEXT PRIMARY KEY,            -- UUID. 응시 1회
  session_id   TEXT NOT NULL,
  level        TEXT NOT NULL,               -- easy | normal | hard
  quiz_ids     TEXT NOT NULL,               -- 출제 id, 콤마 구분(출제 순서)
  max_score    INTEGER NOT NULL,
  score        INTEGER,
  submitted_at TEXT,                        -- NULL 이면 미제출. 제출은 1회
  created_at   TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS timeline (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  month      TEXT NOT NULL,                 -- 'YYYY-MM'
  file       TEXT NOT NULL,                 -- data/timeline/ 안 파일명
  caption    TEXT,
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
```

### 15-5. API
| Method | Path | Req | Res |
|---|---|---|---|
| POST | `/api/quiz/start` | `{sid, level}` | `{attemptId, items:[{id, question, points, choices:[{no,text}], hint?}]}` |
| POST | `/api/quiz/{attemptId}/submit` | `{answers:[{id, no}]}` | `{score, max, results:[{id, correct}]}` / 409 재제출 |
| GET | `/admin` | | 어드민 HTML |
| POST | `/admin/api/login` · `/logout` | `{username, password}` | 204 / 403 |
| GET·POST | `/admin/api/quiz` | `{question, choices, answerNo, points, hint}` | 목록 / `{id}` |
| PUT·DELETE | `/admin/api/quiz/{id}` | 위와 같음 | 204 |
| GET | `/admin/api/timeline` | | 목록 |
| POST | `/admin/api/timeline?month=&caption=` | body `image/jpeg` | `{id}` |
| PUT·DELETE | `/admin/api/timeline/{id}` | `{month, caption}` | 204 |

### 15-6. 사진 관리 (2026-10-12 개정, §7 대체)
- `data/photos` 폴더 스캔 → **`photo` 테이블**. 갤러리·월드컵·메인·카톡 미리보기가 전부 이 테이블을 본다.
- 파일 3종. 업로드할 때 **브라우저가** 만든다(EXIF 회전 반영 + Pi 메모리 보호):
  | 종류 | 경로 | 크기 | 공개 |
  |---|---|---|---|
  | 원본 | `data/photos/original/{uuid}.{jpg,png,heic}` | 폰 원본 그대로 | **비공개**(서빙 경로 없음). 보관용 |
  | 보기용 | `/photos/view/{uuid}.jpg` | 긴 변 2560px, JPEG 0.92 | 메인·갤러리 뷰어·월드컵 결과 |
  | 썸네일 | `/photos/thumb/{uuid}.jpg` | 긴 변 600px | 목록·월드컵 카드 |
- 업로드 1장 = 요청 1개. body 는 `보기용 | 썸네일 | 원본` 을 이어 붙인 바이트, 경계는 쿼리 `?view=길이&thumb=길이`. 원본 상한 40MB. 동시 2장, 장마다 진행률(XHR) + 전체 `n / N`, 실패분만 재시도.
- 순서: `sort`. 어드민에서 드래그(SortableJS, 어드민 전용 CDN). **1번 = 메인**(첫 화면·og:image). 메인 아래 미리보기는 2번부터.
- 월드컵: 사진마다 `worldcup_yn` 체크. 체크된 사진 중 랜덤 32장(모자라면 2의 거듭제곱 최대).
- 다운로드 버튼 없음. 이미지 길게 누르기·우클릭 저장은 막는다(`-webkit-touch-callout:none`, contextmenu 차단). 스크린샷까지는 못 막음.
- `scripts/convert.sh`·`placeholder-photos.sh` 폐기.

### 15-7. 방명록 사진 첨부
- 하객이 글 하나에 **최대 3장**. 올리자마자 공개. 문제 사진은 어드민에서 글 숨김 또는 사진 삭제.
- 폰에서 긴 변 2048px 로 줄여 `POST /api/guestbook/photo`(image/jpeg, 4MB 상한) → 토큰. 글 작성 시 토큰 배열을 함께 보냄.
- 서버는 **반드시 디코딩 후 다시 인코딩**(EXIF·GPS 제거, 이미지 위장 파일 차단) + 600px 썸네일 생성. `data/guestbook/{uuid}.jpg`, `{uuid}_t.jpg`.
- 제한: IP 당 10장/10분, 전체 하루 500장. 글에 안 붙은 사진은 하루 지나면 삭제.

### 15-8. 추가 테이블
```sql
CREATE TABLE IF NOT EXISTS photo (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  file        TEXT NOT NULL,              -- uuid. view/thumb 파일명
  original    TEXT NOT NULL,              -- original/ 안 파일명(확장자 포함)
  sort        INTEGER NOT NULL,           -- 1 = 메인
  worldcup_yn TEXT NOT NULL DEFAULT 'Y',
  created_at  TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE TABLE IF NOT EXISTS guestbook_photo (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  guestbook_id INTEGER,                   -- NULL = 아직 글에 안 붙음
  file         TEXT NOT NULL,             -- uuid
  created_at   TEXT NOT NULL DEFAULT (datetime('now'))
);
```
`pick.winner_id/loser_id` 는 `photo.id` 문자열.

### 15-9. API 추가
| Method | Path | Req | Res |
|---|---|---|---|
| POST | `/api/guestbook/photo` | body `image/jpeg` | `{token}` |
| POST | `/api/guestbook` | 기존 + `photos:[token]` (≤3) | `{id}` |
| GET·POST | `/admin/api/photos` | `?view=&thumb=&worldcup=` body octet-stream | 목록 / `{id}` |
| PUT | `/admin/api/photos/order` | `{ids:[...]}` | 204 |
| PUT·DELETE | `/admin/api/photos/{id}` | `{worldcup}` | 204 |
| GET | `/admin/api/guestbook?page=` | | 숨김 포함 목록 + 사진 |
| PUT | `/admin/api/guestbook/{id}` | `{hidden}` | 204 |
| DELETE | `/admin/api/guestbook/photo/{photoId}` | | 204 |

### 15-10. 사진 저장소: 네이버 Ncloud Storage (2026-10-13)
- 이유: Cloudflare 무료 플랜은 국내 통신사에서 시애틀(SEA) 엣지로 붙어 사진 340KB 가 15초+. Ncloud Storage(서울) 직접 응답 0.05초.
- 버킷 `wedding`(비공개). 새 Ncloud Storage 는 공개 ACL 이 없어 **서명 URL(7일 유효)** 로 내보낸다. 같은 키는 하루 동안 같은 URL 을 재사용(메모리 캐시)해 브라우저 캐시가 먹게 한다.
- 키 = data 디렉토리 기준 상대경로: `photos/view/{uuid}.jpg`, `photos/thumb/{uuid}.jpg`, `timeline/{uuid}.jpg`, `guestbook/{uuid}.jpg`·`_t.jpg`. **원본(`photos/original`)은 올리지 않고 Pi 에만.**
- 업로드 경로는 그대로 브라우저 → Pi(검증·재인코딩) → Ncloud. 키(`NCLOUD_ACCESS_KEY/SECRET_KEY`)가 비어 있으면 로컬 디스크 + `/files/**` 로 서빙(개발용).
- 기존 로컬 파일은 기동 시 한 번 버킷으로 올리고 `data/.migrated-ncloud` 표식을 남긴다.
- SDK: AWS SDK v2 `s3`(엔드포인트 `https://kr.ncloudstorage.com`, 리전 `kr`). S3 호환이라 나중에 AWS·R2 로 옮겨도 설정만 바꾼다.

### 15-11. Tailwind CDN → 미리 만든 CSS (2026-10-13)
- `cdn.tailwindcss.com` 런타임이 첫 화면을 2초 막고 있었다. `scripts/tailwind.sh`(npx tailwindcss@3.4.17)로 `static/tw.css`(약 17KB) 를 만들어 커밋. Pi 빌드엔 node 불필요.
- 템플릿·JS 의 클래스를 바꾸면 스크립트를 다시 돌린다. `tailwind.config.js` content 에 `static/*.js` 포함(JS 가 넣는 클래스).
- `/static/**` 은 내용 해시 URL(`@{/static/..}`) + 1년 캐시. CSP 에서 cdn.tailwindcss.com 제거.

### 15-12. CSS·공통 JS 인라인 (2026-10-13)
- 첫 방문 때 `tw.css`·`app.css`·`app.js` 가 각각 Cloudflare(SEA) 왕복 1~5초. 하객 페이지는 세 파일을 HTML 에 직접 넣는다(`InlineAssets`, 기동 시 1회 로드). 압축 후 약 10KB 증가.
- 우리 빌드 산출물이라 `th:utext` 예외. 닫는 태그가 들어 있으면 기동 실패하게 막아둠. 페이지별 스크립트(quiz.js 등)·어드민은 그대로 파일.

## 14. 확인 필요 (사용자 결정)

1. 서브도메인 이름: `wedding.thaiuniontour.com` 으로 가정. 다른 이름?
2. 예식 일시·장소(위경도)·양가 성함·계좌·인사말 — 구현 시 `application.yml` 에 넣을 값.
3. 접속자 수를 하객에게 공개 표시(푸터)로 가정. 나만 보고 싶으면 어드민 페이지(백로그)로 전환.
4. RSVP(참석 여부) 필요 여부. 방명록은 확정.
5. 사진 40장 원본 전달 경로(폴더).
6. 컨텐츠 허브 카드 초기 구성: 월드컵 · 방명록 · 갤러리 · 오시는 길 4개로 가정. RSVP 를 1차에 넣을지.
7. 태국어 번역 누가 하는지(본인/지인/기계번역 초안 후 검수). 초기 `labels.sql` 은 한국어 확정 후 영·태 초안을 내가 채우고 검수받는 흐름 제안.
