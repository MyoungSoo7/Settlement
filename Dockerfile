# Stage 1: Build
FROM gradle:8.5-jdk21-alpine AS builder
WORKDIR /app

# Copy Gradle files for dependency caching
COPY build.gradle.kts settings.gradle ./

COPY gradle ./gradle
COPY src ./src

RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
