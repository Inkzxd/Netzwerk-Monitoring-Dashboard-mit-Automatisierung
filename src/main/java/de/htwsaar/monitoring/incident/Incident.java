package de.htwsaar.monitoring.incident;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "incidents",
        indexes = {
                @Index(name = "idx_incident_fingerprint", columnList = "fingerprint"),
                @Index(name = "idx_incident_status", columnList = "status")
        }
)
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String fingerprint;

    @Column(nullable = false, length = 255)
    private String alertName;

    @Column(length = 255)
    private String deviceName;

    @Column(length = 255)
    private String host;

    @Column(length = 50)
    private String severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Column(nullable = false)
    private OffsetDateTime startedAt;

    private OffsetDateTime resolvedAt;

    @Column(length = 500)
    private String summary;

    @Column(length = 2000)
    private String description;

    protected Incident() {
    }

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