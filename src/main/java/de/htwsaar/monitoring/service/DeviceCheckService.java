package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for checking configured network devices and publishing metrics.
 */
@Service
public class DeviceCheckService {

    /**
     * Devices configured for monitoring.
     */
    private final List<Device> devices;

    /**
     * Monitoring configuration properties such as devices and timeout.
     */
    private final MonitoringProperties properties;

    /**
     * Metric values indicating whether each device is currently reachable.
     */
    private final Map<String, Double> deviceStatusMetrics =
            new ConcurrentHashMap<>();

    /**
     * Metric values containing the latest measured latency for each device.
     */
    private final Map<String, Double> latencyMetrics =
            new ConcurrentHashMap<>();

    /**
     * Most recent immutable list of device check results.
     */
    private final AtomicReference<List<CheckResult>> latestResults =
            new AtomicReference<>(List.of());

    /**
     * Creates a new device check service and registers device metrics.
     *
     * @param properties monitoring configuration properties
     * @param registry meter registry used to publish Micrometer metrics
     */
    public DeviceCheckService(
            MonitoringProperties properties,
            MeterRegistry registry
    ) {
        this.properties = properties;

        this.devices = properties.devices()
                .stream()
                .map(device -> new Device(
                        device.name(),
                        device.host(),
                        device.port(),
                        device.enabled()
                ))
                .toList();

        registerMetrics(registry);
    }

    /**
     * Registers Prometheus-compatible gauges for device status and latency.
     *
     * @param registry meter registry used to register the gauges
     */
    private void registerMetrics(MeterRegistry registry) {
        for (Device device : devices) {
            deviceStatusMetrics.put(device.getName(), 0.0);
            latencyMetrics.put(device.getName(), 0.0);

            Gauge.builder(
                            "network_device_up",
                            deviceStatusMetrics,
                            metrics -> metrics.getOrDefault(device.getName(), 0.0)
                    )
                    .description("Whether the network device is reachable")
                    .tag("device", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);

            Gauge.builder(
                            "network_device_latency_ms",
                            latencyMetrics,
                            metrics -> metrics.getOrDefault(device.getName(), 0.0)
                    )
                    .description("Latest network device check latency")
                    .tag("device", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);
        }
    }

    /**
     * Checks all enabled devices and updates the latest results and metrics.
     *
     * @return immutable list of check results for enabled devices
     */
    public synchronized List<CheckResult> checkAllDevices() {
        List<CheckResult> results = new ArrayList<>();

        for (Device device : devices) {
            if (!device.isEnabled()) {
                continue;
            }

            CheckResult result = checkDevice(device);
            results.add(result);

            deviceStatusMetrics.put(
                    device.getName(),
                    result.isUp() ? 1.0 : 0.0
            );

            latencyMetrics.put(
                    device.getName(),
                    (double) result.getLatencyMs()
            );
        }

        List<CheckResult> immutableResults = List.copyOf(results);
        latestResults.set(immutableResults);

        return immutableResults;
    }

    /**
     * Checks whether a single device can be reached within the configured timeout.
     *
     * @param device device to check
     * @return result containing reachability, latency, and check timestamp
     */
    public CheckResult checkDevice(Device device) {
        long start = System.currentTimeMillis();
        boolean up;

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(device.getHost(), device.getPort()),
                    (int) properties.timeout().toMillis()
            );
            up = true;
        } catch (Exception exception) {
            up = false;
        }

        long latency = System.currentTimeMillis() - start;

        return new CheckResult(
                device.getName(),
                up,
                latency,
                LocalDateTime.now()
        );
    }

    /**
     * Returns the latest completed device check results.
     *
     * @return immutable list of latest check results
     */
    public List<CheckResult> getLatestResults() {
        return latestResults.get();
    }

    /**
     * Returns the configured devices.
     *
     * @return immutable copy of configured devices
     */
    public List<Device> getDevices() {
        return List.copyOf(devices);
    }
}