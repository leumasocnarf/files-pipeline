ARG JAVA_VERSION=25

FROM eclipse-temurin:$JAVA_VERSION-jre-alpine

# For development only. To make curl requests inside docker network
RUN addgroup -S spring && adduser -S spring -G spring \
    && apk add --no-cache curl bash jq

WORKDIR /app
COPY build/libs/*.jar app.jar

USER spring:spring

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]