# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and dependency files first for layer caching
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon -q

# Copy source and build the fat JAR, skipping tests
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the fat JAR from the build stage
COPY --from=builder /app/build/libs/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 9090

ENTRYPOINT ["java", "-jar", "app.jar"]