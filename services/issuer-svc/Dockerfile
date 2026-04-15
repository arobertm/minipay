FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY services/issuer-svc/target/*.jar app.jar

ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
