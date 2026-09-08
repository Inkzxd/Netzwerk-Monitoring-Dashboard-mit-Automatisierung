# Security Test Results

## Test environment

- Application: Network Monitoring Dashboard
- Authentication: HTTP Basic Authentication
- Authorization: ADMIN role
- Webhook protection: X-Alert-Secret
- Deployment: Docker Compose

## Test cases

| Test | Request | Expected result | Actual result |
|---|---|---:|---:|
| Correct credentials and secret | POST /api/alerts | 200 | 200 |
| Correct credentials, wrong secret | POST /api/alerts | 403 | 403 |
| Missing credentials, correct secret | POST /api/alerts | 401 | 401 |

## Conclusion

The tests confirm that the Alertmanager webhook and write-oriented API endpoints
are protected by both role-based authentication and a shared webhook secret.
Unauthenticated requests and requests with an invalid secret are rejected.