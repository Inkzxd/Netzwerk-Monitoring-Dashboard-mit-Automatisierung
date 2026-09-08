package de.htwsaar.monitoring.alert;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.incident.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * REST controller that receives Alertmanager webhook notifications.
 *
 * <p>The webhook is protected by:</p>
 * <ul>
 *     <li>Spring Security ADMIN authorization</li>
 *     <li>A shared secret in the X-Alert-Secret HTTP header</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(AlertWebhookController.class);

    private static final String ALERT_SECRET_HEADER = "X-Alert-Secret";

    private final IncidentService incidentService;
    private final MonitoringProperties monitoringProperties;

    /**
     * Creates a new alert webhook controller.
     *
     * @param incidentService service responsible for incident processing
     * @param monitoringProperties monitoring configuration containing the webhook secret
     */
    public AlertWebhookController(
            IncidentService incidentService,
            MonitoringProperties monitoringProperties
    ) {
        this.incidentService = incidentService;
        this.monitoringProperties = monitoringProperties;
    }

    /**
     * Handles incoming Alertmanager webhook POST requests.
     *
     * @param payload webhook payload sent by Alertmanager
     * @param receivedSecret shared secret from the request header
     * @return HTTP 200 if accepted, 403 if the secret is invalid,
     *         or 400 if the payload is invalid
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody AlertmanagerWebhookPayload payload,
            @RequestHeader(
                    value = ALERT_SECRET_HEADER,
                    required = false
            ) String receivedSecret
    ) {
        if (!isValidSecret(receivedSecret)) {
            log.warn("Rejected Alertmanager webhook because the secret is invalid");
            return ResponseEntity.status(403).build();
        }

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

    /**
     * Compares the received secret with the configured secret.
     *
     * @param receivedSecret secret from the HTTP request
     * @return true if the secret is valid
     */
    private boolean isValidSecret(String receivedSecret) {
        return receivedSecret != null
                && !receivedSecret.isBlank()
                && Objects.equals(
                receivedSecret,
                monitoringProperties.alertSecret()
        );
    }

    /**
     * Logs a single alert with its important labels and annotations.
     *
     * @param alert alert entry from the webhook payload
     */
    private void logAlert(AlertmanagerWebhookPayload.Alert alert) {
        String alertName = getLabel(alert, "alertname");
        String device = getLabel(alert, "device");
        String host = getLabel(alert, "host");
        String severity = getLabel(alert, "severity");

        String summary = getAnnotation(alert, "summary");
        String description = getAnnotation(alert, "description");

        if ("resolved".equalsIgnoreCase(alert.status())) {
            log.info(
                    "ALERT RESOLVED: alertName={}, device={}, host={}, severity={}, " +
                            "startedAt={}, endedAt={}, fingerprint={}, summary={}, description={}",
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
            return;
        }

        log.warn(
                "ALERT FIRING: alertName={}, device={}, host={}, severity={}, " +
                        "startedAt={}, fingerprint={}, summary={}, description={}",
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

    /**
     * Reads a label value from an alert.
     *
     * @param alert alert containing the labels
     * @param key label key to read
     * @return label value or unknown when missing
     */
    private String getLabel(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        if (alert == null || alert.labels() == null) {
            return "unknown";
        }

        return alert.labels().getOrDefault(key, "unknown");
    }

    /**
     * Reads an annotation value from an alert.
     *
     * @param alert alert containing the annotations
     * @param key annotation key to read
     * @return annotation value or an empty string when missing
     */
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