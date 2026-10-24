# the jar is built and tested by CI (./gradlew bootJar) - this image only packages it
# split the fat jar into layers so a code change doesn't re-push the dependencies
FROM eclipse-temurin:25-jre-alpine AS extractor
WORKDIR /workspace
COPY build/libs/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# least frequently changing layers first
COPY --from=extractor /workspace/extracted/dependencies/ ./
COPY --from=extractor /workspace/extracted/spring-boot-loader/ ./
COPY --from=extractor /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extractor /workspace/extracted/application/ ./

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# size the heap from the container memory limit
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
