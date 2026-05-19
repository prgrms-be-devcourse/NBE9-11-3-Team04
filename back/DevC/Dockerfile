FROM eclipse-temurin:24-jdk AS build
WORKDIR /app

COPY . .
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:24-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
