# build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

# copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# make gradlew executable
RUN chmod +x ./gradlew

# download dependencies
RUN ./gradlew dependencies --no-daemon || true

# build application jar
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# runtime stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

# create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# copy jar from build stage
COPY --from=builder /workspace/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
