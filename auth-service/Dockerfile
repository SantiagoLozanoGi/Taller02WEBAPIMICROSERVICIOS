FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -B -pl auth-service -am dependency:go-offline

COPY auth-service/src auth-service/src
RUN mvn -B -pl auth-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
COPY --from=build /workspace/auth-service/target/auth-service-2.0.0.jar app.jar
USER spring

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
