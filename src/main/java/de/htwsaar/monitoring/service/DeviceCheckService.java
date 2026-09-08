package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final java.util.Map<String, Double> deviceStatusMetrics =
            new ConcurrentHashMap<>();

    /**
     * Metric values containing the latest measured latency for each device.
     */
    private final java.util.Map<String, Double> latencyMetrics =
            new ConcurrentHashMap<>();

    /**
     * Most recent immutable list of device check results.
     */
    private final AtomicReference<List<CheckResult>> latestResults =
            new AtomicReference<>(List.of());

    /**
     * Repository to persist check results. May be null in tests.
     */
    private final CheckResultRepository checkResultRepository;

    /**
     * Creates a new device check service and registers device metrics.
     *
     * @param properties monitoring configuration properties
     * @param registry meter registry used to publish Micrometer metrics
     * @param checkResultRepository repository to persist check results (may be null in tests)
     */
    public DeviceCheckService(
            MonitoringProperties properties,
            MeterRegistry registry,
            CheckResultRepository checkResultRepository
    ) {
        this.properties = properties;
        this.checkResultRepository = checkResultRepository;

        // Map DeviceProperties (with id) to internal Device model (with id)
        this.devices = properties.devices()
                .stream()
                .map(device -> new Device(
                        device.id(),        // stable ID
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
                    .tag("device_id", device.getId())
                    .tag("device_name", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);

            Gauge.builder(
                            "network_device_latency_ms",
                            latencyMetrics,
                            metrics -> metrics.getOrDefault(device.getName(), 0.0)
                    )
                    .description("Latest network device check latency")
                    .tag("device_id", device.getId())
                    .tag("device_name", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);
        }
    }

    /**
     * Checks all enabled devices and updates the latest results and metrics.
     * Also persists each check result to the database if the repository is available.
     *
     * @return immutable list of check results for enabled devices
     */
    @Transactional
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

            // Persist check result if repository is available
            if (checkResultRepository != null) {
                CheckResultEntity entity = new CheckResultEntity(
                        result.getDeviceName(),
                        device.getHost(),
                        result.isUp(),
                        result.getLatencyMs(),
                        result.getCheckedAt(),
                        null
                );
                checkResultRepository.save(entity);
            }
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

    /**
     * Deletes check results older than the given timestamp.
     * Does nothing if the repository is not available.
     *
     * @param before delete all results with checkedAt before this timestamp
     */
    @Transactional
    public void deleteOldCheckResults(LocalDateTime before) {
        if (checkResultRepository == null) {
            return;
        }

        List<CheckResultEntity> all = checkResultRepository.findAllByOrderByCheckedAtDesc();
        List<CheckResultEntity> toDelete = all.stream()
                .filter(e -> e.getCheckedAt().isBefore(before))
                .toList();
        if (!toDelete.isEmpty()) {
            checkResultRepository.deleteAll(toDelete);
        }
    }
}