package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Performs ICMP reachability checks independently from TCP checks.
 * <p>
 * <strong>Important: InetAddress.isReachable() Limitations</strong>:
 * <ul>
 *   <li><strong>Windows</strong>: Uses ICMP ECHO requests (real ping). Requires
 *       administrator privileges or firewall rules allowing ICMP.</li>
 *   <li><strong>Linux/Unix</strong>: May use ICMP ECHO or TCP ACK to port 7
 *       depending on JVM implementation and permissions.</li>
 *   <li><strong>Docker Containers</strong>: ICMP is often blocked by default.
 *       Use {@code --cap-add=NET_RAW} or disable firewall rules for testing.</li>
 *   <li><strong>Timeout Accuracy</strong>: The timeout parameter is approximate
 *       and may be affected by OS scheduling and network stack delays.</li>
 * </ul>
 * <p>
 * <strong>Why Ping Results Are Not Persisted</strong>:
 * <ul>
 *   <li><strong>Volume</strong>: ICMP checks run every 15 seconds for all devices,
 *       generating 5,760 records per device per day. This would quickly bloat
 *       the SQLite database.</li>
 *   <li><strong>Purpose</strong>: Ping is used for real-time network health
 *       monitoring and alerting, not historical analysis. TCP checks serve
 *       as the authoritative record of service availability.</li>
 *   <li><strong>Metrics Sufficiency</strong>: Prometheus scrapes metrics every 10s
 *       and retains them according to the retention policy (default: 15 days),
 *       providing sufficient historical data for trend analysis.</li>
 *   <li><strong>Separation of Concerns</strong>: TCP = service-level monitoring
 *       (persisted), ICMP = network-level monitoring (ephemeral, metrics-only).</li>
 * </ul>
 * <p>
 * Ping results are exposed as Prometheus gauges and are intentionally not
 * persisted in the TCP {@code check_results} table.
 *
 * @see DeviceCheckService for TCP connectivity checks (persisted)
 * @see java.net.InetAddress#isReachable(int) for implementation details
 */
@Service
public class PingCheckService {

    private static final Logger log = LoggerFactory.getLogger(PingCheckService.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(1);

    private final List<Device> devices;
    private final Map<String, Double> pingStatusMetrics = new ConcurrentHashMap<>();
    private final Map<String, Double> pingLatencyMetrics = new ConcurrentHashMap<>();
    private final AtomicReference<List<PingResult>> latestResults =
            new AtomicReference<>(List.of());

    public PingCheckService(
            MonitoringProperties properties,
            MeterRegistry registry
    ) {
        this.devices = properties.devices().stream()
                .map(device -> new Device(
                        device.id(),
                        device.name(),
                        device.host(),
                        device.port(),
                        device.enabled()
                ))
                .toList();

        registerMetrics(registry);
    }

    /**
     * Registers ICMP ping metrics for each configured device.
     * <p>
     * <strong>Metric: {@code network_device_ping_up}</strong>:
     * <ul>
     *   <li>Type: Gauge (0.0 or 1.0)</li>
     *   <li>Value: {@code 1.0} = UP (ICMP reachable), {@code 0.0} = DOWN (ICMP unreachable)</li>
     *   <li>Labels: Same as TCP metrics ({@code device_id}, {@code device}, {@code host})</li>
     *   <li>PromQL Example:
     *     <pre>{@code
     *     # Check network-level reachability
     *     network_device_ping_up{device="Router"}
     *     }</pre>
     *   </li>
     * </ul>
     * <p>
     * <strong>Metric: {@code network_device_ping_latency_ms}</strong>:
     * <ul>
     *   <li>Type: Gauge (milliseconds)</li>
     *   <li>Value: Latest ICMP ping latency</li>
     *   <li>Labels: Same as TCP metrics</li>
     *   <li>PromQL Example:
     *     <pre>{@code
     *     # Compare TCP vs ICMP latency
     *     network_device_latency_ms{device="Router"}
     *     network_device_ping_latency_ms{device="Router"}
     *     }</pre>
     *   </li>
     * </ul>
     */
    private void registerMetrics(MeterRegistry registry) {
        for (Device device : devices) {
            pingStatusMetrics.put(device.getId(), Double.NaN);
            pingLatencyMetrics.put(device.getId(), Double.NaN);

            Gauge.builder(
                            "network_device_ping_up",
                            pingStatusMetrics,
                            values -> values.getOrDefault(
                                    device.getId(),
                                    Double.NaN
                            )
                    )
                    .description("Whether the network device responds to ICMP ping")
                    .tag("device_id", device.getId())
                    .tag("device", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);

            Gauge.builder(
                            "network_device_ping_latency_ms",
                            pingLatencyMetrics,
                            values -> values.getOrDefault(
                                    device.getId(),
                                    Double.NaN
                            )
                    )
                    .description("Latest ICMP ping latency in milliseconds")
                    .tag("device_id", device.getId())
                    .tag("device", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);
        }
    }

    public synchronized List<PingResult> checkAllDevices() {
        List<PingResult> results = new ArrayList<>();

        for (Device device : devices) {
            if (!device.isEnabled()) {
                continue;
            }

            PingResult result = checkDevice(device);
            results.add(result);
            pingStatusMetrics.put(device.getId(), result.up() ? 1.0 : 0.0);
            pingLatencyMetrics.put(device.getId(), (double) result.latencyMs());
        }

        List<PingResult> immutableResults = List.copyOf(results);
        latestResults.set(immutableResults);
        return immutableResults;
    }

    public PingResult checkDevice(Device device) {
        long startNanos = System.nanoTime();
        boolean reachable = false;
        String errorMessage = null;

        try {
            int timeoutMs = Math.toIntExact(DEFAULT_TIMEOUT.toMillis());
            reachable = InetAddress.getByName(device.getHost())
                    .isReachable(timeoutMs);
        } catch (Exception exception) {
            errorMessage = exception.getClass().getSimpleName()
                    + ": " + exception.getMessage();
            log.warn(
                    "Ping check failed for device {} at {}: {}",
                    device.getName(),
                    device.getHost(),
                    errorMessage
            );
        }

        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

        return new PingResult(
                device.getName(),
                device.getHost(),
                reachable,
                latencyMs,
                errorMessage
        );
    }

    public List<PingResult> getLatestResults() {
        return latestResults.get();
    }

    public record PingResult(
            String deviceName,
            String host,
            boolean up,
            long latencyMs,
            String errorMessage
    ) {
    }
}
