package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.model.CheckResult;
import de.htwsaar.monitoring.model.Device;
import de.htwsaar.monitoring.service.DeviceCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller that exposes endpoints for querying devices and triggering
 * monitoring checks.
 * <p>
 * The controller is mapped under {@code /api} and delegates all business logic
 * to {@link DeviceCheckService}.
 * </p>
 */
@RestController
@RequestMapping("/api")
public class DeviceController {

    /**
     * Service used to access configured devices and execute monitoring checks.
     */
    private final DeviceCheckService deviceCheckService;

    /**
     * Creates a new controller instance.
     *
     * @param deviceCheckService the service responsible for device and check operations
     */
    public DeviceController(DeviceCheckService deviceCheckService) {
        this.deviceCheckService = deviceCheckService;
    }

    /**
     * Returns all configured devices.
     *
     * @return a list of devices
     */
    @GetMapping("/devices")
    public List<Device> devices() {
        return deviceCheckService.getDevices();
    }

    /**
     * Returns the most recent monitoring results.
     *
     * @return a list of the latest check results
     */
    @GetMapping("/checks/latest")
    public List<CheckResult> latestChecks() {
        return deviceCheckService.getLatestResults();
    }

    /**
     * Runs checks for all configured devices.
     *
     * @return HTTP 200 response containing the check results
     */
    @PostMapping("/checks/run")
    public ResponseEntity<List<CheckResult>> runChecks() {
        return ResponseEntity.ok(
                deviceCheckService.checkAllDevices()
        );
    }

    /**
     * Returns a single device by name, ignoring case.
     *
     * @param name the device name to look up
     * @return HTTP 200 with the matching device, or HTTP 404 if no device exists with the given name
     */
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