# System Architecture

## Overview

The Network Monitoring Dashboard consists of a Spring Boot monitoring service, a SQLite database, Prometheus, Grafana, Alertmanager, and a browser-based dashboard.

## System Architecture Diagram

```mermaid
flowchart LR
    D[Network Devices] --> S[Spring Boot Monitoring Service]
    S --> DB[(SQLite Database)]
    S --> M[Micrometer Metrics]
    M --> P[Prometheus]
    P --> G[Grafana]
    P --> R[Alert Rules]
    R --> A[Alertmanager]
    A --> W[POST /api/alerts]
    W --> I[IncidentService]
    I --> DB
    S --> UI[Web Dashboard]
    UI --> API[REST API]
    API --> S
```

## Components

| Component | Responsibility |
|---|---|
| Network devices | Devices checked by the monitoring service. |
| Spring Boot service | Executes checks and provides the REST API. |
| SQLite | Stores check results and incidents. |
| Micrometer | Exposes application and monitoring metrics. |
| Prometheus | Stores time-series metrics and evaluates alert rules. |
| Alertmanager | Groups and forwards alerts. |
| Grafana | Visualizes Prometheus metrics. |
| Web dashboard | Displays current device status and incidents. |

## Database Model

```mermaid
erDiagram
    CHECK_RESULT {
        bigint id PK
        varchar device_name
        varchar host
        boolean up
        bigint latency_ms
        datetime checked_at
        varchar error_message
    }

    INCIDENT {
        bigint id PK
        varchar fingerprint
        varchar alert_name
        varchar device_name
        varchar host
        varchar severity
        varchar status
        datetime started_at
        datetime resolved_at
        varchar summary
        varchar description
    }
```

The current implementation stores check results and incidents as separate entities. They are related conceptually through the monitored device and timestamps, but the database model does not use a direct foreign-key relationship between them.

## Alert Flow

```mermaid
sequenceDiagram
    participant Device as Network Device
    participant App as Spring Boot Service
    participant Prom as Prometheus
    participant AM as Alertmanager
    participant Webhook as Alert Webhook
    participant Incident as IncidentService
    participant DB as SQLite

    App->>Device: TCP connectivity check
    Device-->>App: Connection result
    App->>Prom: Export device status and latency
    Prom->>Prom: Evaluate alert rule
    Prom->>AM: Send firing or resolved alert
    AM->>Webhook: POST /api/alerts
    Webhook->>Incident: Process alert payload

    alt Firing alert
        Incident->>DB: Create incident if no active fingerprint exists
    else Resolved alert
        Incident->>DB: Resolve matching active incident
    end
```

## Monitoring Data Flow

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
