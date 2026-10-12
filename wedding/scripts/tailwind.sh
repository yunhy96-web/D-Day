#!/bin/sh
# 템플릿·JS 의 클래스를 바꿨으면 실행해서 static/tw.css 를 다시 만들고 커밋한다. (Pi 빌드엔 node 불필요)
cd "$(dirname "$0")/.." && npx -y tailwindcss@3.4.17 -c tailwind.config.js -i src/main/tailwind/input.css -o src/main/resources/static/tw.css --minify
