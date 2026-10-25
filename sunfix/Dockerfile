# 멀티스테이지. Pi(arm64)에서 docker-compose build 로 빌드.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies -q || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test -q

FROM eclipse-temurin:17-jre
# 최신 temurin(Ubuntu 24.04) 은 UID 1000(ubuntu) 이 이미 있다. 새로 만들지 않고 UID 로만 지정 (Pi 사용자도 1000 이라 ./data 권한 일치)
RUN mkdir -p /data && chown 1000:1000 /data
USER 1000:1000
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
ENV APP_DATA_DIR=/data JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
