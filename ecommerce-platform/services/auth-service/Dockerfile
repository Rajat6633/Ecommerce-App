# ============================================================================
#  auth-service — multi-stage, layered, non-root, healthcheck.
# ============================================================================

# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# The full reactor needs EVERY module pom present (parent <modules> list),
# even when scoped with -pl/-am. Copy all poms first to cache dependencies.
COPY pom.xml .
COPY shared/common-events/pom.xml shared/common-events/pom.xml
COPY services/auth-service/pom.xml services/auth-service/pom.xml
COPY services/product-service/pom.xml services/product-service/pom.xml
COPY services/inventory-service/pom.xml services/inventory-service/pom.xml
COPY services/cart-service/pom.xml services/cart-service/pom.xml
COPY services/order-service/pom.xml services/order-service/pom.xml
COPY services/payment-service/pom.xml services/payment-service/pom.xml
COPY services/notification-service/pom.xml services/notification-service/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -pl services/auth-service -am dependency:go-offline -DskipTests || true
# Copy only the sources this build needs and package
COPY shared/common-events/src shared/common-events/src
COPY services/auth-service/src services/auth-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -q -pl services/auth-service -am clean package -DskipTests

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Non-root user
RUN addgroup -S app && adduser -S app -G app

# Curl for the healthcheck
RUN apk add --no-cache curl

COPY --from=build /build/services/auth-service/target/auth-service.jar app.jar
RUN chown -R app:app /app
USER app

EXPOSE 8081

# Container-aware JVM flags
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS http://localhost:8081/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
