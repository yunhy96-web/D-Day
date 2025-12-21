# Stage 1: Build
FROM node:20-alpine AS builder

WORKDIR /app

# 패치 파일과 package.json 먼저 복사
COPY package*.json ./
COPY patches ./patches

# 의존성 설치 (postinstall에서 patch-package 실행됨)
RUN npm ci

# 소스 복사
COPY . .

# 환경변수 설정 (빌드 시 ARG로 전달)
ARG EXPO_PUBLIC_API_URL
ENV EXPO_PUBLIC_API_URL=$EXPO_PUBLIC_API_URL

# 웹 빌드
RUN npx expo export --platform web

# Stage 2: Production
FROM nginx:alpine

# Nginx 설정 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 빌드 결과물 복사
COPY --from=builder /app/dist /usr/share/nginx/html

# 포트 노출
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
