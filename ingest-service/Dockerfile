# syntax=docker/dockerfile:1
ARG JAVA_VERSION=25

FROM eclipse-temurin:$JAVA_VERSION-jre-alpine
WORKDIR /app

COPY build/libs/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java", "-jar", "app.jar"]