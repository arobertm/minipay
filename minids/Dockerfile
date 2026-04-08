FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml /app/pom.xml
COPY minids/pom.xml /app/minids/pom.xml
COPY minids/src /app/minids/src

RUN mvn -f /app/minids/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/minids/target/*.jar app.jar

EXPOSE ${API_PORT:-8301}
EXPOSE ${RAFT_PORT:-8300}

ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
