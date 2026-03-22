FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/backend-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Xmx650m", "-Xms256m", "-XX:+UseG1GC", "-jar", "app.jar"]