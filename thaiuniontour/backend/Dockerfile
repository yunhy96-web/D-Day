# 사전 빌드된 boot jar만 담는 런타임 이미지. 빌드: ./gradlew :api:bootJar (호스트) → docker build --build-arg MODULE=api
FROM eclipse-temurin:17-jre
ARG MODULE
COPY ${MODULE}/build/libs/${MODULE}.jar /app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx512m"
ENTRYPOINT ["java", "-jar", "/app.jar"]
