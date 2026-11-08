# Multi-stage Dockerfile for Hotel Service
# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -f hotel-service/pom.xml clean package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/hotel-service/target/hotel-service-0.0.1-SNAPSHOT.jar app.jar
USER 1000
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]