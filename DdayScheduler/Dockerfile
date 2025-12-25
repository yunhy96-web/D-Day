FROM --platform=linux/amd64 eclipse-temurin:17-jre
ARG JAR_FILE=build/libs/DdayScheduler-*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
