package de.htwsaar.monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for the monitoring module.
 */
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

    /**
     * Configuration properties for a single monitored device.
     */
    public record DeviceProperties(
            String id,
            String name,
            String host,
            int port,
            boolean enabled
    ) {
        public DeviceProperties {
            if (id == null || id.isBlank()) {
                id = name;
            }
        }
    }
}