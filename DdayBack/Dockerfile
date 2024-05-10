FROM openjdk:17-alpine
ARG JAR_FILE=build/libs/DdayBack-1.0.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]