# Build stage
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy gradle files
COPY gradle/ gradle/
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle .
COPY build.gradle .

# Copy libs
COPY libs/ libs/

# Copy source
COPY services/svc-settlement/ services/svc-settlement/

# Build the application
RUN ./gradlew :services:svc-settlement:bootJar -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
  adduser -u 1001 -S appuser -G appgroup

# Copy the built jar
COPY --from=build /app/services/svc-settlement/build/libs/svc-settlement-*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
