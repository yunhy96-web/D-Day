# CLAUDE.md — wedding

모바일 청첩장. 설계는 `docs/DESIGN.md` 가 단일 기준. 구현 중 설계와 달라지면 문서부터 고친다.

## 규칙
- 단일 Gradle 모듈, Spring Boot 3 / Java 17. 멀티모듈·JPA·Redis·Postgres 추가 금지(설계 §3 근거).
- DB 는 SQLite + JdbcTemplate + `schema.sql`. 테이블은 `visit`, `pick`, `guestbook`, `label`, `content`, `event`. 새 컨텐츠는 자기 테이블만 추가.
- 화면 문구는 전부 `label` 테이블(ko·en·th) 키로만 참조. 템플릿·JS 에 한국어 하드코딩 금지. 번역 불필요한 값(일시·위경도·계좌)만 `application.yml` 의 `wedding.*`.
- 다국어는 `data-i18n` + 임베드 JSON 방식(설계 §6-2). Spring MessageSource·i18n 라이브러리 추가 금지.
- 사진은 `/data/photos/{nn}.webp` 파일. 업로드 API·어드민 만들지 않는다.
- 프론트는 Thymeleaf 템플릿 1장 + Tailwind CDN + vanilla JS. 프론트 빌드 도구 추가 금지.
- 배포 대상은 arm64(Pi). 빌드는 Pi 에서 `docker-compose`(v1) 로.
- 비밀값(`KAKAO_JS_KEY`, `GUESTBOOK_MASTER_PW`)은 `.env` / 환경변수. 커밋 금지.

- 컨텐츠는 `/c/{key}` 라우트 + `content/<key>/` 패키지 + `templates/c/<key>.html` + `content` 테이블 1행(설계 §4-3). 컨텐츠끼리 서로의 테이블·서비스 참조 금지.
- 출력은 `th:text`·`textContent` 만. `th:utext`·`innerHTML` 금지. 모든 `/api/**` 입력은 화이트리스트 검증(설계 §13-2). 클라이언트 IP 는 `CF-Connecting-IP` 만 신뢰.
- 계좌번호·상세주소는 HTML 에 서버 렌더링하지 않는다(복사 버튼 → API).
- Spring Security 추가 금지. 보안 헤더 필터 1개 + Origin 인터셉터 1개로 처리.

## 실행
```bash
./gradlew bootRun     # http://localhost:8080
```
