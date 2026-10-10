# 멀티스테이지. Pi(arm64)에서 docker-compose build 로 빌드.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies -q || true
COPY src src
RUN ./gradlew --no-daemon bootJar -x test -q

FROM eclipse-temurin:17-jre
RUN useradd -u 1000 -m app && mkdir -p /data && chown app /data
USER app
WORKDIR /app
COPY --from=build /src/build/libs/*.jar app.jar
ENV APP_DATA_DIR=/data JAVA_TOOL_OPTIONS="-Xmx256m -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
