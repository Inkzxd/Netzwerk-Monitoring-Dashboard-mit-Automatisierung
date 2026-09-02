package de.htwsaar.monitoring.incident;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Entity representing a monitoring incident created from an alert.
 * <p>
 * An incident starts in the {@link IncidentStatus#FIRING} state and can later be
 * marked as {@link IncidentStatus#RESOLVED}.
 */
@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(
                        name = "idx_incident_fingerprint_status",
                        columnList = "fingerprint,status"
                ),
                @Index(
                        name = "idx_incident_status",
                        columnList = "status"
                )
        }
)
public class Incident {

    /**
     * Unique database identifier of the incident.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Alertmanager fingerprint used to identify matching firing/resolved alerts.
     */
    @Column(nullable = false, length = 255)
    private String fingerprint;

    /**
     * Human-readable alert name.
     */
    @Column(nullable = false, length = 255)
    private String alertName;

    /**
     * Name of the affected device, if provided by the alert labels.
     */
    @Column(length = 255)
    private String deviceName;

    /**
     * Host affected by the incident, if provided.
     */
    @Column(length = 255)
    private String host;

    /**
     * Severity level of the incident, for example warning or critical.
     */
    @Column(length = 50)
    private String severity;

    /**
     * Current lifecycle state of the incident.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    /**
     * Timestamp when the incident started.
     */
    @Column(nullable = false)
    private OffsetDateTime startedAt;

    /**
     * Timestamp when the incident was resolved, or {@code null} while firing.
     */
    private OffsetDateTime resolvedAt;

    /**
     * Short summary of the incident.
     */
    @Column(length = 500)
    private String summary;

    /**
     * Detailed incident description.
     */
    @Column(length = 2000)
    private String description;

    /**
     * Required by JPA for entity creation.
     */
    protected Incident() {
    }

    /**
     * Creates a new firing incident.
     *
     * @param fingerprint unique alert fingerprint
     * @param alertName alert name
     * @param deviceName affected device name
     * @param host affected host
     * @param severity alert severity
     * @param startedAt timestamp when the alert started firing
     * @param summary short incident summary
     * @param description detailed incident description
     */
    public Incident(
            String fingerprint,
            String alertName,
            String deviceName,
            String host,
            String severity,
            OffsetDateTime startedAt,
            String summary,
            String description
    ) {
        this.fingerprint = fingerprint;
        this.alertName = alertName;
        this.deviceName = deviceName;
        this.host = host;
        this.severity = severity;
        this.status = IncidentStatus.FIRING;
        this.startedAt = startedAt;
        this.summary = summary;
        this.description = description;
    }

    /**
     * Marks this incident as resolved.
     *
     * @param resolvedAt timestamp when the incident was resolved
     */
    public void resolve(OffsetDateTime resolvedAt) {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getAlertName() {
        return alertName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getHost() {
        return host;
    }

    public String getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }
}