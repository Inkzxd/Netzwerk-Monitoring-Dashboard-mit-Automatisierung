# Incident History Test Guide

This guide explains how to generate and demonstrate incident history in the Network Monitoring Dashboard.

## Objective

The objective is to simulate unavailable network devices, verify that the monitoring system creates incidents, restore the devices, and confirm that the incidents are marked as resolved in the incident history.

## Prerequisites

The application must be running with Docker Compose:

```bash
docker compose up --build -d
```

Check the running containers:

```bash
docker compose ps
```

The test devices should be configured in `application.yml`:

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
      host: 192.0.2.2
      port: 443
      enabled: true

    - id: test-firewall
      name: Test-Firewall
      host: 192.0.2.3
      port: 443
      enabled: true
```

After changing the configuration, rebuild the application:

```bash
docker compose down
docker compose up --build -d
```

## Step 1: Verify the Test Devices

Check that the devices are loaded by the application:

```bash
curl -s http://localhost:8080/api/devices | jq
```

The response should contain:

- `Test-Router`
- `Test-Switch`
- `Test-Firewall`

Check the latest monitoring results:

```bash
curl -s http://localhost:8080/api/checks/latest | jq
```

The test devices should have the following state:

```json
{
  "deviceName": "Test-Router",
  "up": false
}
```

The same applies to `Test-Switch` and `Test-Firewall`.

## Step 2: Wait for the Alert

The Prometheus alert rule for unavailable devices is based on:

```yaml
- alert: NetworkDeviceDown
  expr: network_device_up{device!=""} == 0
  for: 1m
```

The device must remain unavailable for at least one minute before the alert is triggered.

Check the Prometheus alert page:

```text
http://localhost:9090/alerts
```

The expected alert is:

```text
NetworkDeviceDown
```

The alert severity should be:

```text
critical
```

## Step 3: Verify the Active Incident

Check the active incidents through the API:

```bash
curl -s http://localhost:8080/api/incidents/active | jq
```

An active incident should contain values similar to:

```json
{
  "alertName": "NetworkDeviceDown",
  "deviceName": "Test-Router",
  "status": "FIRING",
  "severity": "critical"
}
```

You can also open the web dashboard:

```text
http://localhost:8080
```

The device should be shown as `DOWN`, and the Active Incidents section should contain the new incident.

## Step 4: Restore a Test Device

To test the resolved state, replace one test device temporarily with a local TCP test service.

Start a temporary TCP listener on the host system:

```bash
nc -lv 59999
```

Change the corresponding device configuration to:

```yaml
    - id: test-router
      name: Test-Router
      host: host.docker.internal
      port: 59999
      enabled: true
```

Restart the application:

```bash
docker compose down
docker compose up --build -d
```

The `Test-Router` should now become available.

Verify the state:

```bash
curl -s http://localhost:8080/api/checks/latest | jq
```

The expected result is:

```json
{
  "deviceName": "Test-Router",
  "up": true
}
```

Do not interrupt production or critical network devices. Use only a safe test target.

## Step 5: Verify the Resolved Incident

Wait until Prometheus detects that the test device is available again and sends a resolved alert.

Check all incidents:

```bash
curl -s http://localhost:8080/api/incidents | jq
```

The incident should now contain:

```json
{
  "alertName": "NetworkDeviceDown",
  "deviceName": "Test-Router",
  "status": "RESOLVED",
  "resolvedAt": "..."
}
```

Check active incidents:

```bash
curl -s http://localhost:8080/api/incidents/active | jq
```

The resolved `Test-Router` incident should no longer be listed as active.

## Step 6: Generate Several Historical Incidents

Repeat the process for the three test devices:

1. Let `Test-Router` remain unavailable until `NetworkDeviceDown` becomes firing.
2. Restore `Test-Router` and wait for the incident to become resolved.
3. Repeat the same process for `Test-Switch`.
4. Repeat the same process for `Test-Firewall`.
5. Open the web dashboard and review the Incident History table.

The table should contain several resolved incidents with different devices, timestamps, and durations.

## Expected Incident History

The result should look similar to:

| Severity | Alert | Device | Status | Started | Resolved | Duration |
|---|---|---|---|---|---|---|
| critical | NetworkDeviceDown | Test-Router | RESOLVED | Test start time | Test end time | Calculated duration |
| critical | NetworkDeviceDown | Test-Switch | RESOLVED | Test start time | Test end time | Calculated duration |
| critical | NetworkDeviceDown | Test-Firewall | RESOLVED | Test start time | Test end time | Calculated duration |

## Useful Verification Commands

Display all incidents in a compact format:

```bash
curl -s http://localhost:8080/api/incidents \
  | jq '.[] | {alertName, deviceName, status, startedAt, resolvedAt}'
```

Display only active incidents:

```bash
curl -s http://localhost:8080/api/incidents/active \
  | jq '.[] | {alertName, deviceName, status}'
```

Display only resolved incidents:

```bash
curl -s http://localhost:8080/api/incidents \
  | jq '[.[] | select(.status == "RESOLVED")]'
```

## Recommended Screenshots

For project documentation, take the following screenshots:

1. Prometheus showing the `NetworkDeviceDown` alert in firing state.
2. Alertmanager showing the active alert.
3. The application dashboard showing test devices as `DOWN`.
4. The Active Incidents table showing a firing incident.
5. The application dashboard showing several resolved incidents in Incident History.
6. Alertmanager showing the resolved state, if required.

Recommended filenames:

```text
docs/images/prometheus-device-down.png
docs/images/alertmanager-firing.png
docs/images/application-device-down.png
docs/images/application-active-incident.png
docs/images/application-incident-history.png
docs/images/alertmanager-resolved.png
```

## Final Result

A successful test demonstrates the complete incident lifecycle:

```text
Device unavailable
    -> Prometheus detects network_device_up = 0
    -> NetworkDeviceDown alert becomes firing
    -> Alertmanager sends the webhook
    -> Spring Boot creates an incident
    -> Device becomes available again
    -> Alertmanager sends a resolved webhook
    -> Spring Boot marks the incident as RESOLVED
    -> Incident appears in Incident History
```
