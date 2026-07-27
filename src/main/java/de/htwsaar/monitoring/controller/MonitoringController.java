package de.htwsaar.monitoring.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class MonitoringController {

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of(
                "status", "ok",
                "timestamp", LocalDateTime.now().toString(),
                "service", "network-monitoring-dashboard"
        );
    }
}