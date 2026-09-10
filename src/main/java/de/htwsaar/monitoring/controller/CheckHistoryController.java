package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/checks/history")
public class CheckHistoryController {

    private final CheckResultRepository repository;

    public CheckHistoryController(CheckResultRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CheckResultEntity> getAll(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String since
    ) {
        if (device != null && since != null) {
            throw new IllegalArgumentException(
                    "Parameters 'device' and 'since' cannot be used together."
            );
        }

        if (limit != null && limit <= 0) {
            throw new IllegalArgumentException(
                    "Parameter 'limit' must be greater than zero."
            );
        }

        List<CheckResultEntity> result;

        if (device != null && !device.isBlank()) {
            result = repository
                    .findByDeviceNameOrderByCheckedAtDesc(device);
        } else if (since != null && !since.isBlank()) {
            LocalDateTime from = LocalDateTime.parse(since);

            result = repository
                    .findByCheckedAtAfterOrderByCheckedAtDesc(from);
        } else {
            result = repository.findAllByOrderByCheckedAtDesc();
        }

        if (limit != null && limit < result.size()) {
            result = result.subList(0, limit);
        }

        return result;
    }
}