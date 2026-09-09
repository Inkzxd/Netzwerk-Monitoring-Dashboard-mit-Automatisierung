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
 * Receives Alertmanager webhook notifications.
 *
 * <p>Access to this endpoint is protected by Spring Security. Alertmanager
 * sends HTTP Basic Authentication credentials, and only a user with the
 * ADMIN role can access this endpoint.</p>
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