# ============================================================
#  Backend Dockerfile – Spring Boot (Java 17)
#  Multi-stage build: Maven build → lightweight JRE image
# ============================================================

# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom first so dependency layer is cached separately
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests -B

# ─────────────────────────────────────────────────────────────
# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Security: run as non-root
RUN addgroup -S finguard && adduser -S finguard -G finguard
USER finguard

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
