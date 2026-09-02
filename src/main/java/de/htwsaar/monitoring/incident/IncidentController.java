package de.htwsaar.monitoring.incident;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller that exposes incident data through HTTP endpoints.
 */
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;

    /**
     * Creates a controller with the services required to read incident data.
     *
     * @param incidentService service used for incident query operations
     * @param incidentRepository repository used for direct incident lookup by ID
     */
    public IncidentController(
            IncidentService incidentService,
            IncidentRepository incidentRepository
    ) {
        this.incidentService = incidentService;
        this.incidentRepository = incidentRepository;
    }

    /**
     * Returns all incidents ordered by their start time.
     *
     * @return list of all incidents
     */
    @GetMapping
    public List<Incident> getAll() {
        return incidentService.getAllIncidents();
    }

    /**
     * Returns only incidents that are currently active.
     *
     * @return list of active incidents
     */
    @GetMapping("/active")
    public List<Incident> getActive() {
        return incidentService.getActiveIncidents();
    }

    /**
     * Returns a single incident by its database identifier.
     *
     * @param id incident database identifier
     * @return HTTP 200 with the incident if found, otherwise HTTP 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Incident> getById(@PathVariable Long id) {
        return incidentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}