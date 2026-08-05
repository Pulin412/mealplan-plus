# Runtime-only image — JAR is pre-built on the host via ./gradlew bootJar
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY build/libs/backend.jar app.jar
EXPOSE 8080
# SerialGC keeps native GC footprint small on a memory-constrained Cloud Run
# container; MaxRAMPercentage caps heap so heap + non-heap stays under the
# 768Mi limit (see backend-deploy.yml --memory). Tuned after a 512Mi OOM.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
