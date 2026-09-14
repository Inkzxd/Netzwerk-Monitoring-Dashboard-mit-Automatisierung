package de.htwsaar.monitoring.scheduler;

import de.htwsaar.monitoring.service.DeviceCheckService;
import de.htwsaar.monitoring.service.PingCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers TCP and ICMP monitoring checks independently.
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
