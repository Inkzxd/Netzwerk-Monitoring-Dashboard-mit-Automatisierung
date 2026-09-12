# API Documentation

## Base URL

```text
http://localhost:8080
```

## Endpoint Overview

| Method | Endpoint | Description | Authentication |
|---|---|---|---|
| GET | `/api/status` | Returns the application status. | Public |
| GET | `/api/devices` | Returns all configured devices. | Public |
| GET | `/api/devices/{name}` | Returns one device by name. | Public |
| GET | `/api/checks/latest` | Returns the latest check results. | Public |
| GET | `/api/checks/history` | Returns historical check results. | Public |
| POST | `/api/checks/run` | Starts a manual check for all devices. | ADMIN |
| GET | `/api/incidents` | Returns all incidents. | Public |
| GET | `/api/incidents/active` | Returns all active incidents. | Public |
| GET | `/api/incidents/{id}` | Returns one incident by ID. | Public |
| POST | `/api/alerts` | Receives an Alertmanager webhook. | ADMIN |
| GET | `/actuator/health` | Returns the application health status. | Public |
| GET | `/actuator/prometheus` | Exposes Prometheus metrics. | Public |

## Endpoint Details

### GET `/api/status`

Returns the current application status.

```bash
curl http://localhost:8080/api/status
```

Example response:

```json
{
  "status": "ok",
  "timestamp": "2026-09-12T12:00:00",
  "service": "network-monitoring-dashboard"
}
```

### GET `/api/devices`

Returns all configured monitoring devices.

```bash
curl http://localhost:8080/api/devices
```

### GET `/api/devices/{name}`

Returns a device by name. The lookup is case-insensitive.

```bash
curl http://localhost:8080/api/devices/Router
```

### GET `/api/checks/latest`

Returns the latest check result for each monitored device.

```bash
curl http://localhost:8080/api/checks/latest
```

### GET `/api/checks/history`

Returns historical check results.

Optional parameters:

| Parameter | Example | Description |
|---|---|---|
| `limit` | `10` | Limits the number of returned results. Must be greater than zero. |
| `device` | `Router` | Filters results by device name. |
| `since` | `2026-09-01T10:00:00` | Returns results after the specified timestamp. |

`device` and `since` must not be used together.

Examples:

```bash
curl "http://localhost:8080/api/checks/history?limit=10"
```

```bash
curl "http://localhost:8080/api/checks/history?device=Router"
```

```bash
curl "http://localhost:8080/api/checks/history?since=2026-09-01T10:00:00"
```

### POST `/api/checks/run`

Starts a manual check for all configured devices. This endpoint requires an administrative account.

```bash
curl -u "$APP_ADMIN_USER:$APP_ADMIN_PASSWORD" \
  -X POST http://localhost:8080/api/checks/run
```

### GET `/api/incidents`

Returns all incidents, ordered by start time.

```bash
curl http://localhost:8080/api/incidents
```

### GET `/api/incidents/active`

Returns incidents that are currently firing.

```bash
curl http://localhost:8080/api/incidents/active
```

### GET `/api/incidents/{id}`

Returns one incident by its database ID.

```bash
curl http://localhost:8080/api/incidents/1
```

### POST `/api/alerts`

Receives an Alertmanager webhook. The endpoint requires the `ADMIN` role.

```bash
curl -u "$APP_ADMIN_USER:$APP_ADMIN_PASSWORD" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8080/api/alerts \
  -d '{"version":"4","status":"firing","alerts":[]}'
```

### GET `/actuator/health`

Returns the health status of the application.

```bash
curl http://localhost:8080/actuator/health
```

### GET `/actuator/prometheus`

Returns metrics in Prometheus format.

```bash
curl http://localhost:8080/actuator/prometheus
```

## Authentication

Administrative endpoints use HTTP Basic Authentication and require the `ADMIN` role. Credentials should be provided through environment variables and must not be committed to the repository.
