# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy settings and pom
COPY settings.xml .
COPY pom.xml .

# Pre-download dependencies
RUN mvn -s settings.xml dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn -s settings.xml package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
