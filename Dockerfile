# syntax=docker/dockerfile:1

# ---- build stage: compiles and packages the executable jar (tests run in CI, not here) ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /src
# Dependencies first, so this layer is reused while only the sources change.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- runtime stage: JRE only, non-root, no build tools ----
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
USER app

# The platform injects PORT (server.port=${PORT:8080}); every secret comes from environment variables.
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT:-8080}/actuator/health" | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
