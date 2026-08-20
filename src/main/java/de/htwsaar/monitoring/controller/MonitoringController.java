/**
 * REST controller providing monitoring API functionality.
 *
 * This controller handles status reporting for the monitoring service. It
 * exposes an API endpoint to query the current operational status.
 */
package de.htwsaar.monitoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * REST controller that provides endpoints for monitoring the status of the application.
 *
 * This controller exposes a single endpoint to check the health and status
 * of the monitoring service. It is intended to provide a lightweight mechanism
 * for verifying the application's operational state.
 */
@RestController
public class MonitoringController {

    /**
     * Returns the current status of the monitoring service.
     * @return a map containing the service status, current timestamp, and service name
     */
    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "ok",
                "timestamp", LocalDateTime.now().toString(),
                "service", "network-monitoring-dashboard"
        );
    }
}