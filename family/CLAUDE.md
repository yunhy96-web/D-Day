# CLAUDE.md — family

가족 홈페이지(사진·일기·근무 캘린더). 설계는 `docs/DESIGN.md` 가 단일 기준. 구현 중 설계와 달라지면 문서부터 고친다.

## 규칙
- 단일 Gradle 모듈, Spring Boot 3 / Java 17. JPA·Spring Security·Redis·프론트 빌드 도구 추가 금지(설계 §2).
- DB 는 SQLite + JdbcTemplate + **Flyway**. `V1__init.sql` 은 고치지 않는다. 스키마 변경은 `V2__설명.sql` 추가. 화면 문구는 `R__labels.sql`(ko·th, INSERT OR REPLACE).
- **가족 격리**: 모든 쿼리는 세션의 `family_id`(`Me.familyId()`)로 거른다. 요청으로 받은 family_id 는 믿지 않는다. 나만 보기 글은 작성자 외엔 404.
- 권한은 `Me.require(Perm.X)` / `me.has(..)`. 새 권한 = `Perm` 상수 + `role_permission` 행(V2..). ADMIN 은 '*'.
- 컨트롤러는 `Me me` 파라미터로 로그인 사용자를 받는다(AuthInterceptor 가 상태별 접근 제한).
- 화면 문구는 템플릿·JS 에 하드코딩 금지. 템플릿 `${L['key']}`, JS `App.t('key')`. ko·th 둘 다 넣는다.
- 출력은 `th:text`·`textContent`. JS 로 HTML 문자열을 만들면 사용자 값은 반드시 `esc()`.
- 색은 CSS 변수 토큰(`bg card fg muted line soft accent`)만. 다크 모드는 변수만 바뀐다. 글씨 크기별 배치는 `font-sm:` / `font-lg:` 변형.
- 설정·캘린더는 저장 버튼 없이 즉시 저장 + `App.saved()` / 되돌리기 `App.toast(msg, undo)`.
- 템플릿·JS 클래스를 바꾸면 `scripts/tailwind.sh` 실행 후 `static/tw.css` 커밋.
- 시각은 UTC ISO 저장(`Times.now()`), 표시는 `Fmt`(서버)·`App.fmtTime`(JS).
- 비밀값은 `.env`(커밋 금지). 예시는 `.env.example`.

## 실행·점검
```bash
./gradlew bootJar && set -a && . ./.env && set +a && SERVER_PORT=8091 java -jar build/libs/family-0.1.0.jar   # http://localhost:8091
ADMIN_PW='...' scripts/smoke.sh          # 빈 DB 에서 전체 흐름 점검
```
개발 중 템플릿 즉시 반영: `--spring.thymeleaf.prefix=file:./src/main/resources/templates/ --spring.web.resources.static-locations=file:./src/main/resources/static/` + `THYMELEAF_CACHE=false`.
