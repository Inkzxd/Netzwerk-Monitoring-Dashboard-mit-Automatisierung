package de.htwsaar.monitoring.incident;

import de.htwsaar.monitoring.alert.AlertmanagerWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for creating, deduplicating, and resolving monitoring incidents
 * based on Alertmanager webhook notifications.
 * <p>
 * <strong>Alertmanager Fingerprint</strong>:
 * <ul>
 *   <li>Each Alertmanager alert includes a {@code fingerprint} field - a unique hash
 *       computed from the alert's labels (e.g., {@code alertname}, {@code device},
 *       {@code host}, {@code severity}).</li>
 *   <li>The fingerprint remains constant across {@code firing} and {@code resolved}
 *       states for the same underlying issue, enabling reliable correlation.</li>
 *   <li>Example fingerprint: {@code "abc123def456"} (computed by Alertmanager).</li>
 * </ul>
 * <p>
 * <strong>Deduplication Logic</strong>:
 * <ol>
 *   <li>When a {@code firing} alert arrives:
 *     <ul>
 *       <li>Query {@link IncidentRepository} for an existing incident with the same
 *           {@code fingerprint} and {@code status = FIRING}.</li>
 *       <li>If found: <strong>Ignore</strong> (duplicate alert, incident already exists).</li>
 *       <li>If not found: <strong>Create</strong> a new {@link Incident} with
 *           {@code status = FIRING}.</li>
 *     </ul>
 *   </li>
 *   <li>When a {@code resolved} alert arrives:
 *     <ul>
 *       <li>Query {@link IncidentRepository} for an existing incident with the same
 *           {@code fingerprint} and {@code status = FIRING}.</li>
 *       <li>If found: <strong>Resolve</strong> the incident by setting
 *           {@code status = RESOLVED} and {@code resolvedAt = now}.</li>
 *       <li>If not found: <strong>Log warning</strong> (resolved without firing -
 *           possible race condition or missed alert).</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * <strong>FIRING/RESOLVED Lifecycle</strong>:
 * <pre>{@code
 * FIRING:
 *   - Alertmanager detects threshold violation (e.g., network_device_up == 0 for 20s)
 *   - Sends POST /api/alerts with status="firing" and fingerprint="abc123"
 *   - IncidentService creates incident with status=FIRING, startedAt=now
 *   - Dashboard shows incident in "Active incidents" panel
 *
 * RESOLVED:
 *   - Alertmanager detects threshold recovery (e.g., network_device_up == 1)
 *   - Sends POST /api/alerts with status="resolved", fingerprint="abc123", endsAt=now
 *   - IncidentService finds matching FIRING incident by fingerprint
 *   - Updates status=RESOLVED, resolvedAt=now
 *   - Dashboard moves incident to "Incident history" panel with duration calculation
 * }</pre>
 * <p>
 * <strong>Edge Cases Handled</strong>:
 * <ul>
 *   <li>Null or blank fingerprint: Alert is ignored with warning log.</li>
 *   <li>Missing labels: Defaults to {@code "unknown"} for safety.</li>
 *   <li>Null {@code startsAt}: Uses current timestamp as fallback.</li>
 *   <li>Null {@code endsAt}: Uses current timestamp for resolution time.</li>
 *   <li>Empty {@code alerts} list: Webhook accepted but no incidents created/updated.</li>
 * </ul>
 *
 * @see Incident for the entity structure
 * @see IncidentRepository for database operations
 * @see AlertWebhookController for webhook reception
 */
@Service
public class IncidentService {

    private static final Logger log =
            LoggerFactory.getLogger(IncidentService.class);

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
     *
     * <p>A fingerprint is required to reliably match firing and resolved alerts.
     * Alerts without a usable fingerprint are ignored and logged. Resolved alerts
     * update an existing active incident, while firing alerts create a new incident
     * only if no active incident with the same fingerprint exists.</p>
     *
     * @param alert alert received from Alertmanager
     */
    @Transactional
    public void process(AlertmanagerWebhookPayload.Alert alert) {
        if (alert == null) {
            log.warn("Received null alert from Alertmanager; ignoring it");
            return;
        }

        if (alert.fingerprint() == null || alert.fingerprint().isBlank()) {
            log.warn(
                    "Ignoring Alertmanager alert without fingerprint: "
                            + "alertName={}, device={}, host={}, status={}",
                    label(alert, "alertname"),
                    label(alert, "device"),
                    label(alert, "host"),
                    alert.status()
            );
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
            log.debug(
                    "Ignoring duplicate firing alert with fingerprint={}",
                    alert.fingerprint()
            );
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

        log.info(
                "Created incident: fingerprint={}, alertName={}, device={}, host={}",
                incident.getFingerprint(),
                incident.getAlertName(),
                incident.getDeviceName(),
                incident.getHost()
        );
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
                .ifPresentOrElse(
                        incident -> {
                            incident.resolve(defaultResolvedTime(alert.endsAt()));

                            log.info(
                                    "Resolved incident: fingerprint={}, alertName={}, device={}, host={}",
                                    incident.getFingerprint(),
                                    incident.getAlertName(),
                                    incident.getDeviceName(),
                                    incident.getHost()
                            );
                        },
                        () -> log.warn(
                                "Received resolved alert without matching active incident: "
                                        + "fingerprint={}",
                                alert.fingerprint()
                        )
                );
    }

    /**
     * Reads one label from an alert safely.
     *
     * @param alert alert received from Alertmanager
     * @param key label key to read
     * @return label value or {@code unknown} when it is missing
     */
    private String label(
            AlertmanagerWebhookPayload.Alert alert,
            String key
    ) {
        return safeMap(alert.labels()).getOrDefault(key, "unknown");
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