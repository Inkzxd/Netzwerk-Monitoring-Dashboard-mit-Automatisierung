# Network Monitoring Dashboard

A lightweight network monitoring dashboard for checking network availability, collecting Prometheus metrics, visualizing monitoring data, and managing incidents.

The project was developed as part of a Bachelor's thesis in Practical Computer Science (*Praktische Informatik*) at HTW Saar. It implements a functional monitoring prototype based on Spring Boot, Micrometer, Prometheus, Grafana, Alertmanager, and Docker Compose.

## Features

- TCP availability checks for configured network devices and services.
- ICMP ping checks with separate ping status and latency metrics.
- Monitoring of LAN and VPN endpoints, including checked service ports.
- Spring Boot Actuator and Micrometer metrics for the application and monitoring service.
- Prometheus time-series storage and alert-rule evaluation.
- Grafana dashboard for device status, TCP latency, ping status, ping latency, and device counts.
- Alertmanager integration for firing and resolved alerts.
- Incident history stored in SQLite.
- Web dashboard for current device status and incidents.
- HTTP Basic Authentication for administrative actions.
- Docker Compose deployment for the complete monitoring stack.
- Bash script for sending email alerts when monitored TCP devices are down.

## Technology Stack

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Boot Actuator
- Micrometer
- SQLite
- Prometheus
- Grafana
- Alertmanager
- Docker Compose
- Maven
- Bash

## Architecture

The monitoring flow is structured as follows:

```text
Network Devices
    -> Spring Boot Monitoring Service
    -> Micrometer and Actuator Metrics
    -> Prometheus
    -> Prometheus Alert Rules
    -> Alertmanager
    -> Spring Boot Alert Webhook
    -> SQLite Incident History
    -> Web Dashboard and Grafana
```

The Spring Boot service performs TCP and ping checks for configured devices. It exposes the current results through REST endpoints and Prometheus metrics. Prometheus stores the metrics and evaluates alert rules. Alertmanager forwards firing and resolved alerts to the Spring Boot webhook and, where configured, by email.

A detailed architecture description is available in [`docs/architecture.md`](docs/architecture.md).

## Requirements

- Docker and Docker Compose
- Alternatively, Java 21 and Maven for local development

## Configuration

Create an environment file from the example:

```bash
cp .env.example .env
```

Adjust the values in `.env` if required. Administrative credentials, Grafana credentials, and email settings should be changed before using the application in a production-like environment.

Monitoring devices are configured in:

```text
src/main/resources/application.yml
```

The configuration can contain application services, Prometheus, Grafana, demo services, a LAN router, a VPN endpoint, and controlled test devices. Replace environment-specific addresses with values from your own test environment before running the monitoring checks.

For detailed LAN and VPN configuration and verification, see [`docs/lan-vpn-endpoint-test-guide.md`](docs/lan-vpn-endpoint-test-guide.md).

## Start with Docker Compose

Build and start the complete monitoring stack:

```bash
docker compose up --build
```

To start the services in the background:

```bash
docker compose up --build -d
```

The main services are available at:

| Service | URL |
|---|---|
| Spring Boot application | [http://localhost:8080](http://localhost:8080) |
| Grafana | [http://localhost:3000](http://localhost:3000) |
| Prometheus | [http://localhost:9090](http://localhost:9090) |
| Alertmanager | [http://localhost:9093](http://localhost:9093) |

Check the container status:

```bash
docker compose ps
```

Stop the services:

```bash
docker compose down
```

To remove containers and volumes as well:

```bash
docker compose down -v
```

## Local Development

Run the complete Maven test suite:

```bash
mvn clean test
```

Start the Spring Boot application locally:

```bash
mvn spring-boot:run
```

The local application is then available at:

```text
http://localhost:8080
```

The test suite contains controller tests, service tests, Alertmanager webhook tests, repository integration tests, and security integration tests. Detailed test procedures are available in [`docs/testing.md`](docs/testing.md).

## Main API Endpoints

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| GET | `/api/status` | Returns the application status | Public |
| GET | `/api/devices` | Returns all configured devices | Public |
| GET | `/api/devices/{name}` | Returns one device by name | Public |
| GET | `/api/checks/latest` | Returns the latest TCP check results | Public |
| GET | `/api/checks/history` | Returns historical TCP check results | Public |
| GET | `/api/pings/latest` | Returns the latest ping check results | Public |
| POST | `/api/checks/run` | Starts a manual check for configured devices | ADMIN |
| GET | `/api/incidents` | Returns all incidents | Public |
| GET | `/api/incidents/active` | Returns active incidents | Public |
| GET | `/api/incidents/{id}` | Returns one incident by ID | Public |
| POST | `/api/alerts` | Receives Alertmanager webhooks | ADMIN |
| GET | `/actuator/health` | Returns the application health status | Public |
| GET | `/actuator/prometheus` | Exposes Prometheus metrics | Public |

Example status request:

```bash
curl http://localhost:8080/api/status
```

Example metrics request:

```bash
curl http://localhost:8080/actuator/prometheus
```

Example authenticated manual check:

```bash
curl -u "$APP_ADMIN_USER:$APP_ADMIN_PASSWORD" \
  -X POST http://localhost:8080/api/checks/run
```

Detailed endpoint descriptions and request examples are available in [`docs/api.md`](docs/api.md).

## Monitoring Metrics

The application exposes Spring Boot and Micrometer metrics through `/actuator/prometheus`. Depending on the enabled instrumentation, these include application, HTTP, JVM, and monitoring metrics.

The monitoring service also exposes device-related metrics, including:

```text
networkdeviceup
networkdevicelatencyms
networkdevicepingup
networkdevicepinglatencyms
```

The exact labels and available values can be inspected directly at the Prometheus endpoint or in the Prometheus web interface.

## Grafana Dashboard

Grafana is provisioned automatically through the files in `deploy/grafana/`. The dashboard is stored in:

```text
deploy/grafana/dashboards/network-monitoring.json
```

The dashboard is intended to show:

- TCP device status.
- TCP latency.
- Ping device status.
- ICMP ping latency.
- Online and offline device counts.
- Current monitoring values for LAN, VPN, application, and service endpoints.

Open Grafana at [http://localhost:3000](http://localhost:3000) and select the configured Network Monitoring dashboard.

## Alerting and Incidents

Prometheus alert rules are located in:

```text
deploy/prometheus/rules/alert.rules.yml
```

Alertmanager is configured in:

```text
deploy/alertmanager/alertmanager.yml
```

Alertmanager can forward firing and resolved alerts to the Spring Boot webhook:

```text
POST /api/alerts
```

The application processes these webhooks and stores incident information in SQLite. Existing active incidents are not duplicated when the same alert fingerprint is received, and resolved alerts update the corresponding incident.

The helper script below queries Prometheus for down TCP devices and sends an email alert:

```bash
scripts/send-tcp-alert.sh
```

Run it from the project root after configuring `.env` and ensuring that the required command-line tools, including `curl`, `jq`, and `mail`, are available.

## LAN and VPN Testing

The project includes a dedicated guide for testing LAN and VPN endpoints:

```text
docs/lan-vpn-endpoint-test-guide.md
```

The guide covers:

- Checking routes with `route -n get`.
- Testing ICMP reachability with `ping`.
- Testing service ports with `nc`.
- Testing DNS reachability with `dig`.
- Verifying the resulting Prometheus metrics.
- Checking the results in Grafana.

A VPN endpoint may be reachable over TCP while ICMP ping is unavailable because ICMP can be blocked by a firewall. Therefore, TCP status and ping status are represented as separate measurements.

## Project Structure

```text
deploy/
  alertmanager/       Alertmanager configuration
  grafana/            Grafana provisioning and dashboard
  prometheus/         Prometheus configuration and alert rules

docs/                 Architecture, API, deployment, and test documentation
scripts/              Helper scripts for alerting
src/main/java/        Spring Boot application source code
src/main/resources/   Application configuration and web dashboard
src/test/             Unit and integration tests
Dockerfile            Application container definition
docker-compose.yml    Multi-container deployment
pom.xml               Maven project configuration
```

## Documentation

- [`docs/api.md`](docs/api.md) – REST API documentation and examples.
- [`docs/architecture.md`](docs/architecture.md) – System architecture, database model, and alert flow.
- [`docs/deployment.md`](docs/deployment.md) – Docker Compose deployment and verification steps.
- [`docs/lan-vpn-endpoint-test-guide.md`](docs/lan-vpn-endpoint-test-guide.md) – LAN and VPN endpoint testing.
- [`docs/incident-history-test-guide.md`](docs/incident-history-test-guide.md) – Incident creation and resolution tests.
- [`docs/security-test-results.md`](docs/security-test-results.md) – Security test results and observations.
- [`docs/testing.md`](docs/testing.md) – Automated and manual testing procedures.

## Security Note

Administrative endpoints are protected with HTTP Basic Authentication and require the `ADMIN` role. Do not use example credentials in production.

Store real credentials and SMTP passwords only in environment variables or another secure secret-management solution. Do not commit `.env` files, passwords, access tokens, or private infrastructure details to the repository.

Before publishing screenshots or documentation, remove passwords, tokens, private IP addresses, personal usernames, and private hostnames where necessary.

## Project Status

This project is a functional prototype for an automated network monitoring dashboard developed as part of a Bachelor's thesis in Practical Computer Science at HTW Saar.

The prototype focuses on automated data collection, Prometheus-based time-series monitoring, Grafana visualization, Alertmanager-based alerting, incident history, LAN/VPN endpoint checks, and containerized deployment. Optional extensions such as Ansible deployment, additional dashboards, advanced multi-condition alert rules, and extended Grafana role management can be added in future work.
