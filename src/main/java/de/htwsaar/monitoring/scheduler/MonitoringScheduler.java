package de.htwsaar.monitoring.scheduler;

import de.htwsaar.monitoring.service.DeviceCheckService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitoringScheduler {

    private final DeviceCheckService deviceCheckService;

    public MonitoringScheduler(DeviceCheckService deviceCheckService) {
        this.deviceCheckService = deviceCheckService;
    }

    @Scheduled(fixedRate = 15000)
    public void runChecks() {
        deviceCheckService.checkAllDevices();
    }
}