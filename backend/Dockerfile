FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy entire source tree
COPY . .

# Package all modules (common and 9 microservices)
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
# MODULE_NAME allows this single Dockerfile to build any of the 9 microservices
ARG MODULE_NAME
ENV MODULE_NAME=${MODULE_NAME}

WORKDIR /app

# Extract only the specific microservice's compiled JAR
COPY --from=builder /build/${MODULE_NAME}/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
