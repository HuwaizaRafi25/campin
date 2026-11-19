# --- Stage 1: Build ---
FROM gradle:8.10-jdk17 AS builder
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN gradle dependencies --no-daemon > /dev/null 2>&1 || true
COPY . .
RUN gradle installDist --no-daemon

# --- Stage 2: Runtime ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/install/campin /app
EXPOSE 8088
ENTRYPOINT ["/app/bin/campin"]