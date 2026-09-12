# Network Monitoring Dashboard

A lightweight network monitoring dashboard for checking device availability, collecting Prometheus metrics, visualizing monitoring data, and managing incidents.

## Features

- TCP availability checks for configured network devices.
- Prometheus metrics for device status and latency.
- Grafana dashboard for monitoring metrics.
- Alertmanager integration for firing and resolving alerts.
- Incident history stored in SQLite.
- Web dashboard for device status and incidents.
- HTTP Basic Authentication for administrative actions.
- Docker Compose deployment.

## Technology Stack

- Java 21
- Spring Boot
- Spring Data JPA
- SQLite
- Prometheus
- Grafana
- Alertmanager
- Docker Compose
- Maven

## Requirements

- Docker and Docker Compose
- Alternatively: Java 21 and Maven for local development

## Configuration

Create an environment file from the example:

```bash
cp .env.example .env
```

Adjust the values in `.env` if required. Administrative credentials should be changed before using the application in a production environment.

Monitoring devices are configured in the application configuration file or through the corresponding environment variables defined in `.env.example`.

## Start with Docker Compose

Build and start all services:

```bash
docker compose up --build
```

The main services are available at:

- Application: http://localhost:8080
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Alertmanager: http://localhost:9093

Stop the services with:

```bash
docker compose down
```

To remove containers and volumes as well, use:

```bash
docker compose down -v
```

## Local Development

Run the tests with Maven:

```bash
mvn clean test
```

Start the Spring Boot application locally with:

```bash
mvn spring-boot:run
```

The application is then available at:

```text
http://localhost:8080
```

## Main API Endpoints

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| GET | `/api/status` | Returns the application status | Public |
| GET | `/api/devices` | Returns all configured devices | Public |
| GET | `/api/checks/latest` | Returns the latest check results | Public |
| GET | `/api/checks/history` | Returns historical check results | Public |
| POST | `/api/checks/run` | Starts a manual check | ADMIN |
| GET | `/api/incidents` | Returns all incidents | Public |
| GET | `/api/incidents/active` | Returns active incidents | Public |
| POST | `/api/alerts` | Receives Alertmanager webhooks | ADMIN |

Example status request:

```bash
curl http://localhost:8080/api/status
```

Example authenticated manual check:

```bash
curl -u "$APP_ADMIN_USER:$APP_ADMIN_PASSWORD" \
  -X POST http://localhost:8080/api/checks/run
```

## Project Structure

```text
deploy/
  alertmanager/       Alertmanager configuration
  grafana/            Grafana datasource and dashboard
  prometheus/         Prometheus configuration and alert rules

docs/                 Additional documentation
src/main/java/        Spring Boot application source code
src/main/resources/   Application configuration and web dashboard
src/test/             Unit and integration tests
Dockerfile            Application container definition
docker-compose.yml    Multi-container deployment
pom.xml               Maven project configuration
```

## Monitoring Flow

```text
Network Device
    -> Spring Boot Monitoring Service
    -> Prometheus Metrics
    -> Prometheus Alert Rules
    -> Alertmanager
    -> Incident Webhook
    -> SQLite Incident History
    -> Web Dashboard
```

## Security Note

Administrative endpoints are protected with HTTP Basic Authentication and the `ADMIN` role. Do not use the example credentials in production. Store real credentials only in environment variables and do not commit the `.env` file to the repository.

## Project Status

This project was developed as part of a Bachelor's thesis in Practical Computer Science at HTW Saar. Its purpose is to demonstrate an automated network monitoring dashboard with metrics, alerting, incident management, and containerized deployment.
