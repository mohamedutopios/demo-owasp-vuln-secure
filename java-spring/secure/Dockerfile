# TaskForge - Java (Spring Boot 3.3.4) - version CONFORME OWASP Top 10:2025
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:17-jre
# Utilisateur non root
RUN useradd --uid 10001 appuser
WORKDIR /app
COPY --from=build /src/target/*.jar app.jar
USER appuser
# Secrets de chiffrement fournis par l'environnement (APP_ENC_PASSWORD / APP_ENC_SALT).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
