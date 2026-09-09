package de.htwsaar.monitoring.service;

import de.htwsaar.monitoring.config.MonitoringProperties;
import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import de.htwsaar.monitoring.model.Device;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service responsible for checking configured network devices, persisting check
 * history, and publishing Prometheus-compatible metrics.
 */
@Service
public class DeviceCheckService {

    private static final Logger log =
            LoggerFactory.getLogger(DeviceCheckService.class);

    private final List<Device> devices;
    private final MonitoringProperties properties;
    private final CheckResultRepository checkResultRepository;

    private final Map<String, Double> deviceStatusMetrics =
            new ConcurrentHashMap<>();

    private final Map<String, Double> latencyMetrics =
            new ConcurrentHashMap<>();

    private final AtomicReference<List<CheckResult>> latestResults =
            new AtomicReference<>(List.of());

    /**
     * Creates the device check service and registers metrics for configured devices.
     *
     * @param properties monitoring configuration
     * @param registry Micrometer registry used by Prometheus
     * @param checkResultRepository repository for persistent check history
     */
    public DeviceCheckService(
            MonitoringProperties properties,
            MeterRegistry registry,
            CheckResultRepository checkResultRepository
    ) {
        this.properties = properties;
        this.checkResultRepository = checkResultRepository;

        this.devices = properties.devices()
                .stream()
                .map(device -> new Device(
                        device.id(),
                        device.name(),
                        device.host(),
                        device.port(),
                        device.enabled()
                ))
                .toList();

        registerMetrics(registry);

        log.info(
                "DeviceCheckService initialized with {} configured device(s)",
                devices.size()
        );
    }

    /**
     * Registers reachability and latency gauges for every configured device.
     *
     * @param registry Micrometer meter registry
     */
    private void registerMetrics(MeterRegistry registry) {
        for (Device device : devices) {
            deviceStatusMetrics.put(device.getId(), 0.0);
            latencyMetrics.put(device.getId(), 0.0);

            Gauge.builder(
                            "network_device_up",
                            deviceStatusMetrics,
                            metrics -> metrics.getOrDefault(device.getId(), 0.0)
                    )
                    .description("Whether the network device is reachable")
                    .tag("device_id", device.getId())
                    .tag("device_name", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);

            Gauge.builder(
                            "network_device_latency_ms",
                            latencyMetrics,
                            metrics -> metrics.getOrDefault(device.getId(), 0.0)
                    )
                    .description("Latest network device check latency in milliseconds")
                    .tag("device_id", device.getId())
                    .tag("device_name", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);
        }
    }

    /**
     * Checks all enabled devices, updates metrics, persists every result, and stores
     * the latest result list in memory.
     *
     * @return immutable list of results for enabled devices
     */
    @Transactional
    public synchronized List<CheckResult> checkAllDevices() {
        log.info("Starting check for {} configured device(s)", devices.size());

        List<CheckResult> results = new ArrayList<>();

        for (Device device : devices) {
            if (!device.isEnabled()) {
                log.debug(
                        "Skipping disabled device: id={}, name={}",
                        device.getId(),
                        device.getName()
                );
                continue;
            }

            CheckResult result = checkDevice(device);
            results.add(result);

            deviceStatusMetrics.put(
                    device.getId(),
                    result.isUp() ? 1.0 : 0.0
            );

            latencyMetrics.put(
                    device.getId(),
                    (double) result.getLatencyMs()
            );

            persistCheckResult(device, result);
        }

        List<CheckResult> immutableResults = List.copyOf(results);
        latestResults.set(immutableResults);

        log.info(
                "Completed device check: total={}, up={}, down={}",
                immutableResults.size(),
                immutableResults.stream().filter(CheckResult::isUp).count(),
                immutableResults.stream().filter(result -> !result.isUp()).count()
        );

        return immutableResults;
    }

    /**
     * Persists a single result. In regular runtime the repository must be present;
     * allowing null only keeps existing isolated unit tests compatible.
     *
     * @param device checked device
     * @param result result of the check
     */
    private void persistCheckResult(Device device, CheckResult result) {
        if (checkResultRepository == null) {
            log.debug(
                    "Skipping persistence because CheckResultRepository is null; device={}",
                    device.getName()
            );
            return;
        }

        CheckResultEntity entity = new CheckResultEntity(
                result.getDeviceName(),
                device.getHost(),
                result.isUp(),
                result.getLatencyMs(),
                result.getCheckedAt(),
                null
        );

        try {
            checkResultRepository.save(entity);

            log.debug(
                    "Persisted check result: device={}, host={}, up={}, latencyMs={}",
                    result.getDeviceName(),
                    device.getHost(),
                    result.isUp(),
                    result.getLatencyMs()
            );
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to persist check result: device={}, host={}, up={}, latencyMs={}",
                    result.getDeviceName(),
                    device.getHost(),
                    result.isUp(),
                    result.getLatencyMs(),
                    exception
            );
            throw exception;
        }
    }

    /**
     * Performs one TCP connection check.
     *
     * @param device device to check
     * @return current check result
     */
    public CheckResult checkDevice(Device device) {
        long startNanos = System.nanoTime();
        boolean up = false;

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(device.getHost(), device.getPort()),
                    Math.toIntExact(properties.timeout().toMillis())
            );

            up = true;

            log.debug(
                    "TCP check succeeded: device={}, host={}, port={}",
                    device.getName(),
                    device.getHost(),
                    device.getPort()
            );
        } catch (Exception exception) {
            log.warn(
                    "TCP check failed: device={}, host={}, port={}, reason={}",
                    device.getName(),
                    device.getHost(),
                    device.getPort(),
                    exception.getMessage()
            );
        }

        long latencyMs =
                (System.nanoTime() - startNanos) / 1_000_000;

        return new CheckResult(
                device.getName(),
                up,
                latencyMs,
                LocalDateTime.now()
        );
    }

    /**
     * Returns results from the latest complete check run.
     *
     * @return immutable latest check results
     */
    public List<CheckResult> getLatestResults() {
        return latestResults.get();
    }

    /**
     * Returns configured devices.
     *
     * @return immutable device list
     */
    public List<Device> getDevices() {
        return List.copyOf(devices);
    }

    /**
     * Deletes persisted results older than the supplied time.
     *
     * @param before delete results older than this time
     */
    @Transactional
    public void deleteOldCheckResults(LocalDateTime before) {
        if (checkResultRepository == null) {
            log.debug("Skipping history cleanup because repository is null");
            return;
        }

        List<CheckResultEntity> oldResults = checkResultRepository
                .findAllByOrderByCheckedAtDesc()
                .stream()
                .filter(result -> result.getCheckedAt().isBefore(before))
                .toList();

        if (oldResults.isEmpty()) {
            log.debug("No old check history entries to delete");
            return;
        }

        checkResultRepository.deleteAll(oldResults);

        log.info(
                "Deleted {} check history entries older than {}",
                oldResults.size(),
                before
        );
    }
}