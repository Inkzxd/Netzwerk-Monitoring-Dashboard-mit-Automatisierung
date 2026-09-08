package de.htwsaar.monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for the monitoring module.
 */
@ConfigurationProperties(prefix = "monitoring")
public record MonitoringProperties(
        /**
         * Timeout used for monitoring checks.
         */
        Duration timeout,

        /**
         * Shared secret used to authenticate Alertmanager webhooks.
         */
        String alertSecret,

        /**
         * Configured devices that should be monitored.
         */
        List<DeviceProperties> devices
) {

    /**
     * Applies default values when optional configuration entries are missing.
     */
    public MonitoringProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(2);
        }

        if (alertSecret == null || alertSecret.isBlank()) {
            alertSecret = "change-me-alert-secret";
        }

        if (devices == null) {
            devices = List.of();
        }
    }

    /**
     * Configuration properties for a single monitored device.
     */
    public record DeviceProperties(
            /**
             * Stable identifier for the device.
             */
            String id,

            /**
             * Display name of the device.
             */
            String name,

            /**
             * Hostname or IP address of the device.
             */
            String host,

            /**
             * Network port used to contact the device.
             */
            int port,

            /**
             * Whether this device is enabled for monitoring.
             */
            boolean enabled
    ) {
        /**
         * Uses the device name as fallback when no ID is configured.
         */
        public DeviceProperties {
            if (id == null || id.isBlank()) {
                id = name;
            }
        }
    }
}