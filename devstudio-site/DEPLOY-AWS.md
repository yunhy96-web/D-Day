# DEV STUDIO 정식 배포 가이드 (AWS S3 + CloudFront)

- 진행일: **2026-10-24 (토), 의뢰자와 대면**
- 목표: 친구 라즈베리파이(임시) → **의뢰자 명의 AWS 계정**으로 이전
- 결과 구조: `devstudio.ai.kr` → CloudFront(서울 엣지) → S3(비공개 버킷)
- 예상 소요: 약 1시간 ~ 1시간 30분 (인증서·CloudFront 반영 대기 포함)

---

## 0. 원칙 (꼭 지킬 것)

1. **의뢰자 루트 계정 비밀번호는 받지 않는다.** 인프라 설정은 의뢰자가 로그인한 화면에서 같이 하고, 나는 **배포 전용 IAM 사용자**만 받는다.
2. 결제는 **의뢰자 카드**. 사이트·계정 소유자는 의뢰자.
3. 내 맥에는 회사 AWS 프로필(`default`, `athometrip`, `shared-athometrip`)이 있다. 이 사이트는 **반드시 `--profile devstudio`로만** 다룬다. `scripts/deploy-aws.sh`가 계정 ID를 확인한 뒤에 배포한다.
4. 라즈베리파이는 **AWS 정상 확인 후에 정리**한다. 그 전까지는 문제가 생기면 DNS만 되돌려서 복구할 수 있다.

---

## 1. 토요일 전에 준비 (나)

- [x] 404 페이지 (`src/pages/404.astro`) — CloudFront 오류 응답에 사용
- [x] 배포 스크립트 (`scripts/deploy-aws.sh`) — 버킷·배포 ID·계정 ID만 채우면 됨
- [x] `npm run build` 정상 확인 (10-22)
- [ ] 노트북 충전, 이 문서 열어두기
- [ ] 의뢰자에게 미리 안내: **본인 휴대폰, 결제용 카드, 이메일 계정 로그인 가능 상태**로 오기

---

## 2. 의뢰자 AWS 계정 만들기 (의뢰자 진행, 약 15분)

1. https://aws.amazon.com → **AWS 계정 생성**
   - 이메일: 의뢰자 대표 이메일 (예: devstudio25@naver.com)
   - 계정 이름: `devstudio`
   - 결제 정보: 의뢰자 카드 (해외결제 가능 카드, 확인용 소액 결제 후 취소됨)
   - 본인 확인: 의뢰자 휴대폰 SMS
   - 지원 플랜: **Basic(무료)**
2. 로그인 후 **루트 계정 MFA 설정**
   - 우측 상단 계정명 → 보안 자격 증명 → MFA 할당 → 의뢰자 휴대폰 인증 앱(Google Authenticator 등)
3. 콘솔 우측 상단 리전을 **아시아 태평양(서울) ap-northeast-2**로 선택

## 3. 결제 알림 (의뢰자 계정에서, 약 5분)

1. 결제 및 비용 관리 → **Budgets → 예산 생성**
2. 템플릿: **월별 비용 예산**, 금액 **$5**, 알림 이메일: 의뢰자 이메일 (+ 원하면 내 이메일)
3. (선택) 결제 기본 설정 → **프리 티어 사용 알림** 켜기

> 이 사이트 규모(약 50MB, 국내 방문)면 CloudFront 무료 한도(월 1TB 전송 / 1천만 요청) 안에서 거의 0원이 예상된다. 무료 한도는 바뀔 수 있으니 당일 요금 페이지에서 한 번 확인.

---

## 4. HTTPS 인증서 (ACM) — ⚠️ 리전: 미국 동부(버지니아 북부) us-east-1

CloudFront용 인증서는 **반드시 us-east-1**에서 만들어야 한다.

1. 리전을 **us-east-1**로 바꾼 뒤 → Certificate Manager → **인증서 요청 → 퍼블릭 인증서**
2. 도메인 이름: `devstudio.ai.kr`, `www.devstudio.ai.kr` (2개)
3. 검증 방법: **DNS 검증**
4. 생성 후 표시되는 **CNAME 이름/값 2쌍**을 내 Cloudflare에 추가
   - Cloudflare → `devstudio.ai.kr` → DNS → Add record
   - Type `CNAME`, Name은 `_xxxx.devstudio.ai.kr`에서 **도메인 부분을 뺀 앞부분**, Target은 ACM이 준 값
   - **Proxy: 끄기 (회색 구름, DNS only)**
5. 상태가 **발급됨(Issued)** 이 될 때까지 대기 (보통 5~30분). 기다리는 동안 5·6단계 진행

## 5. S3 버킷 (서울 리전)

1. 리전 **서울(ap-northeast-2)** → S3 → **버킷 만들기**
   - 이름: `devstudio-ai-kr` (이미 있으면 `devstudio-ai-kr-site` 등)
   - **모든 퍼블릭 액세스 차단: 켬** (CloudFront를 통해서만 열람)
   - 정적 웹사이트 호스팅: **사용하지 않음**
   - 나머지 기본값
2. 버킷 이름 메모 → `scripts/deploy-aws.sh`의 `BUCKET`

## 6. CloudFront 함수 (주소 정리용)

Astro는 `/work` 페이지를 `work/index.html`로 만들기 때문에 주소를 변환해 줘야 한다. `www` → 기본 주소 이동도 여기서 처리한다.

CloudFront → **함수 → 함수 생성** → 이름 `devstudio-rewrite` → 아래 코드 → 저장 → **게시(Publish)**

```js
function handler(event) {
  var req = event.request;
  var host = req.headers.host ? req.headers.host.value : '';
  if (host === 'www.devstudio.ai.kr') {
    return { statusCode: 301, statusDescription: 'Moved Permanently',
             headers: { location: { value: 'https://devstudio.ai.kr' + req.uri } } };
  }
  if (req.uri.endsWith('/')) req.uri += 'index.html';
  else if (req.uri.indexOf('.') === -1) req.uri += '/index.html';
  return req;
}
```

## 7. CloudFront 배포 만들기 (4단계 인증서 발급 후)

CloudFront → **배포 생성**

| 항목 | 값 |
|---|---|
| 원본 도메인 | 5단계 S3 버킷 선택 |
| 원본 액세스 | **원본 액세스 제어 설정(OAC)** → 새 OAC 생성 |
| 뷰어 프로토콜 정책 | **Redirect HTTP to HTTPS** |
| 캐시 정책 | CachingOptimized |
| 함수 연결 | 뷰어 요청 → CloudFront Functions → `devstudio-rewrite` |
| 가격 분류 | 모든 엣지 로케이션 사용 (비용 차이 거의 없음) |
| 대체 도메인 이름(CNAME) | `devstudio.ai.kr`, `www.devstudio.ai.kr` |
| 사용자 정의 SSL 인증서 | 4단계 인증서 |
| 기본값 루트 객체 | `index.html` |
| 웹 애플리케이션 방화벽(WAF) | 사용 안 함 (유료) |

생성 후:

1. 상단 안내 배너의 **버킷 정책 복사** → S3 버킷 → 권한 → 버킷 정책에 붙여넣기 (CloudFront만 읽을 수 있게 하는 정책)
2. 배포 → **오류 페이지 → 사용자 정의 오류 응답 생성** (2개)
   - 403 → 응답 페이지 `/404.html`, 응답 코드 `404`
   - 404 → 응답 페이지 `/404.html`, 응답 코드 `404`
3. **배포 ID**(`E…`)와 **배포 도메인**(`d….cloudfront.net`) 메모
4. 상태가 **배포됨**이 될 때까지 대기 (5~10분)

## 8. 배포 전용 IAM 사용자 (내 작업용)

IAM → 사용자 → **사용자 생성** → 이름 `devstudio-deployer` → 콘솔 액세스 **없음**

권한 → **정책 직접 연결 → 인라인 정책(JSON)**. 아래에서 `BUCKET`, `ACCOUNT_ID`(콘솔 우측 상단 12자리), `DIST_ID`를 바꿔 넣는다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    { "Effect": "Allow", "Action": "s3:ListBucket", "Resource": "arn:aws:s3:::BUCKET" },
    { "Effect": "Allow", "Action": ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"], "Resource": "arn:aws:s3:::BUCKET/*" },
    { "Effect": "Allow", "Action": "cloudfront:CreateInvalidation", "Resource": "arn:aws:cloudfront::ACCOUNT_ID:distribution/DIST_ID" }
  ]
}
```

사용자 → 보안 자격 증명 → **액세스 키 만들기** → 용도 "CLI" → 키를 **그 자리에서 내 맥에 바로 입력** (메신저로 주고받지 않기)

```bash
aws configure --profile devstudio
# Access Key / Secret Key 입력, region: ap-northeast-2, output: json
aws sts get-caller-identity --profile devstudio   # Account가 의뢰자 계정 ID인지 확인
```

## 9. 첫 배포

1. `scripts/deploy-aws.sh` 상단의 `BUCKET`, `DIST_ID`, `ACCOUNT_ID` 채우기
2. 실행:

```bash
cd ~/project/devstudio-site
./scripts/deploy-aws.sh
```

3. DNS 바꾸기 전에 CloudFront 주소로 먼저 확인: `https://d….cloudfront.net/`, `/work`, `/contact`

## 10. DNS 전환 (내 Cloudflare, `devstudio.ai.kr`)

기존 두 레코드(라즈베리파이 터널)를 **수정**한다.

| Type | Name | Target (변경 후) | Proxy |
|---|---|---|---|
| CNAME | `@` | `d….cloudfront.net` | **끄기 (회색 구름)** |
| CNAME | `www` | `d….cloudfront.net` | **끄기 (회색 구름)** |

- 프록시를 켜두면 Cloudflare 해외(LA) 경유로 다시 느려진다. **반드시 회색 구름.**
- 기존 Target 값(`08acb9b7-….cfargotunnel.com`)은 되돌리기용으로 메모해 둔다.

## 11. 확인

```bash
curl -sI https://devstudio.ai.kr/ | grep -iE "^HTTP|x-amz-cf-pop|x-cache"   # x-amz-cf-pop 이 ICN… 이면 서울 엣지
for p in / /work /contact /work#food; do curl -s -o /dev/null -w "$p %{http_code}\n" "https://devstudio.ai.kr$p"; done
curl -s -o /dev/null -w "없는 주소 %{http_code}\n" https://devstudio.ai.kr/nope               # 404
curl -s -o /dev/null -w "www %{http_code} -> %{redirect_url}\n" https://www.devstudio.ai.kr/work  # 301 -> https://devstudio.ai.kr/work
curl -s -o /dev/null -w "http %{http_code} -> %{redirect_url}\n" http://devstudio.ai.kr/          # 301 -> https
```

브라우저 확인: PC·휴대폰(LTE)에서 첫 화면 순환, 포트폴리오 탭·갤러리, 모바일 메뉴, 카톡 버튼

**문제가 생기면 (되돌리기):** 10단계 두 레코드를 원래 터널 Target + 주황 구름으로 되돌리면 라즈베리파이로 즉시 복구된다.

## 12. 마무리

- [ ] 의뢰자에게 남길 것: AWS 계정 소유 확인, MFA 설정됨, 예산 알림 $5 설정됨, `devstudio-deployer`는 나의 배포 전용 계정이며 관계 종료 시 **IAM에서 삭제하면 내 접근이 끊긴다**는 점
- [ ] 1주일 정상 운영 확인 후 라즈베리파이 정리 (친구 서버)
  - `~/workspace/huiyeong/thaiuniontour/cloudflared/config.yml`에서 `devstudio.ai.kr`, `www.devstudio.ai.kr`, `devstudio.thaiuniontour.com` 항목 삭제 → `docker restart tut-cloudflared` (thaiuniontour·wedding·family 수 초 끊김)
  - `cd ~/workspace/huiyeong/devstudio && docker-compose down`
  - 시안 확인용으로 계속 쓸 거면 위 정리는 하지 않고 별도 서브도메인으로 유지
- [ ] (선택) 도메인 DNS도 의뢰자 쪽으로 이전: 의뢰자 Cloudflare 계정(무료) 또는 Route 53(월 약 $0.5)

---

## 이후 수정·배포 방법

```bash
# 사진 바꿨을 때만
node scripts/optimize-photos.mjs
# 배포 (빌드 → S3 업로드 → CloudFront 캐시 갱신)
./scripts/deploy-aws.sh
```

반영까지 보통 1~2분. 휴대폰에서 바로 안 보이면 새로고침.
