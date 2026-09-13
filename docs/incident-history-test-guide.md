# Incident History Test Guide

This guide demonstrates the complete incident lifecycle in the Network Monitoring Dashboard.

## Objective

```text
Device DOWN
    -> Prometheus detects the failure
    -> NetworkDeviceDown becomes FIRING
    -> An incident is created
    -> The device becomes UP again
    -> The alert becomes RESOLVED
    -> The incident appears in Incident history
```

A `DOWN` status alone is not sufficient. The device must remain unavailable until the Prometheus alert becomes `firing`, and the incident must be created before the device is restored.

## Prerequisites

Start the services from the project root:

```bash
docker compose up --build -d
```

Check the running containers:

```bash
docker compose ps
```

The test devices are configured in `src/main/resources/application.yml`:

```yaml
monitoring:
  devices:
    - id: test-router
      name: Test-Router
      host: 192.0.2.1
      port: 443
      enabled: true

    - id: test-switch
      name: Test-Switch
      host: host.docker.internal
      port: 59999
      enabled: true

    - id: test-firewall
      name: Test-Firewall
      host: 192.0.2.3
      port: 443
      enabled: true
```

`Test-Switch` uses port `59999` for the recovery test. Use only test targets.

## Step 1: Verify Device Status

Check the latest monitoring results:

```bash
curl -s http://localhost:8080/api/checks/latest \
  | jq '[.[] | {deviceName, up, latencyMs, errorMessage}]'
```

With the TCP listener stopped, the expected state is:

```text
Test-Router   DOWN
Test-Switch   DOWN
Test-Firewall DOWN
```

Check only `Test-Switch`:

```bash
curl -s http://localhost:8080/api/checks/latest \
  | jq '.[] | select(.deviceName == "Test-Switch")'
```

## Step 2: Wait for the Prometheus Alert

The alert rule requires the device to remain unavailable for 30 seconds before firing. The alert is defined in `deploy/prometheus/alert.rules.yml`:

```yaml
- alert: NetworkDeviceDown
  expr: network_device_up{device!=""} == 0
  for: 20s
```

Open the Prometheus alert page:

```text
http://localhost:9090/alerts
```

Or query the Prometheus API:

```bash
curl -s http://localhost:9090/api/v1/alerts \
  | jq '.data.alerts[] | select(.labels.device == "Test-Switch") | {
      device: .labels.device,
      state,
      activeAt,
      value
    }'
```

Continue only when `Test-Switch` has the following state:

```text
state: firing
```
![test-switch firing.png](img/test-switch%20firing.png)
## Step 3: Verify the Active Incident

Check the application API:

```bash
curl -s http://localhost:8080/api/incidents/active \
  | jq '.[] | {alertName, deviceName, status, severity}'
```

The expected result is similar to:

```json
{
  "alertName": "NetworkDeviceDown",
  "deviceName": "Test-Switch",
  "status": "FIRING",
  "severity": "critical"
}
```

Open the dashboard:

```text
http://localhost:8080
```

The `Active incidents` section should contain `Test-Switch`.

If the device is `DOWN` but no incident is shown, verify that Prometheus is `firing` and inspect the application logs:

```bash
docker compose logs --tail=100 app
```

## Step 4: Recover Test-Switch

After the active incident has been created, start a local TCP listener:

```bash
nc -lk 59999
```

Keep the terminal running. In a second terminal, verify the listener:

```bash
lsof -nP -iTCP:59999 -sTCP:LISTEN
```

Optional connectivity test:

```bash
nc -vz localhost 59999
```

The `Test-Switch` configuration must be:

```yaml
- id: test-switch
  name: Test-Switch
  host: host.docker.internal
  port: 59999
  enabled: true
```

If the configuration was changed, recreate the application:

```bash
docker compose down
docker compose up --build -d
```

Wait 15–30 seconds and check the result:

```bash
curl -s http://localhost:8080/api/checks/latest \
  | jq '.[] | select(.deviceName == "Test-Switch") | {deviceName, up, latencyMs, errorMessage}'
```

The expected result is:

```json
{
  "deviceName": "Test-Switch",
  "up": true
}
```

## Step 5: Verify the Resolved Incident

After `Test-Switch` becomes `UP`, wait for the alert to be resolved. Then check all incidents:

```bash
curl -s http://localhost:8080/api/incidents \
  | jq '.[] | select(.deviceName == "Test-Switch") | {
      alertName,
      deviceName,
      status,
      startedAt,
      resolvedAt
    }'
```

The expected result is:

```json
{
  "alertName": "NetworkDeviceDown",
  "deviceName": "Test-Switch",
  "status": "RESOLVED",
  "startedAt": "...",
  "resolvedAt": "..."
}
```

The active endpoint should no longer return `Test-Switch`:

```bash
curl -s http://localhost:8080/api/incidents/active \
  | jq '.[] | select(.deviceName == "Test-Switch")'
```

Refresh the dashboard and check the `Incident history` section.
![test-switch resolved.png](img/test-switch%20resolved.png)
## Troubleshooting

### Test-Switch remains DOWN

Check the following:

- The port is `59999`, not `5999`.
- `nc -lk 59999` is still running.
- `lsof -nP -iTCP:59999 -sTCP:LISTEN` shows a listener.
- The host is `host.docker.internal`.
- The application container was recreated after changing the configuration.

View the application log:

```bash
docker compose logs --tail=100 app
```

### Device is DOWN but no incident exists

A `DOWN` status alone does not create an incident immediately. Check that:

1. Prometheus shows `NetworkDeviceDown` as `firing`.
2. The alert has remained firing for at least one minute.
3. The application receives and processes the alert.

### Test-Switch becomes UP but no history entry appears

A resolved alert can only resolve an incident that was previously created while the alert was firing. Repeat the complete sequence:

```text
DOWN -> FIRING -> Active Incident -> UP -> RESOLVED -> Incident history
```

## Generate Several History Entries

Repeat the process for `Test-Router`, `Test-Switch`, and `Test-Firewall`:

1. Make one test target unavailable.
2. Wait until `NetworkDeviceDown` becomes `firing`.
3. Verify the device in `/api/incidents/active`.
4. Restore the device.
5. Wait until the incident becomes `RESOLVED`.
6. Verify the result in `/api/incidents` and the dashboard.

The final dashboard should contain several resolved incidents with different devices, timestamps, and durations.

## Useful Commands

Display all incidents:

```bash
curl -s http://localhost:8080/api/incidents \
  | jq '.[] | {alertName, deviceName, status, startedAt, resolvedAt}'
```

Display active incidents:

```bash
curl -s http://localhost:8080/api/incidents/active \
  | jq '.[] | {alertName, deviceName, status}'
```

Display resolved incidents:

```bash
curl -s http://localhost:8080/api/incidents \
  | jq '[.[] | select(.status == "RESOLVED")]'
```

## Final Result

A successful test demonstrates:

```text
DOWN status
    -> Prometheus FIRING alert
    -> Active Incident
    -> Device recovery
    -> RESOLVED alert
    -> Incident history entry
```
