# DEV STUDIO 배포 (Lightsail + Cloudflare)

- 서버: 고객 AWS 계정 865480029677 / Lightsail `devstudio-web` (서울, nano $5) / 고정 IP `52.78.193.240`
- 사이트 경로: `/var/www/devstudio` (nginx, 설정 `/etc/nginx/sites-enabled/devstudio.conf`)
- 도메인: Cloudflare → A `devstudio.ai.kr` = 52.78.193.240 (프록시 ON, SSL Full) — 서버 443은 자체서명 인증서(/etc/ssl/certs/devstudio.crt, 10년)

## 배포
```bash
npm run deploy   # 빌드 + rsync
```

## 다른 컴퓨터에서 처음 한 번
1. 코드 받기
2. 이 맥의 `~/.ssh/devstudio-lightsail` 키 파일을 같은 경로로 복사 (`chmod 600`)
3. `~/.ssh/config` 에 추가
```
Host devstudio-lightsail
  HostName 52.78.193.240
  User ubuntu
  IdentityFile ~/.ssh/devstudio-lightsail
  IdentitiesOnly yes
```
4. `npm install` 후 `npm run deploy`

AWS 액세스 키는 배포에 필요 없음 (서버 생성·변경할 때만).

## 비용 안전장치 (예산 "비용 에산" 월 $10)
- 50%·80%: 메일 알림 (devstudio25@naver.com)
- 100% 초과 시 자동:
  1. SNS `devstudio-budget-stop` → Lambda `devstudio-stop-lightsail` → Lightsail `devstudio-web` 중지 (사이트 내려감)
  2. 예산 액션 → IAM 사용자 `devstudio-dev` 에 `AWSDenyAll` 부착 (CLI·콘솔 모두 차단)
- 복구: 루트 계정 로그인 → IAM → devstudio-dev → 권한에서 AWSDenyAll 제거 → Lightsail 콘솔에서 인스턴스 시작 (고정 IP 유지)

## 방화벽 (Lightsail)
- 80·443: Cloudflare IP 대역만 허용 (https://www.cloudflare.com/ips-v4 , ips-v6). IP 직접 접속은 차단
- 22(SSH): 전체 허용, 키 인증만
- Cloudflare IP 대역이 바뀌면 Lightsail 콘솔 → 네트워킹 → IPv4/IPv6 방화벽 규칙의 80·443 허용 대역을 갱신
