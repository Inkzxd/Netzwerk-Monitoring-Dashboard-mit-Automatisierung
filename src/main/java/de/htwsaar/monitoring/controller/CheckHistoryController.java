package de.htwsaar.monitoring.controller;

import de.htwsaar.monitoring.model.CheckResultEntity;
import de.htwsaar.monitoring.model.CheckResultRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for querying historical TCP check results.
 * <p>
 * <strong>Endpoint</strong>: {@code GET /api/checks/history}
 * <p>
 * <strong>Query Parameters</strong>:
 * <ul>
 *   <li><strong>{@code limit}</strong> (optional, integer):
 *     <ul>
 *       <li>Limits the number of returned results.</li>
 *       <li>Must be greater than zero (validated, returns HTTP 400 if invalid).</li>
 *       <li>Default: no limit (returns all matching records).</li>
 *       <li>Use case: Pagination or sampling recent data.</li>
 *     </ul>
 *   </li>
 *   <li><strong>{@code device}</strong> (optional, string):
 *     <ul>
 *       <li>Filters results by device name (case-sensitive match).</li>
 *       <li>Uses {@link CheckResultRepository#findByDeviceNameOrderByCheckedAtDesc(String)}.</li>
 *       <li>Use case: View history for a specific device.</li>
 *     </ul>
 *   </li>
 *   <li><strong>{@code since}</strong> (optional, ISO 8601 timestamp):
 *     <ul>
 *       <li>Returns results after the specified timestamp.</li>
 *       <li>Format: {@code yyyy-MM-dd'T'HH:mm:ss} (e.g., {@code 2026-09-01T10:00:00}).</li>
 *       <li>Uses {@link CheckResultRepository#findByCheckedAtAfterOrderByCheckedAtDesc(LocalDateTime)}.</li>
 *       <li>Use case: View recent failures or audit trail since a specific event.</li>
 *     </ul>
 *   </li>
 * </ul>
 * <p>
 * <strong>{@code device} and {@code since} Mutually Exclusive</strong>:
 * <ul>
 *   <li><strong>Rationale</strong>: Combining both would create complex query semantics
 *       (filter by device AND time? OR?) and is not needed for current use cases.</li>
 *   <li><strong>Validation</strong>: If both are provided, throws
 *       {@link IllegalArgumentException} with message:
 *       {@code "Parameters 'device' and 'since' cannot be used together."}</li>
 *   <li><strong>Client Guidance</strong>: API documentation ({@code docs/api.md})
 *       explicitly states this constraint.</li>
 * </ul>
 * <p>
 * <strong>Query Selection Logic</strong>:
 * <pre>{@code
 * if (device != null && !device.isBlank()) {
 *     // Filter by device name
 *     -> repository.findByDeviceNameOrderByCheckedAtDesc(device)
 * } else if (since != null && !since.isBlank()) {
 *     // Filter by time range
 *     -> repository.findByCheckedAtAfterOrderByCheckedAtDesc(from)
 * } else {
 *     // No filter: return all results
 *     -> repository.findAllByOrderByCheckedAtDesc()
 * }
 *
 * // Apply limit if specified
 * if (limit != null && limit < result.size()) {
 *     result = result.subList(0, limit);
 * }
 * }</pre>
 * <p>
 * <strong>Ordering</strong>: All queries return results ordered by {@code checkedAt}
 * descending (newest first), ensuring the most recent data appears first.
 *
 * @see CheckResultRepository for database query methods
 * @see CheckResultEntity for the result structure
 */
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