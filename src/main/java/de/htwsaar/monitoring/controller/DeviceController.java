package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import de.htwsaar.monitoring.service.DeviceCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DeviceController {

    private final DeviceCheckService deviceCheckService;

    public DeviceController(DeviceCheckService deviceCheckService) {
        this.deviceCheckService = deviceCheckService;
    }

    @GetMapping("/api/devices")
    public List<Device> devices() {
        return deviceCheckService.getDevices();
    }

    @GetMapping("/api/checks/latest")
    public List<CheckResult> latestChecks() {
        return deviceCheckService.getLatestResults();
    }

    @GetMapping("/api/checks/run")
    public List<CheckResult> runChecks() {
        return deviceCheckService.checkAllDevices();
    }
}