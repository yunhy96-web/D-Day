FROM openjdk:17-alpine
ARG JAR_FILE=build/libs/DdayBack-*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
