#!/bin/bash
# Lightsail 배포: 빌드 → 서버(/var/www/devstudio)로 동기화
# 필요: ~/.ssh/config 의 devstudio-lightsail 항목 + ~/.ssh/devstudio-lightsail 키
set -e
cd "$(dirname "$0")/.."
npm run build
rsync -az --delete dist/ devstudio-lightsail:/var/www/devstudio/
echo "배포 완료: http://52.78.193.240"
