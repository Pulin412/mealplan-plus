# Runtime-only image — JAR is pre-built on the host via ./gradlew bootJar
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY build/libs/backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
