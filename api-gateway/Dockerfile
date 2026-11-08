# Multi-stage Dockerfile for API Gateway
# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -f api-gateway/pom.xml clean package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
USER 1000
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]