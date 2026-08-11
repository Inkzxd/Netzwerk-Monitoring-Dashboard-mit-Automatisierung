package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import de.htwsaar.monitoring.service.DeviceCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DeviceController {

    private final DeviceCheckService deviceCheckService;

    public DeviceController(DeviceCheckService deviceCheckService) {
        this.deviceCheckService = deviceCheckService;
    }

    @GetMapping("/devices")
    public List<Device> devices() {
        return deviceCheckService.getDevices();
    }

    @GetMapping("/checks/latest")
    public List<CheckResult> latestChecks() {
        return deviceCheckService.getLatestResults();
    }

    @PostMapping("/checks/run")
    public ResponseEntity<List<CheckResult>> runChecks() {
        return ResponseEntity.ok(
                deviceCheckService.checkAllDevices()
        );
    }

    @GetMapping("/devices/{name}")
    public ResponseEntity<Device> device(@PathVariable String name) {
        return deviceCheckService.getDevices()
                .stream()
                .filter(device -> device.getName().equalsIgnoreCase(name))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}