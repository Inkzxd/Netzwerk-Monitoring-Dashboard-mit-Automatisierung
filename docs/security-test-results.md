# Security Test Results

## Test Environment

- Application: Network Monitoring Dashboard
- Authentication: HTTP Basic Authentication
- Authorization: `ADMIN` role
- Webhook protection: HTTP Basic Authentication
- Deployment: Docker Compose

## Security Model

Public read-only endpoints provide monitoring information. Write-oriented operations require authentication and the `ADMIN` role.

Protected endpoints include:

- `POST /api/checks/run`
- `POST /api/alerts`

## Automated Security Tests

The security behavior is tested by `SecurityIntegrationTest` and `AlertWebhookControllerTest`.

| Test case | Request | Expected result | Actual result |
|---|---|---:|---:|
| Public status endpoint | `GET /api/status` | 200 | To be completed |
| Public health endpoint | `GET /actuator/health` | 200 | To be completed |
| Public device list | `GET /api/devices` | 200 | To be completed |
| Public latest checks | `GET /api/checks/latest` | 200 | To be completed |
| Public incident list | `GET /api/incidents` | 200 | To be completed |
| Missing credentials for manual checks | `POST /api/checks/run` | 401 | To be completed |
| Missing credentials for webhook | `POST /api/alerts` | 401 | To be completed |
| Valid admin credentials for manual checks | `POST /api/checks/run` | 200 | To be completed |
| Valid admin credentials for webhook | `POST /api/alerts` | 200 | To be completed |
| Invalid non-admin credentials for manual checks | `POST /api/checks/run` | 401 | To be completed |
| Invalid non-admin credentials for webhook | `POST /api/alerts` | 401 | To be completed |

## Manual Security Checks

The following checks should be executed in the running application:

```bash
curl -i -X POST http://localhost:8080/api/checks/run
```

Expected result: `401 Unauthorized`.

```bash
curl -i -X POST http://localhost:8080/api/alerts \
  -H "Content-Type: application/json" \
  -d '{"version":"4","status":"firing","alerts":[]}'
```

Expected result: `401 Unauthorized`.

With valid administrative credentials:

```bash
curl -i -u "$APP_ADMIN_USER:$APP_ADMIN_PASSWORD" \
  -X POST http://localhost:8080/api/checks/run
```

Expected result: successful request, normally HTTP 200.

## Security Conclusion

Complete this section after the tests have been executed:

> The write-oriented endpoints were tested with missing, invalid, and valid credentials. Requests without valid administrative credentials were rejected, while requests using valid administrative credentials were accepted.

## Security Notes

- Change the example administrative password before production use.
- Store credentials in environment variables.
- Do not commit `.env` files or real passwords.
- Do not expose administrative credentials in screenshots.
- Use HTTPS when the application is deployed outside a trusted local environment.
