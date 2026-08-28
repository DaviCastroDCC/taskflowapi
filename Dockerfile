# syntax=docker/dockerfile:1

# ---------- build ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Resolve dependencies first so a source-only change doesn't re-download the world.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B clean package -DskipTests

# Split the fat jar into Spring Boot layers for better image-layer caching.
RUN java -Djarmode=layertools -jar target/taskflow-api.jar extract --destination target/extracted

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# curl is only here to back the container HEALTHCHECK / compose health probe.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Never run as root.
RUN groupadd --system --gid 1001 taskflow \
 && useradd  --system --uid 1001 --gid taskflow --home /app taskflow
USER taskflow

# Ordered least- to most-frequently changed so Docker reuses the lower layers.
COPY --from=build --chown=taskflow:taskflow /workspace/target/extracted/dependencies/          ./
COPY --from=build --chown=taskflow:taskflow /workspace/target/extracted/spring-boot-loader/     ./
COPY --from=build --chown=taskflow:taskflow /workspace/target/extracted/snapshot-dependencies/  ./
COPY --from=build --chown=taskflow:taskflow /workspace/target/extracted/application/            ./

EXPOSE 8080

# Container-aware heap sizing; the runner sets the actual limit.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
