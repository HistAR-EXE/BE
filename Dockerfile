FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src ./src
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:17-jre-alpine
# curl powers the container healthcheck; ffmpeg is required by admin media
# uploads/transcoding before the resulting object is stored in Cloudflare R2.
RUN apk add --no-cache curl ffmpeg
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
