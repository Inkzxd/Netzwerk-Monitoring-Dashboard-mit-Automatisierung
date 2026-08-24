package de.htwsaar.monitoring.alert;

import de.htwsaar.monitoring.incident.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertWebhookController {

    private static final Logger log =
            LoggerFactory.getLogger(AlertWebhookController.class);

    private final IncidentService incidentService;

    public AlertWebhookController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody AlertmanagerWebhookPayload payload
    ) {
        int alertCount = payload.alerts() == null
                ? 0
                : payload.alerts().size();

        log.info(
                "Received Alertmanager webhook: status={}, receiver={}, alerts={}",
                payload.status(),
                payload.receiver(),
                alertCount
        );

        if (payload.alerts() != null) {
            payload.alerts().forEach(alert -> {
                incidentService.process(alert);
                logAlert(alert);
            });
        }

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

    private String getLabel(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        if (alert.labels() == null) {
            return "unknown";
        }

        return alert.labels().getOrDefault(key, "unknown");
    }

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