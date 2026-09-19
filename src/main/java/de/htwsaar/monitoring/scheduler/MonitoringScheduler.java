package de.htwsaar.monitoring.scheduler;

import de.htwsaar.monitoring.service.DeviceCheckService;
import de.htwsaar.monitoring.service.PingCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task orchestrator for periodic network monitoring checks.
 * <p>
 * <strong>Trigger Schedule</strong>:
 * <ul>
 *   <li>Configured via {@code monitoring.interval} in {@code application.yml}
 *       (default: {@code 15000} milliseconds = 15 seconds).</li>
 *   <li>Uses Spring's {@link Scheduled} annotation with fixed-rate execution:
 *       {@code @Scheduled(fixedRateString = "${monitoring.interval}")}.</li>
 *   <li>Fixed-rate means: next execution starts exactly {@code interval} ms after
 *       the previous execution <strong>started</strong>, not when it finished.</li>
 * </ul>
 * <p>
 * <strong>Why TCP and ICMP Are Executed Separately</strong>:
 * <ol>
 *   <li><strong>Independent Failure Domains</strong>:
 *     <ul>
 *       <li>TCP failure: Service is down (e.g., web server crashed), but network
 *           path may still be healthy.</li>
 *       <li>ICMP failure: Network path is broken (e.g., firewall, routing issue),
 *           but service might be running.</li>
 *       <li>Separate execution allows precise root-cause analysis.</li>
 *     </ul>
 *   </li>
 *   <li><strong>Different Persistence Requirements</strong>:
 *     <ul>
 *       <li>TCP results: Persisted to SQLite for audit trails and compliance.</li>
 *       <li>ICMP results: Ephemeral, metrics-only (see {@link PingCheckService}).</li>
 *       <li>Separate methods clarify the different data flows.</li>
 *     </ul>
 *   </li>
 *   <li><strong>Alerting Granularity</strong>:
 *     <ul>
 *       <li>Prometheus alert rules can distinguish between TCP and ICMP failures.</li>
 *       <li>Example: Alert only if TCP DOWN AND ICMP UP (service issue, not network).</li>
 *     </ul>
 *   </li>
 *   <li><strong>Performance Isolation</strong>:
 *     <ul>
 *       <li>TCP checks may be slower (application handshake, TLS negotiation).</li>
 *       <li>ICMP checks are typically faster (single packet round-trip).</li>
 *       <li>Separate execution prevents one from blocking the other.</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * <strong>Execution Flow</strong>:
 * <pre>{@code
 * Every 15 seconds:
 *   1. scheduledTcpCheck()
 *      -> DeviceCheckService.checkAllDevices()
 *      -> Persist to SQLite
 *      -> Update Prometheus metrics (network_device_up, network_device_latency_ms)
 *
 *   2. scheduledPingCheck()
 *      -> PingCheckService.checkAllDevices()
 *      -> In-memory only (no persistence)
 *      -> Update Prometheus metrics (network_device_ping_up, network_device_ping_latency_ms)
 * }</pre>
 *
 * @see DeviceCheckService for TCP check implementation
 * @see PingCheckService for ICMP check implementation
 * @see org.springframework.scheduling.annotation.Scheduled for scheduling semantics
 */
@Component
public class MonitoringScheduler {

    private final DeviceCheckService deviceCheckService;
    private final PingCheckService pingCheckService;

    public MonitoringScheduler(
            DeviceCheckService deviceCheckService,
            PingCheckService pingCheckService
    ) {
        this.deviceCheckService = deviceCheckService;
        this.pingCheckService = pingCheckService;
    }

    @Scheduled(fixedDelayString = "${monitoring.interval:15000}")
    public void runChecks() {
        deviceCheckService.checkAllDevices();
    }

    @Scheduled(fixedDelayString = "${monitoring.ping-interval:30000}")
    public void runPingChecks() {
        pingCheckService.checkAllDevices();
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupHistory() {
        deviceCheckService.cleanupHistory();
    }

}
