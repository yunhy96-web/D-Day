# wedding — 모바일 청첩장

Spring Boot 단일 앱 + Thymeleaf. 친구 Raspberry Pi 5 의 기존 Cloudflare Tunnel 에 서브도메인(`wedding.thaiuniontour.com`)으로 올린다.

- 설계: [docs/DESIGN.md](docs/DESIGN.md)
- 특징: 사진 이상형 월드컵(32강) · 실시간 접속자 수 · 카카오맵 오시는 길
- 스택: Java 17 · Spring Boot 3 · SQLite · Tailwind(CDN) · vanilla JS · Docker

상태: **1차 구현 완료, 로컬 동작 확인** (2026-10-10). 실제 사진·예식 정보·키 입력 전.

## 실행
```bash
scripts/placeholder-photos.sh      # 더미 사진 40장 (실제 사진은 scripts/convert.sh)
./gradlew bootRun                  # http://localhost:8080  (8080 사용 중이면 SERVER_PORT=8090)
scripts/smoke.sh                   # API·보안 헤더·i18n 회귀 체크
```

## 구조
- `src/main/java/com/dday/wedding/` — `page`(화면) · `content`(허브) · `worldcup` · `guestbook` · `presence`(접속자) · `event` · `info`(계좌/주소) · `label`(다국어) · `photo` · `security`
- `src/main/resources/` — `schema.sql`(테이블 6개) · `data.sql`(라벨 ko/en/th 102키 + 허브 카드) · `templates/` · `static/`
- `data/` — SQLite(`wedding.db`) + `photos/`. git 제외.
