package de.htwsaar.monitoring.incident;

import de.htwsaar.monitoring.alert.AlertmanagerWebhookPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
            resolveIncident(alert);
            return;
        }

        createIncidentIfMissing(alert);
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

    private void createIncidentIfMissing(
            AlertmanagerWebhookPayload.Alert alert
    ) {
        if (incidentRepository.findByFingerprint(alert.fingerprint()).isPresent()) {
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

    private void resolveIncident(AlertmanagerWebhookPayload.Alert alert) {
        Optional<Incident> incident = incidentRepository.findByFingerprint(
                alert.fingerprint()
        );

        incident.ifPresent(existingIncident -> {
            if (existingIncident.getStatus() == IncidentStatus.FIRING) {
                existingIncident.resolve(defaultResolvedTime(alert.endsAt()));
            }
        });
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