# ── Build stage ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src
RUN mvn -B package -DskipTests

# ── Runtime stage ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S medtracker && adduser -S medtracker -G medtracker
COPY --from=build /build/target/medtracker-backend-*.jar app.jar
RUN chown medtracker:medtracker app.jar
USER medtracker

ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=6 \
    CMD wget -q -O - http://127.0.0.1:8080/api/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
