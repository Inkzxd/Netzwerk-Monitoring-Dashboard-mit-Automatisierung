package de.htwsaar.monitoring.alert;

import de.htwsaar.monitoring.incident.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that receives Alertmanager webhook notifications.
 * <p>
 * <strong>Alertmanager Webhook Integration</strong>:
 * <ul>
 *   <li>Endpoint: {@code POST /api/alerts}</li>
 *   <li>Content-Type: {@code application/json}</li>
 *   <li>Payload: {@link AlertmanagerWebhookPayload} containing alert group metadata
 *       and individual alert details.</li>
 *   <li>Alertmanager Configuration: See {@code deploy/alertmanager/alertmanager.yml.example}
 *       for webhook setup with HTTP Basic Authentication.</li>
 * </ul>
 * <p>
 * <strong>Authentication Requirements</strong>:
 * <ul>
 *   <li><strong>HTTP Basic Auth</strong>: Alertmanager must send credentials in the
 *       {@code Authorization: Basic <base64(username:password)>} header.</li>
 *   <li><strong>ADMIN Role</strong>: Only users with the {@code ADMIN} role can access
 *       this endpoint (configured in {@link SecurityConfig}).</li>
 *   <li><strong>CSRF Protection</strong>: Disabled for this endpoint to allow
 *       non-browser clients (Alertmanager) to POST without CSRF tokens.</li>
 *   <li><strong>Credentials</strong>: Injected via environment variables
 *       ({@code APP_ADMIN_USER}, {@code APP_ADMIN_PASSWORD}) - never hardcoded.</li>
 * </ul>
 * <p>
 * <strong>Empty Alerts Handling</strong>:
 * <ul>
 *   <li>Alertmanager may send a webhook with an empty {@code alerts} array
 *       (e.g., during initial health check or group state changes).</li>
 *   <li>The controller accepts such requests with HTTP 200 but does not invoke
 *       {@link IncidentService} (no incidents to create/resolve).</li>
 *   <li>This prevents unnecessary database operations and log noise.</li>
 * </ul>
 * <p>
 * <strong>Exception Handling</strong>:
 * <ul>
 *   <li><strong>Malformed JSON</strong>: Returns HTTP 400 with error code
 *       {@code "invalid_json"} (handled by {@link GlobalExceptionHandler}).</li>
 *   <li><strong>Null Payload</strong>: Returns HTTP 400 with error code
 *       {@code "invalid_payload"}.</li>
 *   <li><strong>Missing Fingerprint</strong>: Logs warning and skips alert
 *       (cannot correlate firing/resolved without fingerprint).</li>
 *   <li><strong>Processing Errors</strong>: Logged at WARN level; webhook still
 *       returns HTTP 200 to prevent Alertmanager retry loops.</li>
 * </ul>
 * <p>
 * <strong>Security Design Rationale</strong>:
 * <ul>
 *   <li>Basic Auth is sufficient because Alertmanager is a trusted internal service
 *       (not exposed to public internet).</li>
 *   <li>ADMIN role ensures only authorized services can create/resolve incidents.</li>
 *   <li>CSRF disabled for API endpoints to support non-browser clients while
 *       maintaining CSRF protection for browser-based operations.</li>
 * </ul>
 *
 * @see AlertmanagerWebhookPayload for the request structure
 * @see IncidentService for alert processing logic
 * @see SecurityConfig for authentication and authorization configuration
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(AlertWebhookController.class);

    private final IncidentService incidentService;

    public AlertWebhookController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Processes an Alertmanager webhook request.
     *
     * @param payload Alertmanager notification payload
     * @return HTTP 200 when accepted, or HTTP 400 for an invalid payload
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody AlertmanagerWebhookPayload payload
    ) {
        if (payload == null || payload.alerts() == null) {
            log.warn("Rejected Alertmanager webhook because the payload is invalid");
            return ResponseEntity.badRequest().build();
        }

        log.info(
                "Received Alertmanager webhook: status={}, receiver={}, alerts={}",
                payload.status(),
                payload.receiver(),
                payload.alerts().size()
        );

        payload.alerts().forEach(alert -> {
            incidentService.process(alert);
            logAlert(alert);
        });

        return ResponseEntity.ok().build();
    }

    private void logAlert(AlertmanagerWebhookPayload.Alert alert) {
        String alertName = getLabel(alert, "alertname");
        String device = getLabel(alert, "device");
        String host = getLabel(alert, "host");
        String severity = getLabel(alert, "severity");

        String summary = getAnnotation(alert, "summary");
        String description = getAnnotation(alert, "description");

        if ("resolved".equalsIgnoreCase(alert.status())) {
            log.info(
                    "ALERT RESOLVED: alertName={}, device={}, host={}, severity={}, "
                            + "startedAt={}, endedAt={}, fingerprint={}, summary={}, description={}",
                    alertName,
                    device,
                    host,
                    severity,
                    alert.startsAt(),
                    alert.endsAt(),
                    alert.fingerprint(),
                    summary,
                    description
            );
        } else {
            log.warn(
                    "ALERT FIRING: alertName={}, device={}, host={}, severity={}, "
                            + "startedAt={}, fingerprint={}, summary={}, description={}",
                    alertName,
                    device,
                    host,
                    severity,
                    alert.startsAt(),
                    alert.fingerprint(),
                    summary,
                    description
            );
        }
    }

    private String getLabel(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        if (alert == null || alert.labels() == null) {
            return "unknown";
        }

        return alert.labels().getOrDefault(key, "unknown");
    }

    private String getAnnotation(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        if (alert == null || alert.annotations() == null) {
            return "";
        }

        return alert.annotations().getOrDefault(key, "");
    }
}