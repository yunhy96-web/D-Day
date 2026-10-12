# CLAUDE.md — wedding

모바일 청첩장. 설계는 `docs/DESIGN.md` 가 단일 기준. 구현 중 설계와 달라지면 문서부터 고친다.

## 규칙
- 단일 Gradle 모듈, Spring Boot 3 / Java 17. 멀티모듈·JPA·Redis·Postgres 추가 금지(설계 §3 근거).
- DB 는 SQLite + JdbcTemplate + `schema.sql`. 테이블은 `visit`, `pick`, `guestbook`, `label`, `content`, `event`, `quiz`, `quiz_attempt`, `timeline`, `photo`, `guestbook_photo`. 새 컨텐츠는 자기 테이블만 추가.
- 디자인 규칙: 제목은 MaruBuri 세리프를 크게, 자간 벌리지 않음. 글자 상자(카드)·테두리·그림자 금지, 여백과 글자 크기로 구분. 페이지당 채움 버튼 최대 1개, 나머지는 텍스트 링크. 문구는 짧고 담백하게("~이에요" 금지).
- 화면 문구는 전부 `label` 테이블(ko·en·th) 키로만 참조. 템플릿·JS 에 한국어 하드코딩 금지. 번역 불필요한 값(일시·위경도·계좌)만 `application.yml` 의 `wedding.*`.
- 다국어는 `data-i18n` + 임베드 JSON 방식(설계 §6-2). Spring MessageSource·i18n 라이브러리 추가 금지.
- 사진은 `photo` 테이블 + 어드민 업로드(설계 §15-6). 원본은 비공개, 하객에겐 2560px 보기용·600px 썸네일만. 하객 업로드(방명록)는 서버에서 반드시 재인코딩.
- 어드민(`/admin`)은 설계 §15 범위만. 어드민 화면은 한국어 하드코딩 허용.
- 퀴즈 정답(`answer_no`)은 어떤 응답에도 넣지 않는다. 채점은 서버에서 응시 1회 단위로.
- 프론트는 Thymeleaf 템플릿 1장 + Tailwind CDN + vanilla JS. 프론트 빌드 도구 추가 금지.
- 배포 대상은 arm64(Pi). 빌드는 Pi 에서 `docker-compose`(v1) 로.
- 사진 파일은 `ObjectStore` 로만 읽고 쓴다(Ncloud Storage 서명 URL, 키 없으면 로컬). 원본은 버킷에 올리지 않는다(설계 §15-10).
- 비밀값(`KAKAO_JS_KEY`, `GUESTBOOK_MASTER_PW_HASH`, `ADMIN_PW_HASH`, `NCLOUD_*`)은 `.env` / 환경변수. 커밋 금지.

- 컨텐츠는 `/c/{key}` 라우트 + `content/<key>/` 패키지 + `templates/c/<key>.html` + `content` 테이블 1행(설계 §4-3). 컨텐츠끼리 서로의 테이블·서비스 참조 금지.
- 출력은 `th:text`·`textContent` 만. `th:utext`·`innerHTML` 금지. 모든 `/api/**` 입력은 화이트리스트 검증(설계 §13-2). 클라이언트 IP 는 `CF-Connecting-IP` 만 신뢰.
- Spring Security 추가 금지. 보안 헤더 필터 1개 + Origin 인터셉터 1개 + 어드민 세션 인터셉터 1개로 처리.

## 실행
```bash
./gradlew bootRun     # http://localhost:8080
```
