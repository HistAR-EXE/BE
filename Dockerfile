FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -q package -DskipTests

# Flyway CLI — run migrations in entrypoint before Spring Boot (Render free port-scan)
FROM flyway/flyway:11.7.2-alpine AS flyway

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=flyway /flyway /opt/flyway
COPY src/main/resources/db/migration /app/db/migration
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
