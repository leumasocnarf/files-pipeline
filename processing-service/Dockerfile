ARG JAVA_VERSION=25

FROM eclipse-temurin:$JAVA_VERSION-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY build/libs/*.jar app.jar

USER spring:spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]