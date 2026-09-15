package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.service.PingCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pings")
public class PingController {

    private final PingCheckService pingCheckService;

    public PingController(PingCheckService pingCheckService) {
        this.pingCheckService = pingCheckService;
    }

    @GetMapping("/latest")
    public List<PingCheckService.PingResult> getLatestPingResults() {
        return pingCheckService.getLatestResults();
    }
}