package de.htwsaar.monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "monitoring")
public record MonitoringProperties(
        Duration timeout,
        List<DeviceProperties> devices
) {
    public MonitoringProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(2);
        }

        if (devices == null) {
            devices = List.of();
        }
    }

    public record DeviceProperties(
            String name,
            String host,
            int port,
            boolean enabled
    ) {
    }
}