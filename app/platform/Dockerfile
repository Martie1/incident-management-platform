# The jar is built and tested by CI (./gradlew bootJar), this image only packages it.

# Split the jar into layers (dependencies, application code), so a code change
# doesn't produce a new copy of all the dependencies.
FROM eclipse-temurin:25-jre-alpine AS extractor
WORKDIR /workspace
COPY build/libs/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# install security fixes released after the base image was built (found by the Trivy scan in CI)
RUN apk upgrade --no-cache

# non-root user with a numeric id, so Kubernetes can verify runAsNonRoot
RUN addgroup -S -g 1000 spring && adduser -S -u 1000 -G spring spring
USER 1000:1000

COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

EXPOSE 8080

# heap size based on the container memory limit
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
