# CLAUDE.md — sunfix

집수리 가격비교 Sunfix(운영 컨셉: 엄마 sunflower). 설계는 `docs/DESIGN.md` 가 단일 기준. 구현 중 설계와 달라지면 문서부터 고친다.

## 규칙
- 단일 Gradle 모듈, Spring Boot 3 / Java 17. JPA·Spring Security·Redis·프론트 빌드 도구·지도 SDK 추가 금지(설계 §2).
- DB 는 SQLite + JdbcTemplate + Flyway. **2026-10-25 운영 배포됨**: 기존 `V*` 는 절대 고치지 않고 스키마 변경은 새 `V{n}__설명.sql`. 스키마 변경은 `V3__설명.sql` 추가.
- 카테고리·옵션·지역은 `common_code` 하나(설계 §4). 카테고리는 트리, 가격·제보·문의는 리프(`CodeService.leaf`)만. 표시는 경로 `${f.path(code)}`. 코드 이름은 라벨 `code.{코드}`. 코드는 지우지 않고 `use_yn='N'`. 관리자 저장 후 `CodeService.invalidate()`.
- 화면 문구는 템플릿·JS 하드코딩 금지. 템플릿 `${L['key']}`·`${f.t('key', arg)}`·코드명 `${f.n(code)}`·금액 `${f.won(n)}`, JS `App.t('key')`. 새 키는 `R__labels.sql` 에 ko·en 둘 다(INSERT OR IGNORE 라 기존 키 문구 변경은 관리자 라벨 화면).
- 가격은 서버에서만 계산(`PriceService`). 문의 금액도 클라이언트 값을 믿지 않는다.
- 직접 입력한 제보 업체명(`quote_report.vendor_name`)은 관리자 화면 외에 절대 노출하지 않는다.
- 출력은 `th:text`·`textContent`. `th:utext` 는 `Ljson` 과 관리자만 고치는 `home.title` 뿐.
- 색은 CSS 변수 토큰(`bg card fg muted line soft accent leaf`)만. 템플릿·JS 클래스를 바꾸면 `scripts/tailwind.sh` 후 `static/tw.css` 커밋.
- `th:each` 와 `th:replace` 를 한 요소에 같이 쓰지 않는다(replace 가 먼저 돈다). `<th:block th:each>` 로 감싼다.
- 비밀값은 `.env`(커밋 금지). 예시는 `.env.example`.

## 실행·점검
```bash
./gradlew bootJar && set -a && . ./.env && set +a && SERVER_PORT=8092 java -jar build/libs/sunfix-0.1.0.jar   # http://localhost:8092, 관리자 /admin (sunflower)
./gradlew test        # 가격 합산·선택 검증 (메모리 DB + 예시 데이터)
```
예시 데이터를 처음 상태로: 앱을 끄고 `rm -rf data` 후 재기동.

## 배포 (친구 Pi)
https://sunfix.union-selim.com — Pi `~/workspace/huiyeong/D-Day/sunfix`, 컨테이너 `sunfix-app`, `tut-cloudflared` ingress.
재배포: Pi 에서 `git pull && docker-compose up -d --build`. 다른 존 도메인 CNAME 은 `cloudflared tunnel route dns` 가 thaiuniontour.com 에 붙여 버리므로 대시보드에서 수동.
