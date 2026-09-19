package de.htwsaar.monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Type-safe configuration properties for network monitoring settings.
 * <p>
 * <strong>YAML to Java Record Mapping</strong>:
 * <p>
 * Spring Boot automatically binds properties from {@code application.yml} to this
 * record using the prefix {@code monitoring}. Example YAML:
 * <pre>{@code
 * monitoring:
 *   interval: 15000          # -> maps to interval field (long)
 *   timeout: 100ms           # -> maps to timeout field (Duration)
 *   history-retention: 30d   # -> maps to historyRetention field (Duration)
 *   devices:                 # -> maps to devices field (List<DeviceProperties>)
 *     - id: router
 *       name: Router
 *       host: 192.0.2.1
 *       port: 443
 *       enabled: true
 * }</pre>
 * <p>
 * <strong>Field Descriptions</strong>:
 * <ul>
 *   <li><strong>{@code interval}</strong> (long, milliseconds):
 *     <ul>
 *       <li>Controls how often {@link MonitoringScheduler} triggers TCP and ICMP checks.</li>
 *       <li>Default: {@code 15000} (15 seconds).</li>
 *       <li>Lower values = more frequent checks but higher resource usage.</li>
 *       <li>Higher values = less granular monitoring data.</li>
 *     </ul>
 *   </li>
 *   <li><strong>{@code timeout}</strong> (Duration):
 *     <ul>
 *       <li>Maximum time to wait for a TCP connection or ICMP ping response.</li>
 *       <li>Default: {@code 100ms} (tests), configurable to {@code 1s} or more in production.</li>
 *       <li>Too short: False negatives on slow networks.</li>
 *       <li>Too long: Slow check cycles, delayed alerting.</li>
 *     </ul>
 *   </li>
 *   <li><strong>{@code historyRetention}</strong> (Duration):
 *     <ul>
 *       <li>How long to retain TCP check results in SQLite before cleanup.</li>
 *       <li>Default: {@code 30d} (30 days).</li>
 *       <li>Controls database size: longer retention = more disk usage.</li>
 *       <li>Used by cleanup job (if implemented) to delete old records.</li>
 *     </ul>
 *   </li>
 *   <li><strong>{@code devices}</strong> (List&lt;DeviceProperties&gt;):
 *     <ul>
 *       <li>List of network devices to monitor.</li>
 *       <li>Each device has: {@code id} (unique identifier), {@code name} (display name),
 *           {@code host} (IP or hostname), {@code port} (TCP port), {@code enabled} (boolean).</li>
 *       <li>Devices are injected into {@link DeviceCheckService} and {@link PingCheckService}
 *           at application startup.</li>
 *       <li>Changes require application restart (or dynamic config reload if implemented).</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * <strong>Validation</strong>:
 * <ul>
 *   <li>Spring Boot validates that required fields are present.</li>
 *   <li>{@code timeout} and {@code historyRetention} use Spring's Duration parsing
 *       (supports {@code ms}, {@code s}, {@code m}, {@code h}, {@code d}).</li>
 *   <li>Empty {@code devices} list is allowed (application starts but monitors nothing).</li>
 * </ul>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see java.time.Duration for time-based configuration
 */
@ConfigurationProperties(prefix = "monitoring")
public record MonitoringProperties(
        Duration timeout,
        Duration historyRetention,
        List<DeviceProperties> devices
) {

    public MonitoringProperties {
        if (timeout == null) {
            timeout = Duration.ofSeconds(2);
        }

        if (historyRetention == null) {
            historyRetention = Duration.ofDays(30);
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