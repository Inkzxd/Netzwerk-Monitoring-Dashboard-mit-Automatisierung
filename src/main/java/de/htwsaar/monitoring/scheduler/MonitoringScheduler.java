package de.htwsaar.monitoring.scheduler;

import de.htwsaar.monitoring.service.DeviceCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler responsible for triggering periodic monitoring checks.
 */
@Component
public class MonitoringScheduler {

    /**
     * Service used to check all configured devices.
     */
    private final DeviceCheckService deviceCheckService;

    /**
     * Creates a new monitoring scheduler.
     *
     * @param deviceCheckService service responsible for checking configured devices
     */
    public MonitoringScheduler(DeviceCheckService deviceCheckService) {
        this.deviceCheckService = deviceCheckService;
    }

    /**
     * Runs checks for all configured devices at the configured monitoring interval.
     * <p>
     * The delay is read from {@code monitoring.interval}; if it is not configured,
     * a default delay of 15000 milliseconds is used.
     */
    @Scheduled(fixedDelayString = "${monitoring.interval:15000}")
    public void runChecks() {
        deviceCheckService.checkAllDevices();
    }
}