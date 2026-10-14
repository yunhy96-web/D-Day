# 가족 홈페이지 — 설계

구현 중 설계와 달라지면 이 문서부터 고친다.

## 0. 한 줄 요약
우리 가족만 쓰는 사이트. 공개 도메인의 로그인 화면으로 들어와 사진과 일기를 올리고 댓글로 이야기한다. 한국어·태국어 화면, 라이트·다크 모드. 청첩장(`wedding/`)과 같은 스택·인프라를 쓰고, 나중에 다른 가족(양가)도 받을 수 있게 데이터는 처음부터 `family_id` 로 나눈다.

## 1. 결정 사항
| 날짜 | 항목 | 결정 |
|---|---|---|
| 10-14 | 도메인 | `family.thaiuniontour.com` (서브도메인, 도메인 바꿀 때 같이 이전) |
| 10-14 | 범위 | 우리 가족 1개로 시작. 구조는 가족 여러 개를 받을 수 있게 |
| 10-14 | 사진 저장소 | Ncloud Storage 버킷 `family` (비공개, 생성 완료) |
| 10-14 | 가입 | 초대 링크 없음. 공개 로그인 화면 + 회원가입 → **관리자 승인 후 사용** |
| 10-14 | 언어 | 한국어(ko)·태국어(th). 화면 문구는 전부 `label` 테이블 |
| 10-14 | 테마 | 시스템 따라감 / 라이트 / 다크, 회원별 저장 |
| 10-14 | 비밀번호 | BCrypt 단방향 해시. 관리자·DB 유출 시에도 원문 복구 불가 |
| 10-14 | 관리자 계정 | 아이디 `admin`. 비밀번호 해시는 `.env` 의 `ADMIN_PW_HASH`(로컬 생성 완료, 커밋 안 됨) |
| 10-14 | 권한 | 역할·권한 테이블 분리(`role`·`role_permission`·`member_role`, §4-1) |
| 10-14 | 글 종류 | 순간(사진 위주)·일기(제목·긴 글·나만 보기 가능) 두 종류(§5) |
| 10-14 | 프론트 | React 분리 안 함. Thymeleaf 서버 렌더링 유지. 느린 원인은 Cloudflare 경로라 분리로는 안 빨라진다(§2-1) |
| 10-14 | 근무 캘린더 | 개인별 교대근무(2·3·4교대 프리셋, 교대별 시작·종료 시각), 날짜 탭으로 바로 입력, 자동 저장(§5-1) |
| 10-14 | 글씨 크기 | 작게·보통·크게 3단계, 회원별 저장(§7-1) |
| 10-14 | 저장 방식 | 설정·캘린더는 저장 버튼 없이 바꾸는 즉시 자동 저장. 글 올리기만 버튼(§7-2) |
| 10-14 | 장기 비전 | 가족 기록(사진·음성·글)으로 고인을 AI 로 복원하는 것까지 고려. 지금은 **원본 데이터를 잘 모으는 구조**만 만든다(§15) |

## 2. 스택 (청첩장 재사용)
- Spring Boot 3 / Java 17 단일 모듈, `D-Day/family/`. JdbcTemplate + SQLite(`data/family.db`).
- 스키마는 **Flyway** 마이그레이션(`db/migration/V1__init.sql`, `V2__...`). 청첩장의 `CREATE IF NOT EXISTS` 방식은 컬럼 추가를 못 하는데, 이 사이트는 수십 년 쓸 데이터라 변경 이력이 필요하다. 라벨은 `R__labels.sql`(`INSERT OR IGNORE`).
- Thymeleaf + 미리 만든 Tailwind CSS(`static/tw.css`, `darkMode: 'class'`) + vanilla JS.
- 사진: Ncloud Storage 버킷 `family`, 서명 URL.
- 배포: Pi docker-compose, `tut-cloudflared` ingress 한 줄.
- 청첩장에서 **복사**해 오는 클래스: `ObjectStore`, `Images`, `RateLimiter`, `ClientIp`, `SecurityHeadersFilter`, `OriginInterceptor`, `Valid`, `ApiExceptionHandler`, `InlineAssets`, `LabelService`. 공유 모듈은 만들지 않는다(두 앱뿐이라 복사가 더 싸다).
- 추가 금지: Spring Security, JPA, Redis, Spring MessageSource·i18n 라이브러리, 프론트 빌드 도구.

## 2-1. 왜 React 로 분리하지 않나 (속도)
2026-10-14 측정: 청첩장(`wedding.thaiuniontour.com`) HTML 64KB 한 번 요청에 **1.0~1.5초**, Cloudflare 엣지는 `LAX`(로스앤젤레스). 국내 → 미국 엣지 → 터널 → 한국 Pi 로 돌아오는 경로 때문이다.
- 이 경로는 프론트를 분리해도 그대로다. API 요청도 같은 터널을 탄다.
- 오히려 SPA 는 첫 화면에 HTML → JS 번들 → API 호출 2~3번으로 왕복이 늘어 **더 느려진다**(요청당 1초+). 서버 렌더링은 한 번 왕복에 완성된 화면.
- 무거운 사진은 이미 Ncloud(서울)에서 직접 받으므로 터널을 안 탄다.
- **진짜 해결책**은 Cloudflare 를 거치지 않는 것: (a) 친구 공유기 포트포워딩 + Pi 에 Caddy(Let's Encrypt 자동 인증서) + DNS 를 Cloudflare 프록시 끔(회색 구름), (b) Ncloud 서버(서울) 1대로 이전. 둘 다 프론트 구조와 무관하므로 나중에 결정해도 된다.
- API 는 처음부터 `/api/**` JSON 으로 분리해 두므로, 나중에 앱(React Native 등)을 만들어도 백엔드는 그대로 쓴다.

## 3. 확장성: 가족 단위로 나누기
- 모든 컨텐츠(글·사진·댓글)는 `family_id` 를 가진다. 사람은 `family_member` 로 여러 가족에 속할 수 있다(양가 대비).
- 로그인 세션에 "지금 보고 있는 가족"(`family_id`)을 넣고, **모든 조회·수정 쿼리는 세션의 `family_id` 로 거른다.** URL·요청 본문으로 받은 `family_id` 는 믿지 않는다.
- 지금은 가족 1행(`id=1`). 가입 신청은 이 가족으로 들어온다. 가족 선택·전환 UI 는 2번째 가족이 생길 때 만든다.
- 기능 추가 방식: 기능마다 패키지(`com.dday.family.<기능>`) + 자기 테이블. 기능끼리 서로의 테이블 직접 참조 금지(글 ID 참조는 허용).

## 4. 회원·로그인
- **로그인 `/login`**: 공개. 아이디·비밀번호. 언어 전환(ko/th)과 테마 전환이 로그인 전에도 보인다.
- **회원가입 `/signup`**: 공개. 아이디(영문·숫자 4~20자)·비밀번호(8자 이상)·이름·언어. 가입하면 `family_member.status = PENDING` 이고 "승인을 기다리는 중" 화면만 보인다. 관리자가 `/admin` 에서 승인하면 `ACTIVE`.
  - 공개 가입이라 아무나 신청할 수 있다. 승인 전에는 어떤 컨텐츠도 볼 수 없고, 가입은 IP 당 3회/시간 제한. 대기 신청이 있으면 관리자 피드 상단에 "승인 대기 N명".
- **비밀번호 분실**: 관리자가 `/admin` 에서 임시 비밀번호로 초기화 → 직접 전달 → 로그인 후 변경 강제(`member.must_change_pw = 'Y'`). 이메일·문자 인증 없음.
- **세션**: `session` 테이블 + 쿠키(`HttpOnly; Secure; SameSite=Lax`, 90일, 접속할 때마다 연장). 서버 메모리 세션이 아니라서 재배포해도 다시 로그인할 일이 없다.
- 비밀번호는 BCrypt(cost 10) 해시로만 저장. 암호화가 아니라 단방향이라 복호화 경로 자체가 없다. 로그인 실패 IP 당 10회/10분 제한(`RateLimiter`). 실패 메시지는 "아이디 또는 비밀번호가 틀렸습니다" 하나(아이디 존재 여부 노출 안 함).
- 권한은 §4-1.
- 최초 관리자: 기동 시 회원이 0명이면 `.env` 의 `ADMIN_LOGIN_ID`(=`admin`)·`ADMIN_PW_HASH` 로 관리자 1명 + 가족 1행 + `ADMIN` 역할 부여.

### 4-1. 역할·권한
- **역할**(`role`)은 권한 묶음, **권한**(`role_permission.permission`)은 코드 문자열, 사람에게는 **가족별로 역할을 부여**(`member_role`). 한 사람이 역할 여러 개 가능.
- 기본 역할(V1 에 시드):
  | 역할 | 권한 |
  |---|---|
  | `ADMIN` | 전부 |
  | `MEMBER` | `post.write`, `comment.write`, `media.upload`, `shift.write` |
  | `VIEWER` | (읽기만. 어린 조카·친척용) |
- 권한 코드(`<대상>.<동작>`): `post.write`, `post.delete.any`, `comment.write`, `comment.delete.any`, `media.upload`, `media.original.download`, `shift.write`, `shift.write.any`, `member.approve`, `member.manage`, `role.manage`, `audit.read`. 기능이 늘면 코드 상수 + `role_permission` 행만 추가.
- 자기 글·댓글 수정·삭제는 권한이 아니라 "작성자 본인" 규칙. `*.delete.any` 는 남의 것 삭제.
- 확인 방법: 로그인 시 세션의 권한 집합을 한 번 조회해 요청 동안 들고 있고, 컨트롤러에서 `auth.require("member.approve")`(없으면 403). Spring Security 없이 메서드 1개.
- 관리자 화면에서 회원별 역할 부여·회수. 역할 정의 편집 화면은 백로그(지금은 SQL).

## 5. 기능 (MVP)
1. **피드 `/`** — 가족의 글이 최신순. 상단 필터: 전체 · 순간 · 일기. 20개씩 더보기.
2. **글쓰기 `/write`, 수정 `/p/{id}/edit`** — 처음에 종류 선택:
   - **순간**(`moment`): 사진 0~20장 + 짧은 글. 카톡에 사진 올리듯.
   - **일기**(`diary`): 제목 + 긴 본문(줄바꿈 유지) + 사진 0~20장 + 날짜 + **공개 범위**(가족 공개 / 나만 보기). 나만 보기 일기는 관리자도 못 본다.
   - 날짜 기본값: 첫 사진의 촬영일, 없으면 오늘(작성자 시간대 기준).
   - 작성 중 내용은 브라우저 `localStorage` 에 임시 저장(긴 일기 날아감 방지).
3. **글 상세 `/p/{id}`** — 사진 넘겨보기, 댓글, 하트(1인 1회 토글, 누가 눌렀는지 이름 표시).
4. **앨범 `/album`** — 가족의 모든 사진을 촬영일순 그리드, 월 단위 구분선. 누르면 그 글로.
5. **내 정보 `/me`** — 프로필 사진, 표시 이름, 언어, 테마, **글씨 크기**, 시간대, 비밀번호 변경, 로그아웃. 전부 자동 저장.
6. **관리 `/admin`** — 가입 승인·거절, 회원 목록·비활성화, 비밀번호 초기화.

- **내 일기 `/diary`** — 내가 쓴 일기만 달력·목록으로(나만 보기 포함).
- **근무 캘린더 `/calendar`** — §5-1.

하단 탭(5개 고정): **홈 · 앨범 · ＋ · 캘린더 · 나**. 일기는 홈 필터와 `나` 화면에서 들어간다.
- 홈 맨 위 **"오늘" 줄**: 오늘 근무가 있는 가족만 한 줄씩(`아빠 · 야간 20:00–08:00`, `동생 · 휴무`). 누르면 캘린더 가족 보기.

### 5-1. 근무 캘린더 (`/calendar`)
아빠·동생처럼 교대근무하는 가족이 자기 근무표를 넣고, 가족 모두가 "오늘 누가 일하는지" 본다.

**처음 설정 (한 번만, 3단계)**
1. 근무 방식 고르기 — 큰 카드 4개: `2교대` · `3교대` · `4교대` · `직접 만들기`. 교대근무가 아니면 이 캘린더를 안 써도 된다(빈 화면에 "근무표 만들기" 버튼 하나).
2. 교대 확인·수정 — 프리셋이 채워진 목록. 각 줄: 색 동그라미 · 이름 · 시작–종료 시각(`<input type="time">`, 휠 피커). 이름·시각·색 탭해서 바로 수정, 자동 저장. 줄 추가·삭제 가능.
3. 순환 패턴(선택) — 프리셋 패턴이 칩으로 보이고("주 주 야 야 비 휴 휴 휴"), 시작일을 고르면 **3개월 미리 채움**. 건너뛰면 날짜마다 직접 입력.

**프리셋** (전부 수정 가능)
| 방식 | 교대(약자·기본 시각) | 기본 순환 |
|---|---|---|
| 2교대 | 주간 `주` 08:00–20:00 · 야간 `야` 20:00–08:00 · 휴무 `휴` | 주 주 야 야 휴 휴 |
| 3교대 | 주간 `D` 06:00–14:00 · 오후 `E` 14:00–22:00 · 야간 `N` 22:00–06:00 · 휴무 `휴` | D D E E N N 휴 휴 |
| 4교대(4조 2교대) | 주간 `주` 07:00–19:00 · 야간 `야` 19:00–07:00 · 비번 `비` · 휴무 `휴` | 주 주 야 야 비 휴 휴 휴 |
- 종료가 시작보다 이르면 다음 날 종료(야간). 화면에 `20:00–08:00(+1)`.
- 휴무·비번은 시각 없음. 회사마다 시각이 달라서 프리셋은 출발점일 뿐.

**날짜 입력 — "붓" 방식** (가장 자주 쓰는 동작이라 탭 한 번으로)
- 월 달력 아래에 교대 칩이 한 줄(`주` `야` `비` `휴` `지우개`). 칩 하나를 고르면(붓을 듦) 이후 날짜를 **탭하는 대로 그 교대로 칠해진다.** 여러 날을 연속으로 빠르게 탭.
- 칩을 안 골랐을 때 날짜를 탭하면 바텀시트: 교대 버튼들 + 메모 한 줄(`병원`, `회식`). 고르면 바로 닫히고 저장.
- 칠한 날은 셀 전체가 교대 색 배경 + 큰 약자. 오늘은 굵은 테두리. 패턴으로 채운 날과 직접 바꾼 날은 구분 안 함(사용자는 몰라도 된다).
- 달 이동: 좌우 스와이프 + `‹ 10월 ›`. "오늘" 버튼.

**자동 저장** (§7-2 규칙)
- 탭하는 즉시 화면이 바뀌고(낙관적 갱신) 뒤에서 `PUT` 1건. 상단에 작게 `저장됨 ✓`. 실패하면 셀에 점 표시 + 자동 재시도, 오프라인이면 모아 뒀다가 연결되면 전송.
- 실수 대비 **되돌리기**: 바꿀 때마다 아래 토스트 `야간으로 바꿨어요 · 되돌리기` 4초. 패턴 채우기는 `3개월 채웠어요 · 되돌리기`.

**보기**
- `나` / `가족` 토글. 가족 보기는 한 달 달력의 각 날짜에 사람별 작은 색 막대(아빠 파랑 계열, 동생 초록 계열 — 사람 색은 가족 안에서 자동 배정), 날짜를 누르면 그날 누가 어떤 근무인지 목록.
- 남의 근무표는 읽기만. 고칠 수 있는 건 자기 것 + `shift.write.any` 권한자(관리자가 부모님 대신 입력).
- 일정(생일·여행 등 가족 이벤트)은 같은 화면에 나중에 추가(백로그). 테이블이 다르므로 지금 구조에 영향 없음.

## 6. 다국어 (ko · th)
청첩장 §6-2 와 같은 `label` 테이블을 쓰되, 로그인 서비스라 **서버 렌더링 위주**로 단순화한다.
- 화면 문구는 템플릿·JS 에 하드코딩하지 않고 키로만 참조: `th:text="${L['feed.empty']}"`. 관리자 화면도 라벨 사용(관리자가 태국어를 쓸 수도 있으니).
- 초기값 `labels.sql`(`INSERT OR IGNORE`, ko·th 둘 다). 키 네이밍 `<화면>.<의미>`, 플레이스홀더 `{0}`. 번역이 빠진 키는 기동 시 경고 로그 + ko fallback.
- 언어 결정: 로그인 후 `member.lang` → 로그인 전 쿠키 `lang` → `Accept-Language`(`th*` 면 th, 그 외 ko).
- 언어를 바꾸면 저장 후 새로고침(페이지 내 즉시 교체 안 함). JS 가 쓰는 문구만 `<script type="application/json">` 으로 현재 언어 것만 임베드.
- 날짜: `Intl.DateTimeFormat(lang, {calendar:'gregory'})` 로 서기 고정(태국어 기본은 불기 พ.ศ.).
- 폰트: 한국어 Pretendard, 태국어 Noto Sans Thai. `:lang(th)` 로 교체.
- 사용자가 쓴 글·댓글은 번역하지 않는다(라벨은 UI 문구만). 글 번역 버튼은 백로그.

## 7. 다크 모드
- Tailwind `darkMode: 'class'`. 색은 전부 `bg-white dark:bg-neutral-900` 식으로 쌍으로 쓴다.
- 설정값 `member.theme`: `system`(기본) | `light` | `dark`. 로그인 전에는 쿠키 `theme`.
- 깜빡임 방지: 서버가 `<html data-theme="...">` 를 렌더링하고, `<head>` 인라인 스크립트 1줄이 `dark` 이거나 (`system` 이고 `prefers-color-scheme: dark`) 면 `class="dark"` 추가. `system` 은 OS 변경도 따라감(`matchMedia` change).
- 사진은 다크 모드에서도 그대로(필터 없음). 청첩장은 화이트 고정이었지만 여기서는 OS 설정을 따른다.

## 7-1. 글씨 크기 (작게 · 보통 · 크게)
나이 드신 가족도 편하게. 세 단계만 두고 단계마다 **글자만이 아니라 화면 구성이 같이 바뀐다.**

**구현 원리**: `<html data-font="sm|md|lg">` 로 루트 `font-size` 를 바꾸고(15 / 17 / 20px), 모든 크기·간격을 `rem` 으로 써서 버튼·여백·터치 영역이 비율대로 함께 커진다. 배치가 바뀌어야 하는 곳만 Tailwind 커스텀 변형(`font-lg:grid-cols-2`, `font-sm:grid-cols-4`)으로 따로 지정. 브라우저 확대(핀치 줌)는 막지 않는다.

| | 작게 (15px) | 보통 (17px, 기본) | 크게 (20px) |
|---|---|---|---|
| 대상 | 정보 많이 보고 싶은 사람 | 대부분 | 부모님·노안 |
| 본문 줄간격 | 1.5 | 1.6 | 1.75 |
| 보조 글씨 색 | 회색(neutral-500) | neutral-500 | **진하게**(neutral-700 / 다크 300) — 큰 글씨 쓰는 사람은 대비도 필요 |
| 하단 탭 | 아이콘 + 작은 라벨, 높이 56 | 아이콘 + 라벨, 64 | 아이콘 + **큰 라벨**, 76 |
| 버튼 최소 높이 | 40 | 48 | 56, 전체 폭 |
| 앨범 그리드 | 4열 | 3열 | 2열 |
| 피드 | 사진 4:3, 본문 3줄 미리보기 | 4:3, 3줄 | 사진 1:1 크게, 본문 2줄 + "더보기" 크게 |
| 캘린더 셀 | 약자 + 시작 시각(`야 20:00`) | 큰 약자 + 색 | **약자 1글자 아주 크게** + 색, 시각은 날짜 누를 때만. 칩도 2배 |
| 날짜 표기 | `10.14 (화)` | `10월 14일 화` | `10월 14일 화요일` |
| 시각 | `20:00` | `20:00` | `저녁 8시` (긴 표현이 더 빨리 읽힘) |
| 아이콘 | 아이콘만 있는 버튼 허용 | 허용 | **아이콘 옆에 항상 글자** |

- 태국어는 성조·모음 기호가 위아래로 쌓이므로 같은 단계에서 줄간격 +0.1.
- **고르는 곳**: (1) `나` 화면 맨 위 — 세 칸 짜리 큰 선택 버튼 `가 / 가 / 가`(실제 크기로 그려진 글자) 아래에 미리보기 문장 한 줄이 즉시 바뀜. (2) 로그인 화면 우측 상단 `가가` 버튼(로그인 전에도). (3) **첫 로그인 직후 한 번** "글씨 크기를 골라 주세요" 화면 — 부모님이 설정을 찾아 들어가지 않아도 되게.
- 저장: `member.font_scale`, 로그인 전엔 쿠키. 서버가 `<html>` 에 바로 렌더링하므로 깜빡임 없음.

## 7-2. 쉬운 UI 원칙
- **저장 버튼 없음**: 설정·캘린더·프로필은 바꾸는 순간 저장, `저장됨 ✓` 표시, 실수는 되돌리기 토스트. 저장 버튼은 "글 올리기"·"댓글 달기"처럼 남에게 보이는 행동에만.
- 화면당 주요 버튼 1개. 확인 팝업은 삭제에만(되돌리기 토스트가 있으면 팝업 생략).
- 터치 영역 최소 48px(크게 단계 56px). 글자 대비 WCAG AA 이상, 다크 모드 포함.
- 전문용어 금지: "로그아웃" 대신 "나가기", "업로드" 대신 "올리기". 라벨 작성 시 적용.
- 입력은 가능한 한 탭: 시각은 `<input type="time">`, 날짜는 달력, 색은 8색 팔레트 중 탭.

## 8. 사진
- 브라우저가 줄여서 올린다(청첩장 §15-6 방식, Pi 메모리 보호): 보기용 긴 변 2560px JPEG, 썸네일 600px. 아이폰 HEIC 는 `<input accept="image/*">` 에서 Safari 가 JPEG 로 바꿔 준다.
- **원본도 버킷에 보관**한다(청첩장과 다름). 가족 기록이라 원본이 친구 Pi 디스크에만 있으면 위험하다. 원본은 서빙 경로 없음, 본인·관리자 "원본 받기"만 서명 URL.
- 서버는 보기용·썸네일을 디코딩 후 재인코딩(위장 파일 차단, EXIF·GPS 제거).
- 촬영일: 브라우저에서 JPEG EXIF `DateTimeOriginal` 만 읽는 작은 함수(라이브러리 없음). 없으면 `file.lastModified`.
- 버킷 키: `{family_id}/view/{uuid}.jpg`, `{family_id}/thumb/{uuid}.jpg`, `{family_id}/original/{uuid}.{ext}`. 가족별 접두사라 나중에 가족 단위 이전·삭제가 쉽다.
- 서명 URL 유효 1일, 같은 키는 하루 동안 같은 URL 재사용(브라우저 캐시).
- 한도: 글당 20장, 원본 장당 40MB. 동영상은 백로그.

## 9. 데이터 모델 (`db/migration/V1__init.sql`)
```sql
CREATE TABLE IF NOT EXISTS family (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  name        TEXT NOT NULL,
  created_at  TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);
CREATE TABLE IF NOT EXISTS member (
  id             INTEGER PRIMARY KEY AUTOINCREMENT,
  login_id       TEXT NOT NULL UNIQUE,
  password_hash  TEXT NOT NULL,
  name           TEXT NOT NULL,
  avatar_key     TEXT,
  lang           TEXT NOT NULL DEFAULT 'ko',      -- ko | th
  theme          TEXT NOT NULL DEFAULT 'system',  -- system | light | dark
  font_scale     TEXT NOT NULL DEFAULT 'md',      -- sm | md | lg
  font_scale_set_yn TEXT NOT NULL DEFAULT 'N',    -- 첫 로그인 글씨 크기 선택 화면을 봤는지
  color          TEXT,                            -- 가족 캘린더에서 이 사람 색
  must_change_pw TEXT NOT NULL DEFAULT 'N',
  timezone       TEXT NOT NULL DEFAULT 'Asia/Seoul', -- 태국은 Asia/Bangkok(2시간 차)
  last_seen_at   TEXT,                            -- '새 글' 표시용
  legacy_consent_at TEXT,                         -- 내 기록의 AI 복원 사용 동의 시각. NULL = 미동의(§15-3)
  created_at     TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);
CREATE TABLE IF NOT EXISTS family_member (
  family_id  INTEGER NOT NULL REFERENCES family(id),
  member_id  INTEGER NOT NULL REFERENCES member(id),
  status     TEXT NOT NULL DEFAULT 'PENDING',  -- PENDING | ACTIVE | BLOCKED
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
  PRIMARY KEY (family_id, member_id)
);
CREATE TABLE IF NOT EXISTS role (
  code       TEXT PRIMARY KEY,                 -- ADMIN | MEMBER | VIEWER
  label_key  TEXT NOT NULL                     -- 화면 표시용 라벨 키
);
CREATE TABLE IF NOT EXISTS role_permission (
  role_code  TEXT NOT NULL REFERENCES role(code),
  permission TEXT NOT NULL,                    -- 'post.write' 등(§4-1)
  PRIMARY KEY (role_code, permission)
);
CREATE TABLE IF NOT EXISTS member_role (
  family_id  INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  role_code  TEXT NOT NULL REFERENCES role(code),
  granted_by INTEGER,
  granted_at TEXT NOT NULL,
  PRIMARY KEY (family_id, member_id, role_code)
);
CREATE TABLE IF NOT EXISTS audit_log (         -- 승인·역할 변경·삭제·원본 다운로드·로그인 실패
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id  INTEGER,
  member_id  INTEGER,                          -- 한 사람(로그인 실패면 NULL)
  action     TEXT NOT NULL,                    -- 'member.approve', 'media.original.download', 'login.fail' ...
  target     TEXT,                             -- 'member:3', 'media:120'
  ip         TEXT,
  created_at TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_audit ON audit_log(family_id, created_at DESC);
CREATE TABLE IF NOT EXISTS session (
  token_hash TEXT PRIMARY KEY,                 -- 쿠키에는 원문, DB 에는 SHA-256
  member_id  INTEGER NOT NULL,
  family_id  INTEGER NOT NULL,
  expires_at TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS label (
  key  TEXT NOT NULL,
  lang TEXT NOT NULL,                          -- ko | th
  text TEXT NOT NULL,
  PRIMARY KEY (key, lang)
);
CREATE TABLE IF NOT EXISTS post (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id  INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  kind       TEXT NOT NULL,                    -- moment | diary
  title      TEXT,                             -- diary 만
  body       TEXT NOT NULL DEFAULT '',
  visibility TEXT NOT NULL DEFAULT 'family',   -- family | private(작성자만)
  post_date  TEXT NOT NULL,                    -- 'yyyy-MM-dd', 작성자 시간대 기준 날짜
  created_at TEXT NOT NULL,
  updated_at TEXT,
  deleted_at TEXT,                             -- NULL = 살아 있음
  deleted_by INTEGER
);
CREATE INDEX IF NOT EXISTS ix_post_feed ON post(family_id, post_date DESC, id DESC) WHERE deleted_at IS NULL;
CREATE TABLE IF NOT EXISTS media (            -- 사진·음성·영상 공통. MVP 는 kind='photo' 만 업로드 가능(§15)
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id   INTEGER NOT NULL,
  member_id   INTEGER NOT NULL,                -- 올린 사람
  post_id     INTEGER,                         -- NULL = 올렸지만 아직 글 저장 전(하루 지나면 삭제)
  kind        TEXT NOT NULL,                   -- photo | audio | video
  file        TEXT NOT NULL,                   -- uuid
  original    TEXT NOT NULL,                   -- original 파일명(확장자 포함), 무손실 그대로
  mime        TEXT NOT NULL,
  size_bytes  INTEGER NOT NULL,
  width       INTEGER,                         -- photo·video
  height      INTEGER,
  duration_ms INTEGER,                         -- audio·video
  sha256      TEXT NOT NULL,                   -- 원본 해시. 중복 업로드 방지·무결성 확인
  taken_at    TEXT,                            -- EXIF 촬영일 / 녹음일
  sort        INTEGER NOT NULL DEFAULT 0,
  created_at  TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now'))
);
CREATE INDEX IF NOT EXISTS ix_media_album ON media(family_id, kind, taken_at DESC);
CREATE INDEX IF NOT EXISTS ix_media_sha ON media(family_id, sha256);   -- 같은 사진을 다른 글에 또 올릴 수 있으므로 유니크 아님
CREATE TABLE IF NOT EXISTS shift_type (           -- 한 사람의 교대 종류
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  family_id  INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  name       TEXT NOT NULL,                    -- '야간'
  short      TEXT NOT NULL,                    -- '야' (1~2자)
  color      TEXT NOT NULL,                    -- 팔레트 키 'blue' 등
  start_time TEXT,                             -- 'HH:mm', 휴무·비번은 NULL
  end_time   TEXT,                             -- start 보다 이르면 다음 날
  sort       INTEGER NOT NULL DEFAULT 0,
  deleted_at TEXT                              -- 지워도 과거 기록 표시는 유지
);
CREATE TABLE IF NOT EXISTS shift_setting (        -- 1인 1행
  member_id     INTEGER NOT NULL,
  family_id     INTEGER NOT NULL,
  system        TEXT NOT NULL,                 -- '2' | '3' | '4' | 'custom'
  pattern       TEXT,                          -- JSON 배열 [shift_type.id, ...] 순환
  pattern_start TEXT,                          -- 'yyyy-MM-dd'
  updated_at    TEXT NOT NULL,
  PRIMARY KEY (family_id, member_id)
);
CREATE TABLE IF NOT EXISTS shift_day (            -- 날짜별 실제 근무. 패턴 채우기도 여기에 행으로 쓴다
  family_id     INTEGER NOT NULL,
  member_id     INTEGER NOT NULL,
  day           TEXT NOT NULL,                 -- 'yyyy-MM-dd'
  shift_type_id INTEGER,                       -- NULL = 메모만 있는 날
  note          TEXT,                          -- ≤ 30자
  updated_at    TEXT NOT NULL,
  updated_by    INTEGER NOT NULL,
  PRIMARY KEY (family_id, member_id, day)
);
CREATE INDEX IF NOT EXISTS ix_shift_family_day ON shift_day(family_id, day);
CREATE TABLE IF NOT EXISTS comment (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  post_id    INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  body       TEXT NOT NULL,
  created_at TEXT NOT NULL,
  updated_at TEXT,
  deleted_at TEXT,
  deleted_by INTEGER
);
CREATE TABLE IF NOT EXISTS reaction (
  post_id    INTEGER NOT NULL,
  member_id  INTEGER NOT NULL,
  created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%SZ','now')),
  PRIMARY KEY (post_id, member_id)
);
```
- `comment`·`reaction` 은 `post` 를 거쳐 가족을 확인한다(글의 `family_id` 가 세션과 다르면 404). `private` 글은 작성자가 아니면 404.
- **시각은 전부 UTC ISO-8601**(`2026-10-14T05:00:00Z`)로 저장하고 화면에서 `member.timezone` 으로 바꿔 보여준다. 한국·태국 2시간 차 때문. 예외는 `post_date`(작성자 기준 날짜).
- 삭제는 전부 소프트 삭제(`deleted_at`). 실제 파일 삭제는 관리자가 휴지통에서 30일 뒤.

## 10. API
| Method | Path | 설명 |
|---|---|---|
| POST | `/api/login` · `/api/logout` | `{loginId, password}` |
| POST | `/api/signup` | `{loginId, password, name, lang}` → PENDING |
| PUT | `/api/prefs` | `{lang?, theme?}` 로그인 전엔 쿠키, 후엔 `member` 에 저장 |
| POST | `/api/media?view=&thumb=` | body = 보기용·썸네일·원본 이어붙인 바이트 + `takenAt` → `{id}` |
| POST · PUT · DELETE | `/api/posts` · `/api/posts/{id}` | `{body, postDate, mediaIds:[...]}` |
| GET | `/api/posts?before=` | 피드 더보기 |
| POST · DELETE | `/api/posts/{id}/comments` · `/api/comments/{id}` | `{body}` ≤ 1000자 |
| PUT | `/api/posts/{id}/reaction` | 토글 → `{liked, count}` |
| GET | `/api/media/{id}/original` | 본인·관리자만, 서명 URL 302 |
| PUT | `/api/me` · `/api/me/password` | `/api/me` 는 필드 하나씩 보내도 됨(자동 저장) |
| GET | `/api/shifts?month=2026-10&who=me\|family` | 그 달 근무 + 교대 종류 |
| PUT · DELETE | `/api/shifts/{day}` | `{shiftTypeId?, note?}` 탭 1번 = 요청 1건. 응답에 이전 값(되돌리기용) |
| POST | `/api/shifts/fill` | `{from, months}` 패턴으로 채우기 → 덮어쓴 날들의 이전 값 `{previous:[...]}` |
| PUT | `/api/shifts` | `{days:[{day, shiftTypeId, note}]}` 여러 날 한 번에. 패턴 되돌리기용 |
| GET · PUT | `/api/shift-setting` | `{system, pattern, patternStart}` |
| POST · PUT · DELETE | `/api/shift-types` · `/api/shift-types/{id}` | |
| GET · PUT | `/admin/api/members` · `/admin/api/members/{id}` | `{status?, role?}` 승인·차단 |
| POST | `/admin/api/members/{id}/reset-password` | → `{tempPassword}` |

## 11. 보안
- 로그인 인터셉터 1개: `/login`, `/signup`, `/api/login`, `/api/signup`, `/api/prefs`, 정적 파일 외 전부 `ACTIVE` 세션 필수. 없으면 페이지는 `/login`, API 는 401. `PENDING` 은 대기 화면만. `must_change_pw` 면 비밀번호 변경 화면만.
- 가족 격리: §3 규칙. 리뷰 때 `family_id` 없는 쿼리를 1순위로 본다.
- 출력은 `th:text`·`textContent` 만. 입력은 화이트리스트 검증.
- `robots.txt` 전체 차단 + `X-Robots-Tag: noindex`. 카톡 미리보기는 사이트 이름만(사진 안 나감).
- 쓰기 요청 Origin 검사(`OriginInterceptor`), 보안 헤더 필터. 클라이언트 IP 는 `CF-Connecting-IP` 만 신뢰.
- 비밀값(`NCLOUD_*`, `ADMIN_LOGIN_ID`, `ADMIN_PW_HASH`)은 `.env`. 커밋 금지.

## 12. 백업
- 매일 04:00 `VACUUM INTO` 로 DB 스냅샷 → 버킷 `backup/family-YYYYMMDD.db`, 30개 보관. 앱 안 `@Scheduled` 하나.
- 사진은 원본까지 버킷에 있으므로 DB 백업만 있으면 전체 복구된다.

## 13. 배포
청첩장 §11 과 같다. 차이만:
- Pi 경로 `~/workspace/huiyeong/D-Day/family`, 컨테이너 `family-app`, `mem_limit: 512m`.
- ingress: `- hostname: family.thaiuniontour.com` / `service: http://family-app:8080` 를 `thaiuniontour/cloudflared/config.yml` 맨 앞에, `route dns` 로 DNS 등록.
- 버킷 `family`, 키는 청첩장과 같은 계정 키(`NCLOUD_*`).

## 14. 백로그 (요청 시 추가)
- **글·댓글 번역 버튼**(한↔태). 파파고 또는 Claude API, 결과 캐시 테이블. 사용자가 쓴 글을 여자친구가 바로 읽게.
- 가족 일정(생일·기일·여행)을 근무 캘린더 화면에 함께, 생일 D-Day 배너
- 근무 알림(전날 저녁 "내일 야간")
- "N년 전 오늘" 카드
- 앨범 태그·폴더(예: 2026 제주여행)
- 새 글 알림(웹푸시. iOS 는 홈 화면 추가 필요), 마지막 접속 이후 새 글 표시
- 동영상
- 2번째 가족: 가족 생성·선택 UI, 가입 시 가족 선택
- 카카오·라인 로그인
- 영어(en) 라벨
- 역할·권한 편집 화면, 감사 로그 화면
- 일기 기분·날씨 태그, 일기 검색
- 기록 보존·AI 복원 단계는 §15

## 15. 장기 비전: 기록 보존과 AI 복원
가족 중 누군가 세상을 떠나도 사진·목소리·글로 그 사람과 다시 대화할 수 있게 하는 것. AI 기술은 몇 년 안에 크게 바뀌므로 **지금 특정 모델에 묶지 않는다.** 대신 나중에 어떤 기술이 나와도 쓸 수 있게 **원본 데이터를 잘, 많이, 맥락과 함께** 모아 두는 것이 이 단계의 전부다. 데이터는 그 사람이 살아 있을 때만 모을 수 있다.

### 15-1. 지금(MVP)에 넣어 둔 것
- `media` 테이블을 사진 전용이 아니라 사진·음성·영상 공통으로 설계. 음성 업로드는 2단계지만 테이블은 그대로 쓴다.
- 원본 무손실 보관(버킷 `{family_id}/original/`) + `sha256`. 화면용 리사이즈본만 보여주고 원본은 손대지 않는다.
- 모든 기록에 올린 사람(`member_id`)·날짜(`taken_at`).
- 글·댓글 원문 그대로 보관(삭제도 `deleted_at` 소프트 삭제). 말투·성격 학습 재료.
- `member.legacy_consent_at` 동의 필드(§15-3).

### 15-2. 단계
| 단계 | 내용 | 비고 |
|---|---|---|
| 1. MVP | 사진·일기·댓글 | 이 문서 §5 |
| 2. 목소리 모으기 | 통화녹음 업로드(m4a·mp3·amr·wav, 원본 보관), **"목소리 남기기"**: 질문 카드("어린 시절 살던 집은?")에 답하며 브라우저(`MediaRecorder`)로 녹음, 영상 업로드 | 통화녹음은 8kHz·두 사람 목소리가 섞여 음성 복제 품질이 낮다. 조용한 곳에서 혼자 이야기한 녹음이 훨씬 중요. 1인당 누적 1시간 이상이 목표 |
| 3. 사람 단위 정리 | `person` 테이블(로그인 안 하는 할머니·할아버지도 포함, 생몰일), 사진·녹음에 "누가 나오는지/말하는지" 태그, 카카오톡 대화 내보내기(txt) 가져오기 | 화자 분리·얼굴 인식으로 자동 태깅 가능 |
| 4. 전사·색인 | 녹음 → 텍스트(Whisper 계열 STT), 화자 분리, 사람별 타임라인 | 외부 GPU/API. Pi 에서 돌리지 않음 |
| 5. 내보내기 | 사람 1명의 모든 기록을 표준 포맷 zip(JSON + 원본 파일)으로 | 서비스·회사가 바뀌어도 데이터가 남게 |
| 6. AI 복원 | 음성 복제(TTS) + 그 사람의 글·전사본으로 만든 대화 모델(RAG) + 실시간 음성 통화 | 그때 가장 좋은 모델로. 결과물은 가족 안에서만 |

### 15-3. 동의와 원칙
- 복원 대상 본인의 **생전 동의**(`legacy_consent_at`)가 있는 사람만 6단계 대상. `/me` 에서 언제든 동의·철회. 동의 안 한 사람의 기록도 보관·열람은 그대로 된다(복원에만 안 쓴다).
- 통화녹음에는 상대방 목소리도 들어 있다. 가족 밖 사람이 나오는 녹음은 올리지 않도록 업로드 화면에 안내.
- 목소리 데이터는 보이스피싱에 악용될 수 있는 가장 민감한 자료다. 음성·영상 원본은 어떤 공유 링크로도 나가지 않고, 다운로드는 관리자만, 다운로드 기록을 남긴다.
- 복원된 AI 는 "그 사람"이 아니라 기록을 바탕으로 만든 재현이라는 표시를 화면에 항상 둔다.

## 16. 구현 메모 (2026-10-14, 1차 구현)
- 라벨은 `R__labels.sql`(Flyway 반복 마이그레이션, `INSERT OR REPLACE`). **파일이 기준**이라 파일을 바꾸면 재배포 때 sqlite 로 고친 값도 덮어쓴다. 태국어 검수 필요.
- `media.sha256` 은 유니크가 아니다(같은 사진을 다른 글에 또 올릴 수 있게). 한 업로드는 한 글에만 붙는다.
- 로컬 모드(Ncloud 키 없음) 파일은 `data/store/{key}`, `/files/{family}/{view|thumb|original}/..` 로 로그인한 같은 가족에게만.
- 근무 프리셋 이름은 설정하는 사람의 언어로 만든다(태국어 사용자는 กะเช้า 등). 이후엔 사용자 데이터라 번역 안 함.
- 근무 칩(붓)은 달력 위에 고정(아래 탭·되돌리기 토스트와 겹치지 않게).
- 지우기·차단·거절·초기화는 팝업 대신 "한 번 더 누르면…" 두 번 누르기.
- 프로필 사진(`avatar_key`)은 아직 안 씀. 이름 첫 글자 + 내 색 동그라미.
- 점검: `scripts/smoke.sh`(API 전체 흐름).
