# Multi-stage build — uses standalone backend/settings.gradle.kts
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Gradle wrapper + backend build files
COPY gradlew gradlew.bat gradle.properties ./
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

# Build the JAR (standalone backend build, no Android/KMP needed)
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

# --- Runtime image ---
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /workspace/build/libs/backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
