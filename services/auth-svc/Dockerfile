# Use pre-built JAR (built locally with: mvn clean package -pl services/auth-svc -am -DskipTests)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY services/auth-svc/target/*.jar app.jar

# Virtual threads + optimized GC for containers
ENTRYPOINT ["java", \
  "-XX:+UseZGC", \
  "-XX:+ZGenerational", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
