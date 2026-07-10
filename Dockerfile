# Stage 1: Build the Maven application using Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight runtime container using Java 21 JRE
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the compiled executable fat-jar from the build container
COPY --from=build /app/target/*.jar app.jar

# Expose your custom fallback port
EXPOSE 8045

ENTRYPOINT ["java", "-jar", "app.jar"]