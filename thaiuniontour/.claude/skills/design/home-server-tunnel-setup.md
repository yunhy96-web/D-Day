# 홈서버(친구 Pi) Cloudflare Tunnel 배포 연습 가이드

> 목표: 친구 라즈베리파이에서 **내 도메인으로 "Welcome to nginx" 페이지가 열리는 것**까지.
> 이게 성공하면 도메인 → Cloudflare → 터널 → 컨테이너 전체 경로가 검증된 것이고,
> 이후 진짜 서비스는 이미지 이름만 바꾸면 된다.
> 예상 소요: 30분~1시간. 실패해도 잃을 것 없음 (공개된 서비스 없음, 친구 인프라 안 건드림).

---

## 0. 사전 정보

| 항목 | 값 |
|---|---|
| 서버 | Raspberry Pi 5 (8GB, ARM64/aarch64), Debian 12 |
| 접속 | `ssh seonghanim@125.240.159.13` (비밀번호는 아는 것 사용) |
| 내 작업 디렉토리 | `~/workspace/huiyeong/` (생성 완료) |
| 필요한 것 | 내가 사둔 도메인, Cloudflare 무료 계정 |

**주의사항 (친구 서버 예의)**
- 친구 것 절대 건드리지 않기: `~/workspace/bloomi`, `backend`, `frontend`, `db`, `jenkins`, 실행 중인 컨테이너 4개(bloomi-*, chatdb)
- 공유기 설정 건드릴 일 없음 (Tunnel은 인바운드 포트 불필요)
- 80/443/8080 등 포트 바인딩 안 함 → 충돌 가능성 0

**아키텍처 (완성 후 모습)**
```
방문자 → https://내도메인.com
       → Cloudflare (DNS + TLS + CDN)
       ⇦ Pi의 cloudflared가 미리 걸어둔 아웃바운드 연결
       → 같은 도커 네트워크의 nginx 컨테이너 (외부 포트 노출 없음)
```

---

## 1단계: Cloudflare에 도메인 등록

1. https://dash.cloudflare.com 가입 (Free 플랜)
2. **Add a Site** → 내 도메인 입력 → Free 선택
3. Cloudflare가 알려주는 **네임서버 2개**를 복사
4. 도메인 구입처(가비아/후이즈/GoDaddy 등) 관리 페이지에서 네임서버를 그 2개로 변경
5. 전파 대기 (수 분~수 시간). Cloudflare 대시보드에 "Active" 뜨면 완료

> DNS 레코드(A레코드 등)는 **직접 만들 필요 없음** — 3단계에서 터널이 자동 생성해줌.

## 2단계: Tunnel 생성 + 토큰 받기

1. Cloudflare 대시보드 → **Zero Trust** (좌측 메뉴) → **Networks → Tunnels**
2. **Create a tunnel** → Cloudflared 선택 → 이름: `huiyeong-pi` (아무거나)
3. 설치 안내 화면에서 Docker 탭의 명령에 들어있는 **토큰**(`eyJ...` 긴 문자열)만 복사
   - 명령 전체 실행할 필요 없음. 토큰만 필요.
4. 화면은 그대로 두거나 Next로 진행 (Public Hostname은 4단계에서 등록)

## 3단계: Pi에 연습용 컴포즈 작성

SSH 접속 후:

```bash
mkdir -p ~/workspace/huiyeong/hello
cd ~/workspace/huiyeong/hello
vi docker-compose.yml
```

내용:

```yaml
services:
  web:
    image: nginx:alpine
    restart: always
    # ports 없음! 외부 노출 없이 터널로만 접근

  cloudflared:
    image: cloudflare/cloudflared:latest
    command: tunnel run
    environment:
      - TUNNEL_TOKEN=여기에_2단계_토큰_붙여넣기
    restart: always
```

실행:

```bash
docker compose up -d
docker compose ps          # 둘 다 Up 인지 확인
docker compose logs cloudflared | tail -20
# "Registered tunnel connection" 이 보이면 터널 연결 성공
```

> 참고: nginx:alpine은 arm64 지원 이미지라 Pi에서 그대로 동작. 앞으로 쓸 이미지도 arm64 지원 여부 확인 습관 들이기.

## 4단계: 도메인 → 컨테이너 연결 (Public Hostname)

1. Zero Trust → Networks → Tunnels → `huiyeong-pi` 선택 → **Public Hostname** 탭 → Add
2. 입력:
   - **Subdomain**: 비움 (또는 `www`)
   - **Domain**: 내 도메인 선택
   - **Service**: Type `HTTP`, URL `web:80`
     - `web` = 컴포즈의 서비스 이름. cloudflared와 같은 컴포즈(=같은 도커 네트워크)라 이름으로 통신됨
3. Save → Cloudflare가 DNS 레코드 자동 생성

## 5단계: 확인

- 브라우저(폰 LTE로 하면 외부망 테스트까지 됨)에서 `https://내도메인.com` 접속
- **"Welcome to nginx!"** 페이지가 뜨면 성공 🎉
- https 자물쇠 확인 (인증서는 Cloudflare가 자동 처리한 것)

---

## 트러블슈팅

| 증상 | 확인할 것 |
|---|---|
| 도메인 접속 시 에러 502 | `docker compose ps`에서 web이 Up인지, Public Hostname의 Service가 `web:80` 맞는지 |
| 에러 1033 (Tunnel error) | cloudflared 로그 확인. 토큰 오타 가능성. `docker compose logs cloudflared` |
| 도메인이 아예 안 열림 | 1단계 네임서버 전파 미완. Cloudflare 대시보드가 Active인지 확인 |
| cloudflared 재시작 반복 | 토큰이 잘렸는지 확인 (매우 긴 문자열임, 줄바꿈 없이 한 줄) |
| 접속은 되는데 느림 | 정상. 집 회선 + 터널 경유라 첫 접속은 느릴 수 있음. CDN 캐시되면 개선 |

문제 원인 분리 순서 (블로그 교훈과 동일):
`컨테이너(docker compose ps) → 터널 연결(cloudflared 로그) → DNS(대시보드 Active) → 브라우저`

## 정리(원상복구)하고 싶을 때

```bash
cd ~/workspace/huiyeong/hello && docker compose down
```
+ Zero Trust 대시보드에서 터널 삭제. 흔적 없이 끝.

---

## 다음 단계 (연습 성공 후)

1. **진짜 앱 구조로 확장**: web 자리에 프론트 컨테이너, 백엔드/DB 추가
   ```yaml
   services:
     frontend:   # 내 프론트 이미지
     backend:    # 내 백엔드 이미지 (스프링부트 등)
     db:
       image: postgres:16-alpine
       volumes: [dbdata:/var/lib/postgresql/data]
       # ports 절대 노출 금지 — 도커 내부 네트워크로만
     cloudflared: # 그대로
   ```
   - Public Hostname 라우팅: `/` → `frontend:포트`, path `api` → `backend:포트` (경로 기반이라 CORS 없음)
2. **DB 백업 습관**: `docker exec db pg_dump ...`를 cron으로 → 내 로컬로 주기적 복사
3. **쇼핑몰 등 실전 오픈 시점**: 집서버 ✗ → 작은 VPS(월 5천~1만원)로 이사.
   컴포즈 파일 그대로 들고 가면 `docker compose up` 한 번으로 이사 끝.

## 배경 지식 요약 (까먹었을 때)

- **포트 = IP라는 건물의 문 번호.** 80=http, 443=https, 22=ssh. 포트마다 듣는 프로그램이 다름
- **공유기 포트포워딩**: 밖→안 인바운드를 특정 기기로 넘기는 규칙 (친구가 22/80/443 이미 설정해둠. 우리는 안 씀)
- **Tunnel 원리**: 공유기는 안→밖 연결은 허용함. cloudflared가 Cloudflare로 먼저 연결을 걸어 유지 → 방문자 요청이 그 연결을 타고 거꾸로 들어옴 → 인바운드 포트 개방 불필요
- **컨테이너 간 통신**: 같은 컴포즈 안에서는 서비스 이름이 곧 호스트명 (`web:80`, `db:5432`)
- **localhost 함정**: 컨테이너 안에서 localhost는 그 컨테이너 자신. 다른 컨테이너는 반드시 서비스 이름으로
