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

import jakarta.annotation.PostConstruct;

/**
 * Service responsible for performing TCP connectivity checks on configured network devices.
 * <p>
 * This service executes the following workflow for each enabled device:
 * <ol>
 *   <li><strong>TCP Connection Check</strong>: Attempts to establish a TCP connection to the
 *       configured host and port using {@link java.net.Socket#connect(java.net.SocketAddress, int)}.</li>
 *   <li><strong>Timeout Handling</strong>: Uses a configurable timeout (default: {@code monitoring.timeout})
 *       to prevent indefinite blocking. If the connection attempt exceeds this timeout, it is
 *       considered a failure.</li>
 *   <li><strong>Latency Calculation</strong>: Measures the round-trip time in milliseconds from
 *       the start of the connection attempt to either successful connection or failure.</li>
 *   <li><strong>Failure Handling</strong>: Catches {@link java.net.SocketTimeoutException},
 *       {@link java.net.ConnectException}, and other {@link Exception} types. Failed checks
 *       record the exception type and message in the {@code errorMessage} field.</li>
 *   <li><strong>Persistence</strong>: All TCP check results are persisted to the SQLite database
 *       via {@link CheckResultRepository} for historical analysis and incident tracking.</li>
 *   <li><strong>Prometheus Metrics</strong>: Exposes two gauge metrics per device:
 *       <ul>
 *         <li>{@code network_device_up} - TCP connectivity status (1.0 = UP, 0.0 = DOWN)</li>
 *         <li>{@code network_device_latency_ms} - Latest TCP connection latency in milliseconds</li>
 *       </ul>
 *   </li>»
 * </ol>
 * <p>
 * <strong>Design Rationale</strong>:
 * <ul>
 *   <li>TCP checks are persisted because they represent application-level reachability
 *       (e.g., web server on port 80, database on port 5432) and are needed for audit trails.</li>
 *   <li>TCP and ICMP checks are separated to distinguish between service-level failures
 *       (TCP port unreachable) and network-level failures (host completely unreachable).</li>
 *   <li>Metrics use {@code device_id}, {@code device}, and {@code host} labels to enable
 *       flexible filtering in Grafana dashboards and Prometheus queries.</li>
 * </ul>
 *
 * @see PingCheckService for ICMP reachability checks
 * @see CheckResultEntity for the persisted check result structure
 * @see MonitoringScheduler for the scheduled execution of checks
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
     * Executes an initial device check after the application has started.
     * <p>
     * This ensures that metrics are populated before the first scheduled
     * check runs, reducing the risk of false-positive alerts during startup.
     */
    @PostConstruct
    public void initialCheck() {
        log.info("Executing initial device check after startup");
        checkAllDevices();
    }

    /**
     * Registers TCP connectivity metrics for each configured device.
     * <p>
     * <strong>Metric: {@code network_device_up}</strong>:
     * <ul>
     *   <li>Type: Gauge (0.0 or 1.0)</li>
     *   <li>Value: {@code 1.0} = UP (connection successful), {@code 0.0} = DOWN (connection failed)</li>
     *   <li>Labels:
     *     <ul>
     *       <li>{@code device_id}: Unique device identifier (e.g., {@code "router"})</li>
     *       <li>{@code device}: Human-readable device name (e.g., {@code "Router"})</li>
     *       <li>{@code host}: IP address or hostname (e.g., {@code "192.0.2.1"})</li>
     *     </ul>
     *   </li>
     *   <li>PromQL Example:
     *     <pre>{@code
     *     # Check if Router is up
     *     network_device_up{device="Router"}
     *
     *     # Count all up devices
     *     sum(network_device_up{device!=""})
     *     }</pre>
     *   </li>
     * </ul>
     * <p>
     * <strong>Metric: {@code network_device_latency_ms}</strong>:
     * <ul>
     *   <li>Type: Gauge (milliseconds)</li>
     *   <li>Value: Latest TCP connection latency (e.g., {@code 15.0} = 15ms)</li>
     *   <li>Labels: Same as {@code network_device_up}</li>
     *   <li>PromQL Example:
     *     <pre>{@code
     *     # Average latency across all devices
     *     avg(network_device_latency_ms{device!=""})
     *
     *     # Alert if latency > 100ms for 5 minutes
     *     avg_over_time(network_device_latency_ms{device="Router"}[5m]) > 100
     *     }</pre>
     *   </li>
     * </ul>
     * <p>
     * <strong>Label Design Rationale</strong>:
     * <ul>
     *   <li>{@code device_id}: Stable identifier for joining with configuration data.</li>
     *   <li>{@code device}: Human-readable name for dashboard display.</li>
     *   <li>{@code host}: Enables filtering by IP range or subnet in PromQL.</li>
     * </ul>
     */
    private void registerMetrics(MeterRegistry registry) {
        for (Device device : devices) {
            deviceStatusMetrics.put(device.getId(), Double.NaN);
            latencyMetrics.put(device.getId(), Double.NaN);

            Gauge.builder(
                            "network_device_up",
                            deviceStatusMetrics,
                            metrics -> metrics.getOrDefault(
                                    device.getId(),
                                    Double.NaN
                            )
                    )
                    .description("Whether the network device is reachable")
                    .tag("device_id", device.getId())
                    .tag("device", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);

            Gauge.builder(
                            "network_device_latency_ms",
                            latencyMetrics,
                            metrics -> metrics.getOrDefault(
                                    device.getId(),
                                    Double.NaN
                            )
                    )
                    .description(
                            "Latest network device check latency in milliseconds"
                    )
                    .tag("device_id", device.getId())
                    .tag("device", device.getName())
                    .tag("host", device.getHost())
                    .register(registry);
        }
    }

    /**
     * Executes TCP connectivity checks for all enabled devices.
     * <p>
     * For each device:
     * <ol>
     *   <li>Skips disabled devices ({@code device.enabled = false})</li>
     *   <li>Creates a new {@link Socket} and attempts connection</li>
     *   <li>Records success/failure and latency</li>
     *   <li>Persists result via {@link CheckResultRepository#save(CheckResultEntity)}</li>
     *   <li>Updates Prometheus gauges for real-time monitoring</li>
     * </ol>
     *
     * @return immutable list of check results for all enabled devices
     * @see #checkDevice(Device) for single-device check logic
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
                result.getErrorMessage()
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
     * Performs a TCP connectivity check on a single device.
     * <p>
     * <strong>Timeout</strong>: Uses {@code monitoring.timeout} from configuration
     * (default: 100ms in tests, configurable in production).
     * <p>
     * <strong>Latency Calculation</strong>:
     * \[
     * \text{latencyMs} = \frac{\text{System.nanoTime()}_{\text{end}} - \text{System.nanoTime()}_{\text{start}}}{1,000,000}
     * \]
     * <p>
     * <strong>Failure Scenarios</strong>:
     * <ul>
     *   <li>{@link SocketTimeoutException}: Connection exceeded timeout</li>
     *   <li>{@link ConnectException}: Connection refused (no service on port)</li>
     *   <li>{@link UnknownHostException}: DNS resolution failed</li>
     *   <li>Other {@link Exception}: Network unreachable, permission denied, etc.</li>
     * </ul>
     *
     * @param device the device to check (host, port, timeout)
     * @return check result containing status, latency, and optional error message
     */
    public CheckResult checkDevice(Device device) {
        long startNanos = System.nanoTime();
        boolean up = false;
        String errorMessage = null;

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(
                            device.getHost(),
                            device.getPort()
                    ),
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
            errorMessage = exception.getClass().getSimpleName()
                    + ": "
                    + exception.getMessage();

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
                LocalDateTime.now(),
                errorMessage
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

        List<CheckResultEntity> oldResults =
                checkResultRepository.findByCheckedAtBefore(before);

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

    /**
     * Deletes check history entries older than the configured retention period.
     * <p>
     * This method is intended to be called by a scheduled task.
     */
    @Transactional
    public void cleanupHistory() {
        if (checkResultRepository == null) {
            log.debug("Skipping history cleanup because repository is null");
            return;
        }

        if (properties.historyRetention() == null) {
            log.debug("Skipping history cleanup because retention period is not configured");
            return;
        }

        LocalDateTime before = LocalDateTime.now().minus(properties.historyRetention());
        deleteOldCheckResults(before);
    }
}