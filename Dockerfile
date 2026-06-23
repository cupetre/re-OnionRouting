FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/onionRouting2-1.0-SNAPSHOT.jar /app/onion-routing.jar

ENTRYPOINT ["java", "-cp", "/app/onion-routing.jar", "org.example.Main"]
