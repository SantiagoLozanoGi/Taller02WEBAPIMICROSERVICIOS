FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY user-service/pom.xml user-service/pom.xml
RUN mvn -B -pl user-service -am dependency:go-offline

COPY user-service/src user-service/src
RUN mvn -B -pl user-service -am clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring
COPY --from=build /workspace/user-service/target/user-service-2.0.0.jar app.jar
USER spring

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
