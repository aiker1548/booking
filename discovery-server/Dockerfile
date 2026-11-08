# Multi-stage Dockerfile for Discovery Server
# Build stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -f discovery-server/pom.xml clean package

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/discovery-server/target/discovery-server-0.0.1-SNAPSHOT.jar app.jar
USER 1000
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]