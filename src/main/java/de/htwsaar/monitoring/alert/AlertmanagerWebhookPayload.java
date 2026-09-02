package de.htwsaar.monitoring.alert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Represents the JSON payload sent by Alertmanager to the webhook endpoint.
 *
 * @param version Alertmanager webhook payload version
 * @param groupKey unique key identifying the alert group
 * @param status overall status of the alert group, for example {@code firing} or {@code resolved}
 * @param receiver name of the Alertmanager receiver that handled the notification
 * @param groupLabels labels used by Alertmanager to group related alerts
 * @param commonLabels labels shared by all alerts in this notification
 * @param commonAnnotations annotations shared by all alerts in this notification
 * @param alerts individual alerts included in the webhook payload
 */
public record AlertmanagerWebhookPayload(
        String version,
        String groupKey,
        String status,
        String receiver,
        Map<String, String> groupLabels,
        Map<String, String> commonLabels,
        Map<String, String> commonAnnotations,
        List<Alert> alerts
) {

    /**
     * Represents a single alert entry contained in an Alertmanager webhook payload.
     *
     * @param status current alert status, for example {@code firing} or {@code resolved}
     * @param labels alert-specific labels such as alert name, device, host, and severity
     * @param annotations additional alert details such as summary and description
     * @param startsAt time when the alert started firing
     * @param endsAt time when the alert was resolved, if available
     * @param generatorURL URL of the Prometheus expression or graph that generated the alert
     * @param fingerprint unique Alertmanager fingerprint for this alert
     */
    public record Alert(
            String status,
            Map<String, String> labels,
            Map<String, String> annotations,
            OffsetDateTime startsAt,
            OffsetDateTime endsAt,
            String generatorURL,
            String fingerprint
    ) {
    }
}