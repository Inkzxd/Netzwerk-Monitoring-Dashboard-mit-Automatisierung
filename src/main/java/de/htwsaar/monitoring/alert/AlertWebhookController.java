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
 * Each incoming alert is forwarded to the incident service and logged with
 * relevant labels and annotations.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(AlertWebhookController.class);

    private final IncidentService incidentService;

    /**
     * Creates a new alert webhook controller.
     *
     * @param incidentService service responsible for creating or resolving incidents
     */
    public AlertWebhookController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    /**
     * Handles incoming Alertmanager webhook POST requests.
     *
     * @param payload webhook payload sent by Alertmanager
     * @return HTTP 200 response after the payload has been processed
     */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody AlertmanagerWebhookPayload payload
    ) {
        // Count alerts safely because Alertmanager may send an empty or missing alert list.
        int alertCount = payload.alerts() == null
                ? 0
                : payload.alerts().size();

        log.info(
                "Received Alertmanager webhook: status={}, receiver={}, alerts={}",
                payload.status(),
                payload.receiver(),
                alertCount
        );

        // Process each alert individually so incidents can be created or resolved.
        if (payload.alerts() != null) {
            payload.alerts().forEach(alert -> {
                incidentService.process(alert);
                logAlert(alert);
            });
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Logs a single alert with the most relevant labels and annotations.
     *
     * @param alert alert entry from the webhook payload
     */
    private void logAlert(AlertmanagerWebhookPayload.Alert alert) {
        // Extract common Alertmanager labels used for incident identification.
        String alertName = getLabel(alert, "alertname");
        String device = getLabel(alert, "device");
        String host = getLabel(alert, "host");
        String severity = getLabel(alert, "severity");

        // Extract human-readable alert details from annotations.
        String summary = getAnnotation(alert, "summary");
        String description = getAnnotation(alert, "description");

        // Resolved alerts are logged as informational events.
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

        // Firing alerts are logged as warnings because they represent active problems.
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
     * @return label value, or {@code "unknown"} if labels or the key are missing
     */
    private String getLabel(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        if (alert.labels() == null) {
            return "unknown";
        }

        return alert.labels().getOrDefault(key, "unknown");
    }

    /**
     * Reads an annotation value from an alert.
     *
     * @param alert alert containing the annotations
     * @param key annotation key to read
     * @return annotation value, or an empty string if annotations or the key are missing
     */
    private String getAnnotation(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        if (alert.annotations() == null) {
            return "";
        }

        return alert.annotations().getOrDefault(key, "");
    }
}