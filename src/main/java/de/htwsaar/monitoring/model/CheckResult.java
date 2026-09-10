package de.htwsaar.monitoring.model;

import java.time.LocalDateTime;

/**
 * Represents the result of a network device availability check.
 */
public class CheckResult {

    private String deviceName;
    private boolean up;
    private long latencyMs;
    private LocalDateTime checkedAt;
    private String errorMessage;

    public CheckResult(
            String deviceName,
            boolean up,
            long latencyMs,
            LocalDateTime checkedAt,
            String errorMessage
    ) {
        this.deviceName = deviceName;
        this.up = up;
        this.latencyMs = latencyMs;
        this.checkedAt = checkedAt;
        this.errorMessage = errorMessage;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public boolean isUp() {
        return up;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}