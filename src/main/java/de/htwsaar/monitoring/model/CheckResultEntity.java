package de.htwsaar.monitoring.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "check_results",
        indexes = {
                @Index(name = "idx_check_device_time", columnList = "deviceName,checkedAt"),
                @Index(name = "idx_check_time", columnList = "checkedAt")
        }
)
public class CheckResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String deviceName;

    @Column(nullable = false, length = 255)
    private String host;

    @Column(nullable = false)
    private boolean up;

    @Column(nullable = false)
    private long latencyMs;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    @Column(length = 500)
    private String errorMessage;

    protected CheckResultEntity() {}

    public CheckResultEntity(
            String deviceName,
            String host,
            boolean up,
            long latencyMs,
            LocalDateTime checkedAt,
            String errorMessage
    ) {
        this.deviceName = deviceName;
        this.host = host;
        this.up = up;
        this.latencyMs = latencyMs;
        this.checkedAt = checkedAt;
        this.errorMessage = errorMessage;
    }

    // Getters
    public Long getId() { return id; }
    public String getDeviceName() { return deviceName; }
    public String getHost() { return host; }
    public boolean isUp() { return up; }
    public long getLatencyMs() { return latencyMs; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public String getErrorMessage() { return errorMessage; }
}