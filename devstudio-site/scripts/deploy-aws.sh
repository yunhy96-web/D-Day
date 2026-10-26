#!/bin/sh
# 정식 사이트(의뢰자 AWS) 배포: 빌드 → S3 업로드 → CloudFront 캐시 갱신
# 사용: ./scripts/deploy-aws.sh   (설정 방법은 DEPLOY-AWS.md)
set -e

PROFILE=devstudio
BUCKET=""       # 예: devstudio-ai-kr        (DEPLOY-AWS.md 5단계)
DIST_ID=""      # 예: E1ABCDEF2GHIJK         (7단계)
ACCOUNT_ID=""   # 의뢰자 AWS 계정 12자리      (8단계)

[ -n "$BUCKET" ] && [ -n "$DIST_ID" ] && [ -n "$ACCOUNT_ID" ] || { echo "BUCKET / DIST_ID / ACCOUNT_ID 를 먼저 채우세요"; exit 1; }

# 회사 계정 등 다른 계정으로 올라가는 사고 방지
ACTUAL=$(aws sts get-caller-identity --profile "$PROFILE" --query Account --output text)
[ "$ACTUAL" = "$ACCOUNT_ID" ] || { echo "계정 불일치: 프로필 $PROFILE = $ACTUAL, 기대값 $ACCOUNT_ID"; exit 1; }

cd "$(dirname "$0")/.."
npm run build

# 파일명에 해시가 붙는 빌드 산출물은 오래 캐시, 나머지(HTML·사진)는 짧게
aws s3 sync dist/_astro "s3://$BUCKET/_astro" --delete --profile "$PROFILE" \
  --cache-control "public,max-age=31536000,immutable"
aws s3 sync dist "s3://$BUCKET" --delete --profile "$PROFILE" --exclude "_astro/*" \
  --cache-control "public,max-age=300"

aws cloudfront create-invalidation --distribution-id "$DIST_ID" --paths "/*" --profile "$PROFILE" \
  --query Invalidation.Id --output text
echo "배포 완료 → https://devstudio.ai.kr (반영 1~2분)"
