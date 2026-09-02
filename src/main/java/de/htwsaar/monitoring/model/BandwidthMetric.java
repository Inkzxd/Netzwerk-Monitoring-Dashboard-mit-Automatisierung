package de.htwsaar.monitoring.model;

public class BandwidthMetric {
    private final String deviceName;
    private final String host;
    private final double usagePercent;
    private final double capacityMbps;

    public BandwidthMetric(String deviceName, String host, double usagePercent, double capacityMbps) {
        this.deviceName = deviceName;
        this.host = host;
        this.usagePercent = usagePercent;
        this.capacityMbps = capacityMbps;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getHost() {
        return host;
    }

    public double getUsagePercent() {
        return usagePercent;
    }

    public double getCapacityMbps() {
        return capacityMbps;
    }
}
