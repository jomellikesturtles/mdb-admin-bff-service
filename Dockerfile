# Stage 1: Build the application
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

# Copy gradle wrapper and build files first for caching
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Grant execution rights to gradlew
RUN chmod +x ./gradlew

# Copy source code
COPY src src

# Build the application
RUN ./gradlew clean build -x test

# Stage 2: Run the application
FROM eclipse-temurin:24-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar from the build stage
COPY --from=build /workspace/build/libs/*.jar app.jar

# Expose the port (matches application.yml)
EXPOSE 3001

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
