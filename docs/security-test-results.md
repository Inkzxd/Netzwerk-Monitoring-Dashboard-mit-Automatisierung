# Security Test Results

## Test environment

- Application: Network Monitoring Dashboard
- Authentication: HTTP Basic Authentication
- Authorization: ADMIN role
- Webhook protection: HTTP Basic Authentication
- Deployment: Docker Compose

## Test cases

| Test | Request | Expected result | Actual result |
|---|---|---:|---:|
| Correct Basic Auth credentials | POST /api/alerts | 200 | 200 |
| Missing credentials | POST /api/alerts | 401 | 401 |
| Invalid Basic Auth credentials | POST /api/alerts | 401 | 401 |

## Conclusion

The write-oriented endpoints are protected by Spring Security
HTTP Basic Authentication and role-based authorization. The Alertmanager
webhook uses Basic Authentication with the ADMIN user. Requests without
valid credentials are rejected.