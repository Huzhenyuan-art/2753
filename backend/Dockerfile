FROM maven:3.9.6-eclipse-temurin-17 AS dependency
WORKDIR /app
COPY settings.xml .
COPY pom.xml .
RUN mvn -s settings.xml dependency:go-offline -B -q

FROM dependency AS build
WORKDIR /app
COPY src ./src
RUN mvn -s settings.xml package -DskipTests -B -q \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads/avatars /app/logs \
    && chown -R spring:spring /app

USER spring:spring

COPY --from=build /app/target/extracted/dependencies/ ./
COPY --from=build /app/target/extracted/spring-boot-loader/ ./
COPY --from=build /app/target/extracted/snapshot-dependencies/ ./
COPY --from=build /app/target/extracted/application/ ./

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dfile.encoding=UTF-8 org.springframework.boot.loader.launch.JarLauncher"]
