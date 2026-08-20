package de.htwsaar.monitoring.alert;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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