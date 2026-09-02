package de.htwsaar.monitoring.model;

import java.time.LocalDateTime;

/**
 * Represents the result of a network device availability check.
 */
public class CheckResult {

    /**
     * Name of the checked device.
     */
    private String deviceName;

    /**
     * Indicates whether the device was reachable during the check.
     */
    private boolean up;

    /**
     * Measured response latency in milliseconds.
     */
    private long latencyMs;

    /**
     * Timestamp when the check was performed.
     */
    private LocalDateTime checkedAt;

    /**
     * Creates a new check result.
     *
     * @param deviceName name of the checked device
     * @param up whether the device was reachable
     * @param latencyMs measured response latency in milliseconds
     * @param checkedAt timestamp when the check was performed
     */
    public CheckResult(String deviceName, boolean up, long latencyMs, LocalDateTime checkedAt) {
        this.deviceName = deviceName;
        this.up = up;
        this.latencyMs = latencyMs;
        this.checkedAt = checkedAt;
    }

    /**
     * Returns the name of the checked device.
     *
     * @return checked device name
     */
    public String getDeviceName() { return deviceName; }

    /**
     * Returns whether the device was reachable.
     *
     * @return {@code true} if the device was reachable, otherwise {@code false}
     */
    public boolean isUp() { return up; }

    /**
     * Returns the measured response latency.
     *
     * @return response latency in milliseconds
     */
    public long getLatencyMs() { return latencyMs; }

    /**
     * Returns when the check was performed.
     *
     * @return check timestamp
     */
    public LocalDateTime getCheckedAt() { return checkedAt; }
}