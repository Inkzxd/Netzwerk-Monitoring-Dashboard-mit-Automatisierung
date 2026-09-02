package de.htwsaar.monitoring.incident;

import de.htwsaar.monitoring.alert.AlertmanagerWebhookPayload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for creating, resolving, and retrieving monitoring incidents.
 * <p>
 * Incoming Alertmanager alerts are converted into {@link Incident} entities. Firing
 * alerts create new incidents unless an active incident with the same fingerprint
 * already exists. Resolved alerts mark the matching active incident as resolved.
 */
@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    /**
     * Creates a new incident service.
     *
     * @param incidentRepository repository used to access incident data
     */
    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Processes a single Alertmanager alert.
     * <p>
     * Alerts without a fingerprint are ignored because the fingerprint is required
     * to match firing and resolved alerts. Resolved alerts update an existing active
     * incident, while firing alerts create a new incident only if no active incident
     * with the same fingerprint exists.
     *
     * @param alert alert received from Alertmanager
     */
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

    /**
     * Returns all incidents ordered by newest start time first.
     *
     * @return all known incidents
     */
    @Transactional(readOnly = true)
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAllByOrderByStartedAtDesc();
    }

    /**
     * Returns all currently firing incidents ordered by newest start time first.
     *
     * @return active firing incidents
     */
    @Transactional(readOnly = true)
    public List<Incident> getActiveIncidents() {
        return incidentRepository.findAllByStatusOrderByStartedAtDesc(
                IncidentStatus.FIRING
        );
    }

    /**
     * Creates a new incident from a firing alert if no active incident with the same
     * fingerprint already exists.
     *
     * @param alert firing alert used to create the incident
     */
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

    /**
     * Resolves the active incident matching the alert fingerprint, if one exists.
     *
     * @param alert resolved alert containing the fingerprint and optional end time
     */
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

    /**
     * Returns an empty map when the provided map is {@code null}.
     *
     * @param value map to check
     * @return the original map or an empty map
     */
    private Map<String, String> safeMap(Map<String, String> value) {
        return value == null ? Map.of() : value;
    }

    /**
     * Returns the alert start time or the current time if no start time is available.
     *
     * @param startsAt start time from Alertmanager
     * @return provided start time or current time
     */
    private OffsetDateTime defaultStartTime(OffsetDateTime startsAt) {
        return startsAt == null ? OffsetDateTime.now() : startsAt;
    }

    /**
     * Returns the alert end time or the current time if no end time is available.
     *
     * @param endsAt end time from Alertmanager
     * @return provided end time or current time
     */
    private OffsetDateTime defaultResolvedTime(OffsetDateTime endsAt) {
        return endsAt == null ? OffsetDateTime.now() : endsAt;
    }
}