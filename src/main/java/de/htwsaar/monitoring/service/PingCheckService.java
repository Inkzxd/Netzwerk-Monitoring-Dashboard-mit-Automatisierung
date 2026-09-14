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
 *
 * Ping results are exposed as Prometheus gauges and are intentionally not
 * persisted in the TCP check_results table.
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
