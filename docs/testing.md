# Testing

## Test Strategy

The project uses unit tests, controller tests, repository integration tests, and security integration tests.

The main test areas are:

- REST controller behavior.
- Device check functionality.
- Alertmanager webhook processing.
- Incident creation and resolution.
- Database persistence and queries.
- Authentication and authorization.
- Error handling and invalid request parameters.

## Execute the Test Suite

Run the complete Maven test suite from the project root:

```bash
mvn clean test
```

The expected final result is:

```text
BUILD SUCCESS
```

## Test Classes

| Test category | Test class | Main purpose |
|---|---|---|
| Device controller | `DeviceControllerTest` | Tests device and check endpoints. |
| Check history controller | `CheckHistoryControllerTest` | Tests history filtering, limits, and invalid parameters. |
| Alert webhook | `AlertWebhookControllerTest` | Tests valid, malformed, resolved, and unauthorized webhook requests. |
| Incident service | `IncidentServiceTest` | Tests incident creation, duplicate handling, and resolution. |
| Repository integration | `RepositoryIntegrationTest` | Tests SQLite persistence and repository queries. |
| Security integration | `SecurityIntegrationTest` | Tests public endpoints and protected administrative endpoints. |

## Test Result Table

Complete the `Actual result` column after executing the tests.

| Test category | Expected result | Actual result |
|---|---:|---:|
| Controller tests | Passed | To be completed |
| Alert webhook tests | Passed | To be completed |
| Incident service tests | Passed | To be completed |
| Repository integration tests | Passed | To be completed |
| Security integration tests | Passed | To be completed |
| Complete Maven build | `BUILD SUCCESS` | To be completed |

## Manual Verification

The following manual checks should also be performed:

| Check | Expected result | Actual result |
|---|---|---|
| Open the web dashboard | HTTP 200 and dashboard visible | To be completed |
| Query `/api/status` | JSON response with `status: ok` | To be completed |
| Query `/actuator/health` | Health status available | To be completed |
| Open Grafana | Grafana is accessible | To be completed |
| Open Prometheus | Prometheus is accessible | To be completed |
| Open Alertmanager | Alertmanager is accessible | To be completed |
| Trigger a safe test alert | Alert appears in Alertmanager | To be completed |
| Resolve the test condition | Incident changes to resolved | To be completed |

## Maven Test Screenshot

Run:

```bash
mvn clean test
```

Take a screenshot of the final terminal output showing:

```text
BUILD SUCCESS
```

Recommended location:

```text
docs/images/maven-clean-test.png
```

Include it in this document with:

```markdown
![Maven Test Result](images/maven-clean-test.png)
```

## Notes

Do not mark a test as successful before it has been executed. Record the real output and, if a test fails, document the cause and the corrective action.
