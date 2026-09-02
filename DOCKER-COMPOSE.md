# Docker Compose Deployment

This project can be started as a complete local monitoring stack with Docker Compose. The stack consists of the network-monitoring application, Prometheus, Alertmanager, and Grafana.

## Architecture

All services are connected to the internal `monitoring` network and can reach one another by service name:

```text
                         scrapes /actuator/prometheus
                    ┌──────────────────────────────┐
                    │                              ▼
┌───────────────┐   │  ┌──────────────┐      ┌──────────────┐
│ Spring Boot   │◄──┴──│ Prometheus   │─────►│ Alertmanager │
│ app           │      └──────────────┘      └──────┬───────┘
└──────┬────────┘                                   │ webhook
       │                                             ▼
       │                                      `/api/alerts`
       │
       └──────────────────────► Grafana
                              (Prometheus datasource)
```

Prometheus scrapes the application every 15 seconds. Alert rules are loaded from `deploy/prometheus/rules`, and Alertmanager sends alert notifications back to the application at `http://app:8080/api/alerts`. Grafana is provisioned with Prometheus as its default datasource and loads dashboards from `deploy/grafana/dashboards`.

## Services and ports

| Service | Image/build | Host port | Purpose |
|---|---|---:|---|
| `app` | Built from `.` | `8080` | Network-monitoring dashboard and API |
| `prometheus` | `prom/prometheus:v2.54.1` | `9090` | Metrics collection, querying, and alert rules |
| `alertmanager` | `prom/alertmanager:v0.27.0` | `9093` | Alert grouping and webhook delivery |
| `grafana` | `grafana/grafana:11.1.0` | `3000` | Metrics dashboards and visualization |

The ports are mapped from the host to the same port inside each container:

- Application: <http://localhost:8080>
- Prometheus: <http://localhost:9090>
- Alertmanager: <http://localhost:9093>
- Grafana: <http://localhost:3000>

## Start the stack

Run these commands from the directory containing `docker-compose.yml`:

```bash
docker compose up --build -d
```

Check service status and logs:

```bash
docker compose ps
docker compose logs -f app
```

Stop the containers while keeping their persistent data:

```bash
docker compose down
```

To stop the stack and remove its named volumes, including the SQLite database, Prometheus data, Alertmanager data, and Grafana configuration, run:

```bash
docker compose down -v
```

## Persistent data

The following named volumes preserve data across container recreation:

| Volume | Container path | Contents |
|---|---|---|
| `monitoring_data` | `/app/data` | Application SQLite database (`monitoring.db`) |
| `prometheus_data` | `/prometheus` | Prometheus time-series data |
| `alertmanager_data` | `/alertmanager` | Alertmanager state |
| `grafana_data` | `/var/lib/grafana` | Grafana users and settings |

Configuration files and dashboards are bind-mounted read-only from `deploy/`, so changes to those files can be reviewed and versioned in the project.

## Configuration details

- The application uses timezone `Europe/Berlin` and listens on port `8080`.
- Prometheus reads `deploy/prometheus/prometheus.yml` and the rule files in `deploy/prometheus/rules`.
- Prometheus targets the application internally as `app:8080`, using `/actuator/prometheus`.
- Alertmanager reads `deploy/alertmanager/alertmanager.yml` and sends resolved as well as firing alerts to the application webhook.
- Grafana reads provisioning files from `deploy/grafana/provisioning` and dashboards from `deploy/grafana/dashboards`.
- Service-to-service URLs must use Compose service names such as `prometheus:9090` and `grafana:3000`; `localhost` refers to the current container, not another service.

## Grafana login

The Compose file currently configures:

```text
Username: admin
Password: admin
```

These credentials are suitable only for local development. Change `GF_SECURITY_ADMIN_PASSWORD` before exposing Grafana beyond a trusted local environment. For production, provide the value through an environment file or secret-management system rather than committing credentials to the Compose file.

## Troubleshooting

Validate the merged Compose configuration before starting:

```bash
docker compose config
```

If a service is not healthy or reachable, inspect its logs:

```bash
docker compose logs --tail=100 app prometheus alertmanager grafana
```

Common checks:

1. Confirm that ports `8080`, `9090`, `9093`, and `3000` are available on the host.
2. Open Prometheus and check the `spring-boot-app` target under **Status → Targets**.
3. Check that Prometheus can reach `app:8080` and Alertmanager can reach `app:8080/api/alerts` on the `monitoring` network.
4. If dashboards or configuration changes are not visible, recreate the affected service:

   ```bash
   docker compose up -d --force-recreate prometheus alertmanager grafana
   ```
