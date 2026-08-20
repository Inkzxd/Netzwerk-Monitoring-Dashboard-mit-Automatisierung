package de.htwsaar.monitoring.alert;

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

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody AlertmanagerWebhookPayload payload
    ) {
        log.info(
                "Received Alertmanager webhook: status={}, receiver={}, alerts={}",
                payload.status(),
                payload.receiver(),
                payload.alerts() == null ? 0 : payload.alerts().size()
        );

        if (payload.alerts() != null) {
            payload.alerts().forEach(this::logAlert);
        }

        return ResponseEntity.ok().build();
    }

    private void logAlert(AlertmanagerWebhookPayload.Alert alert) {
        String alertName = alert.labels() == null
                ? "unknown"
                : alert.labels().getOrDefault("alertname", "unknown");

        String device = alert.labels() == null
                ? "unknown"
                : alert.labels().getOrDefault("device", "unknown");

        String host = alert.labels() == null
                ? "unknown"
                : alert.labels().getOrDefault("host", "unknown");

        String severity = alert.labels() == null
                ? "unknown"
                : alert.labels().getOrDefault("severity", "unknown");

        String summary = alert.annotations() == null
                ? ""
                : alert.annotations().getOrDefault("summary", "");

        String description = alert.annotations() == null
                ? ""
                : alert.annotations().getOrDefault("description", "");

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
}