# Stage 1: Build the Maven application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
# CHANGE THIS LINE 👇 (Remove the ./ and the w)
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight runtime container
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the compiled executable fat-jar from the build container
COPY --from=build /app/target/*.jar app.jar

# Expose your custom fallback port
EXPOSE 8045

ENTRYPOINT ["java", "-jar", "app.jar"]