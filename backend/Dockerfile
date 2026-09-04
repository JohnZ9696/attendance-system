# Use Maven with Java 17 to build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (caches this layer!)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# STAGE 2: RUN STAGE
# ==========================================
# Use a lightweight Java 17 runtime environment (No Maven needed here!)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# only copy the built .jar file from the "builder" stage above
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot server port (default is 8080)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]