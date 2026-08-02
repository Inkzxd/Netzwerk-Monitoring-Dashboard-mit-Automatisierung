package de.htwsaar.monitoring.model;

import java.time.LocalDateTime;

public class CheckResult {
    private String deviceName;
    private boolean up;
    private long latencyMs;
    private LocalDateTime checkedAt;

    public CheckResult(String deviceName, boolean up, long latencyMs, LocalDateTime checkedAt) {
        this.deviceName = deviceName;
        this.up = up;
        this.latencyMs = latencyMs;
        this.checkedAt = checkedAt;
    }

    public String getDeviceName() { return deviceName; }
    public boolean isUp() { return up; }
    public long getLatencyMs() { return latencyMs; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
}