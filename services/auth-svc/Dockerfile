# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copiem parent POM si modulul
COPY ../../pom.xml /app/pom.xml
COPY pom.xml /app/services/auth-svc/pom.xml
COPY src /app/services/auth-svc/src

RUN mvn -f /app/pom.xml clean package -pl services/auth-svc -am -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/services/auth-svc/target/*.jar app.jar

# Virtual threads + GC optimizat pentru containere
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
