package de.htwsaar.monitoring.incident;

import de.htwsaar.monitoring.alert.AlertmanagerWebhookPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public void process(AlertmanagerWebhookPayload.Alert alert) {
        if (alert == null || alert.fingerprint() == null) {
            return;
        }

        if ("resolved".equalsIgnoreCase(alert.status())) {
            resolveActiveIncident(alert);
            return;
        }

        createIncidentIfNoActiveIncidentExists(alert);
    }

    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Incident> getActiveIncidents() {
        return incidentRepository.findAllByStatusOrderByStartedAtDesc(
                IncidentStatus.FIRING
        );
    }

    private void createIncidentIfNoActiveIncidentExists(
            AlertmanagerWebhookPayload.Alert alert
    ) {
        boolean activeIncidentExists = incidentRepository
                .findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                        alert.fingerprint(),
                        IncidentStatus.FIRING
                )
                .isPresent();

        if (activeIncidentExists) {
            return;
        }

        Map<String, String> labels = safeMap(alert.labels());
        Map<String, String> annotations = safeMap(alert.annotations());

        Incident incident = new Incident(
                alert.fingerprint(),
                labels.getOrDefault("alertname", "unknown"),
                labels.getOrDefault("device", "unknown"),
                labels.getOrDefault("host", "unknown"),
                labels.getOrDefault("severity", "unknown"),
                defaultStartTime(alert.startsAt()),
                annotations.getOrDefault("summary", ""),
                annotations.getOrDefault("description", "")
        );

        incidentRepository.save(incident);
    }

    private void resolveActiveIncident(
            AlertmanagerWebhookPayload.Alert alert
    ) {
        incidentRepository
                .findFirstByFingerprintAndStatusOrderByStartedAtDesc(
                        alert.fingerprint(),
                        IncidentStatus.FIRING
                )
                .ifPresent(incident -> incident.resolve(
                        defaultResolvedTime(alert.endsAt())
                ));
    }

    private Map<String, String> safeMap(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    private OffsetDateTime defaultStartTime(OffsetDateTime startsAt) {
        return startsAt == null ? OffsetDateTime.now() : startsAt;
    }

    private OffsetDateTime defaultResolvedTime(OffsetDateTime endsAt) {
        return endsAt == null ? OffsetDateTime.now() : endsAt;
    }
}