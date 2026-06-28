# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

# Copy contracts folder only if it exists (optional – used by web3j plugin)
# If missing, the build skips Solidity wrapper generation
COPY . .

# Skip Solidity source generation during Docker build
# Run `mvn web3j:generate-sources` locally before pushing
RUN apk add --no-cache maven && \
    mvn -B clean package -DskipTests -Dskip.web3j.generate=true

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S skincert && adduser -S skincert -G skincert
USER skincert

COPY --from=builder /app/target/skincertchain-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/api/v1/certificates/health || exit 1

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
