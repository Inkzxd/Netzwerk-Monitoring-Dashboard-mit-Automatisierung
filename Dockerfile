## Dockerfile

## This Dockerfile defines the build process for the Network-Monitoring Dashboard.
## The application is packaged as a Spring Boot executable JAR and deployed using a lightweight Java 21 runtime image.

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/network-monitoring-dashboard-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]